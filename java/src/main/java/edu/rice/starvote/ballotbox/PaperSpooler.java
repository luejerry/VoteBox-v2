package edu.rice.starvote.ballotbox;

import com.pi4j.io.gpio.PinEdge;
import com.pi4j.io.gpio.PinState;
import edu.rice.starvote.ballotbox.drivers.IDiverter;
import edu.rice.starvote.ballotbox.drivers.IMotor;
import edu.rice.starvote.ballotbox.drivers.IScanner;
import edu.rice.starvote.ballotbox.util.GPIOListener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

/**
 * Reference implementation of ballot box paper feeder controller. The entire ballot feeding and processing sequence
 * is controlled by this module.
 *
 * @author luejerry
 */
public class PaperSpooler implements ISpooler {

    final static int SCANTIME = 5;

    private DeviceStatus status = DeviceStatus.READY;
    private final IStatusUpdate statusUpdater;
    private final IDiverter diverter;
    private final IMotor motor;
    private final GPIOListener halfwaySensor;
    private final IScanner scanner;
    private final IValidator validator;

    /**
     * Constructor. All dependency components must be supplied.
     *
     * @param statusUpdater Ballot status updater module.
     * @param diverter Ballot accept/reject diverter module.
     * @param motor Printer motor controller module.
     * @param halfwaySensor Internal printer paper sensor (HP1010 specific): reads LOW when paper is halfway through
     *                      the feed path.
     * @param scanner Code scanner module.
     * @param validator Code validator module.
     */
    public PaperSpooler(IStatusUpdate statusUpdater,
                        IDiverter diverter,
                        IMotor motor,
                        GPIOListener halfwaySensor,
                        IScanner scanner,
                        IValidator validator) {
        this.statusUpdater = statusUpdater;
        this.diverter = diverter;
        this.motor = motor;
        this.halfwaySensor = halfwaySensor;
        this.scanner = scanner;
        this.validator = validator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeviceStatus getStatus() {
        switch (status) {
            case READY:
                /* Make sure there is no paper in the feed path. */
                if (halfwaySensor.getState() == PinState.HIGH) { return status; }
                else {
                    statusUpdater.pushStatus(BallotStatus.OFFLINE);
                    status = DeviceStatus.ERROR;
                    System.out.println(getClass().getSimpleName() + ": Paper jam detected, device state set to ERROR");
                    return status;
                }
            case ERROR:
                /* Clear error status if paper jam has been cleared from feed path. */
                if (halfwaySensor.getState() == PinState.HIGH) {
                    statusUpdater.pushStatus(BallotStatus.WAITING);
                    status = DeviceStatus.READY;
                    System.out.println(getClass().getSimpleName() + "Paper jam cleared, device now READY");
                    return status;
                } else { return status; }
            default:
                return status;
        }
    }

    /**
     * {@inheritDoc}
     * @throws UncheckedIOException If an I/O error occurs communicating with motor or scanner drivers.
     */
    @Override
    public synchronized void takeIn() {
        /*
         * This implementation is fully event-driven by sensor interrupts, rather than polling. The expected sequence
         * of execution is:
         *
         *  1. Signal ballot status is SPOOLING ("pending").
         *  2. Feed in paper (motor forward).
         *  3. Event generated by halfway sensor.
         *  4. Eject paper slow (motor reverse slow).
         *  5. Scan and validate code.
         *  6. Signal ballot status is ACCEPT or REJECT.
         *  6. Actuate diverter to appropriate path (accept/reject).
         *  7. Eject paper (motor reverse).
         *  8. Event generated by halfway sensor.
         *  9. Stop feed motor.
         * 10. Signal ballot status is WAITING.
         *
         * Because event handlers execute concurrently with the main thread and each other, care must be taken to
         * ensure synchronization and ordering constraints do not allow the machine to enter an inconsistent state.
         */

        statusUpdater.pushStatus(BallotStatus.SPOOLING);
        status = DeviceStatus.BUSY;

        try { // IOException
            diverter.up();
            motor.forward(60);

            /* Wait for paper to enter the scanner. */
            final boolean paperSpooled = halfwaySensor.waitForEvent(PinEdge.FALLING, 3000);
            if (paperSpooled) {
                System.out.println(getClass().getSimpleName() + ": Paper taken in, beginning scan");
                waitMillis(150); // Small delay is necessary here to ensure paper is fed

                motor.reverse(21);
                String code = scanner.scan(SCANTIME);
                System.out.println(getClass().getSimpleName() + ": Code scanned: " + code);
                motor.stop();
               if (validator.validate(code)) {
                    System.out.println(getClass().getSimpleName() + ": Ballot code valid");
                    diverter.down();
                } else {
                    System.out.println(getClass().getSimpleName() + ": Ballot code invalid");
                    diverter.up();
                }

                waitMillis(1000); // Wait for diverter to fully actuate
                motor.reverse(60);

                /* Wait for paper to exit the scanner. */
                final boolean paperEjected = halfwaySensor.waitForEvent(PinEdge.RISING, 4000);
                if (paperEjected) {
                    System.out.println(getClass().getSimpleName() + ": Spooler cleared");
                    waitMillis(600); // Ensure paper is completely ejected
                    motor.stop();
//                    statusUpdater.pushStatus(scanStatus);
                    status = DeviceStatus.READY;
                    statusUpdater.pushStatus(BallotStatus.WAITING);
                } else {
                /* Paper did not exit scanner (paper jam). */
                    if (halfwaySensor.getState().isLow()) {
                    // Paper still in feeder, abort with error
                        System.out.println(getClass().getSimpleName() + ": Spooler jammed");
                        motor.stop();
                        status = DeviceStatus.ERROR;
                        statusUpdater.pushStatus(BallotStatus.OFFLINE);
                    } else {
                    // Feeder is clear, continue
                        System.out.println(getClass().getSimpleName() + ": Spooler checked clear");
                        motor.stop();
                        status = DeviceStatus.READY;
                        statusUpdater.pushStatus(BallotStatus.WAITING);
                    }
                }

            } else {
            /* Paper did not enter scanner. */

                System.out.println(getClass().getSimpleName() + ": Paper tray empty");

                // Reset the feeder
                motor.reverse();
                waitMillis(250);
                motor.stop();

                status = DeviceStatus.READY;
                statusUpdater.pushStatus(BallotStatus.WAITING);
            }

        } catch (IOException e) {
            statusUpdater.pushStatus(BallotStatus.OFFLINE);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Helper method to make sleep calls less unwieldy. InterruptedExceptions are printed to standard error.
     * @param delay Delay in milliseconds. Not guaranteed to block for full delay.
     */
    private static void waitMillis(int delay) {
        try {
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
            System.err.println(e.toString());
        }
    }

    /**
     * **Not yet implemented**.
     * {@inheritDoc}
     */
    @Override
    public void eject() {

    }
}
