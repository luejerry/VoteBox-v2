package edu.rice.starvote.ballotbox;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import static org.junit.Assert.*;

/**
 * Created by cyricc on 10/21/2016.
 */
public class CodeInputReaderTest {
    @Test
    public void test() throws Exception {
        final BallotDatabase ballots = new BallotDatabase();
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(
                ("BAL1 1\n" +
                        "BAL2 2\n" +
                        "BAL3 3\n" +
                        "exit\n").getBytes());
        final Scanner scanner = new Scanner(inputStream);
        final CodeInputReader reader = new CodeInputReader(scanner, ballots);

        reader.monitorUserInput();

        assertTrue(ballots.getProgress("BAL1").isPresent());
        assertFalse(ballots.getProgress("BAL0").isPresent());
        assertTrue(ballots.getProgress("BAL2").isPresent());
        assertTrue(ballots.getProgress("BAL3").isPresent());
        assertEquals(1, ballots.getProgress("BAL1").map((progress) -> progress.pagesTotal).get());
        assertEquals(2, ballots.getProgress("BAL2").map((progress) -> progress.pagesTotal).get());
        assertEquals(3, ballots.getProgress("BAL3").map((progress) -> progress.pagesTotal).get());
    }
}