package edu.rice.starvote.ballotbox.drivers;

import java.io.IOException;
import java.util.Optional;

/**
 * Interface for a code scanner controller.
 *
 * @author luejerry
 */
public interface IScanner {

    /**
     * Attempt to scan a code.
     *
     * @param timeout Timeout in seconds.
     * @return Optional containing scanned code, or empty optional if timeout elapsed.
     * @throws IOException If an error occurs when communicating with the device driver.
     */
    Optional<String> scan(int timeout) throws IOException;
}
