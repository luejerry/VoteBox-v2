package edu.rice.starvote.ballotbox;

/**
 * Created by cyricc on 10/20/2016.
 */
public class CodeInputTest {
    public static void main(String[] args) {
        final BallotDatabase ballotDatabase = new BallotDatabase();
        final CodeInputReader reader = new CodeInputReader(ballotDatabase);
        reader.monitorUserInput();
    }
}
