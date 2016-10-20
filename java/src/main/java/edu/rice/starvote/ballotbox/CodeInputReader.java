package edu.rice.starvote.ballotbox;

import java.util.Scanner;

/**
 * Created by cyricc on 10/20/2016.
 */
public class CodeInputReader {
    private final Scanner reader = new Scanner(System.in);
    private final BallotDatabase ballotDb;

    public CodeInputReader(BallotDatabase ballotDb) {
        this.ballotDb = ballotDb;
    }

    public void monitorUserInput() {
        System.out.println("Input Ready: Enter <ballot code> <pages> to enter a new ballot into the database");
        int pages;
        String code;
        while (true) {
            try {
                final String[] args = reader.nextLine().split(" ");
                if (args.length == 0) {
                    continue;
                } else if (args.length == 1) {
                    pages = 1;
                } else {
                    pages = Integer.parseInt(args[1]);
                    if (pages < 1) {
                        throw new NumberFormatException();
                    }
                }
                code = args[0];
                ballotDb.addBallot(code, pages);
                System.out.format("Input Success: Ballot %s added with %d pages\n", code, pages);
            } catch (NumberFormatException e) {
                System.err.println("Input Error: Second argument must be the number of pages in the ballot");
            }
        }
    }
}
