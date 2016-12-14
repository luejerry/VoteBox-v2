package edu.rice.starvote.ballotbox;

import edu.rice.starvote.ballotbox.drivers.*;
import edu.rice.starvote.ballotbox.localvalidator.BallotDatabase;
import edu.rice.starvote.ballotbox.localvalidator.BallotProgress;
import edu.rice.starvote.ballotbox.localvalidator.CodeInputReader;
import edu.rice.starvote.ballotbox.swingui.DisplayController;
import edu.rice.starvote.ballotbox.swingui.SwingDisplay;
import edu.rice.starvote.ballotbox.swingui.VoiceController;
import edu.rice.starvote.ballotbox.util.GPIOListener;

import java.util.Optional;

/**
 * Main entry point of program. Instantiates all components of the ballot box software and links them together.
 *
 * @author luejerry
 */
public class PageController {

    private final String TESTCODE ="021908509730";

    private final GPIOListener listener;
    private final GPIOListener halfwaySensor;
    private final ISpooler spooler;
    private final IScanner scanner;
    private final IMotor motor;
    private final IDiverter diverter;
    private final IStatusUpdate updater;
    private final IValidator validator;
    private final Monitor monitor;
    private final DisplayController display;
    private final BallotDatabase ballotDb;
    private final CodeInputReader inputReader;

    /**
     * Instantiates all modules, performing the necessary linking. Does not start the paper listener or status server.
     */
    public PageController() {
        listener = new GPIOListener(24);
        halfwaySensor = new GPIOListener(23);
        motor = new PrinterMotor(22, 27, new PWMBlaster(17, 50));
        diverter = new DiverterPWM(new PWMBlaster(18, 50));
        scanner = new Scan();
        ballotDb = new BallotDatabase();
        display = new DisplayController(new SwingDisplay());
        final IStatusUpdate voiceController = new VoiceController();
        updater = status -> {
            display.pushStatus(status);
            voiceController.pushStatus(status);
        };
        validator = code -> {
            if (code == null) {
                System.out.println("Validator: No code was scanned");
                updater.pushStatus(BallotStatus.REJECT);
                return false;
            }
            final Optional<BallotProgress> oProgress = ballotDb.scanPage(code);
            return oProgress.map((progress) -> {
                if (progress.completed()) {
                    System.out.println("Validator: Ballot code " + code + " completed");
                    updater.pushStatus(BallotStatus.ACCEPT);
                    if (code.equals(TESTCODE)) {
                        ballotDb.addBallot(TESTCODE, 1);
                        System.out.println("New ballot added with code " + TESTCODE + ", 1 page");
                    } else if (code.equals("")) {
                        ballotDb.addBallot("", 1);
                        System.out.println("New ballot added with blank code, 1 page");
                    }

//                    addTestBallots(); // For testing only: readd the ballot code after it is finished scanning
                } else {
                    System.out.println("Validator: Ballot code " + code + " scanned " + progress.pagesScanned + " of " + progress.pagesTotal);
                    display.pushProgress(progress);
                }
                return true;
            }).orElseGet(() -> {
                System.out.println("Validator: Ballot code " + code + " invalid");
                updater.pushStatus(BallotStatus.REJECT);
                return false;
            });
        };
        spooler = new PaperSpooler(updater, diverter, motor, halfwaySensor, scanner, validator);
        monitor = new Monitor(listener, spooler);
        inputReader = new CodeInputReader(ballotDb);
    }

    /**
     * Start the status server and paper listener. **This method does not return.**
     */
    public void run() {
        display.start();
        updater.pushStatus(BallotStatus.WAITING);
        addTestBallots(); // For testing only: add initial 3 page ballot
        new Thread(monitor::run).start();
        inputReader.monitorUserInput();
    }

    public void addTestBallots() {
        final String code = "Acc3ptB@LL07";
        ballotDb.addBallot(code, 3);
        System.out.println("New ballot added with code " + code + ", 3 pages");
        ballotDb.addBallot(TESTCODE, 1);
        System.out.println("New ballot added with code " + TESTCODE + ", 1 page");

        // For public demo purpose only; barcode scanner sometimes returns an empty string instead of the code
        ballotDb.addBallot("", 1);
        System.out.println("New ballot added with empty code, 1 page");
    }

    /**
     * Program main entry point. Starts up the ballot box.
     *
     * @param args Ignored.
     */
    public static void main(String[] args) {
        final PageController controller = new PageController();
        controller.run();
    }
}
