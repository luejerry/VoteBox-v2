package edu.rice.starvote.ballotbox;

import edu.rice.starvote.ballotbox.localvalidator.BallotDatabase;
import edu.rice.starvote.ballotbox.localvalidator.BallotProgress;
import org.junit.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Created by cyricc on 10/19/2016.
 */
public class BallotDatabaseTest {
    @Test
    public void scanPage() throws Exception {
        final BallotDatabase ballotDb = new BallotDatabase();
        ballotDb.addBallot("test", 3);
        Function<String, Integer> scanfunc = (code) -> {
            final Optional<BallotProgress> ballotProgress = ballotDb.scanPage(code);
            return ballotProgress.map((progress) -> progress.pagesScanned).orElse(-1);
        };
        assertEquals(-1, scanfunc.apply("invalid"));
        assertEquals(1, scanfunc.apply("test"));
        assertEquals(2, scanfunc.apply("test"));
        assertEquals(3, scanfunc.apply("test"));
    }

}