package edu.rice.starvote.ballotbox;

import java.util.Scanner;

/**
 * Created by cyricc on 10/20/2016.
 */
public class CodeInputReader {
    private final Scanner reader;
    private final BallotDatabase ballotDb;

    public CodeInputReader(BallotDatabase ballotDb) {
        this.reader = new Scanner(System.in);
        this.ballotDb = ballotDb;
    }

    public CodeInputReader(Scanner reader, BallotDatabase ballotDb) {
        this.reader = reader;
        this.ballotDb = ballotDb;
    }

    public void monitorUserInput() {
        System.out.println("Input Ready: Enter <ballot code> <pages> to enter a new ballot into the database");
        int pages;
        String code;
        while (true) {
            try {
                final String input = reader.nextLine();
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
                final String[] args = input.split(" ");
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
