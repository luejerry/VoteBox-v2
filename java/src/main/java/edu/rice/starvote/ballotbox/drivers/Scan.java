package edu.rice.starvote.ballotbox.drivers;

import edu.rice.starvote.ballotbox.util.ExternalProcess;
import edu.rice.starvote.ballotbox.util.JarResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Implementation of scanner controller that invokes an external C scan program.
 *
 * @author luejerry
 */
public class Scan implements IScanner {

    private final static Path scanPath = JarResource.getResource("scan");

    public Optional<String> scan(int timeout) throws IOException {
        final String result = ExternalProcess.runInDirAndCapture(scanPath.getParent().toFile(), "./scan", String.valueOf(timeout));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.substring(0, result.length() - 1)); // Remove the trailing newline
    }
}
