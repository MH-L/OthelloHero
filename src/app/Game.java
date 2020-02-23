package app;

import algorithm.BoardTree;
import model.OthelloBoard;

import java.util.Scanner;

public class Game {
    public static void main(String[] args) {
        playInteractively();

//        selfPlay(25);
//        OthelloBoard ob = new OthelloBoard();
//        long curTime = System.currentTimeMillis();
//        ob.updateBoard(34);
//        ob.updateBoard(26);
//        ob.updateBoard(18);
//        ob.updateBoard(42);
//        ob.updateBoard(20);
//        ob.updateBoard(21);
//        ob.updateBoard(16);
//        ob.updateBoard(9);
//        ob.updateBoard(0);
//        ob.updateBoard(21);
//        ob.updateBoard(34);
//        ob.withdraw();
//        ob.withdraw();
//        ob.updateBoard(21);
//        ob.updateBoard(34);
//        ob.withdraw();
//        ob.withdraw();
//        ob.withdraw();
//        ob.withdraw();
//        ob.withdraw();
//        System.out.println("Turn: " + ob.getTurn());
//        System.out.println("Move suggested by board tree: " + BoardTree.alphaBetaSillyImpl(ob, 10));
//        ob.reset();
//        System.out.println("Time spent: " + (System.currentTimeMillis() - curTime));
//        System.out.println("Update count: " + BoardTree.updateCount);
//        System.out.println("Withdrawal count: " + BoardTree.withdrawCount);
//        ob.render();
    }

    public static void playInteractively() {
        System.out.println("Welcome to Othello Hero, LMH's second attempt for games!");
        System.out.println("Choose difficulty... OH NO! The only difficulty is hell mode!");
        System.out.println("Game started!");
        OthelloBoard board = new OthelloBoard();
        Scanner sc = new Scanner(System.in);
        while (board.gameOver() == OthelloBoard.GAME_IN_PROGRESS) {
            if (board.getTurn()) {
//                int optimal = BoardTree.alphaBetaSillyImpl(board, 9);
                int optimal = BoardTree.alphaBetaMulti(board, 8);
                System.out.println("Evaluation: " +  board.evaluateIntermediate());
                board.updateBoard(optimal);
                System.out.println("Evaluation after: " + board.evaluateIntermediate());

                int rowIdx = optimal / OthelloBoard.WIDTH + 1;
                int colIdx = optimal % OthelloBoard.WIDTH + 1;
                System.out.println("Computer Move: " + rowIdx + "," + colIdx);
            } else {
                board.render();
                System.out.println("Now, it's your turn!");
                String move = sc.nextLine();
                try {
                    int rowIdx = Integer.parseInt(move.split(",")[0].trim());
                    int colIdx = Integer.parseInt(move.split(",")[1].trim());
                    rowIdx--;
                    colIdx--;
                    boolean valid = board.updateBoard(rowIdx * OthelloBoard.WIDTH + colIdx);
                    if (!valid)
                        throw new IllegalArgumentException();
                    board.render();
                } catch (Exception e) {
                    System.out.println("That is not a valid choice!");
                    continue;
                }
            }
        }

        System.out.println("Game finished, here is the stats: ");
        System.out.println("Black pieces: " + board.getNumBlack());
        System.out.println("White pieces: " + board.getNumWhite());
    }

    private static void selfPlay(int rounds) {
        int blWins = 0;
        int wtWins = 0;
        int drawCnt = 0;
        for (int i = 0; i < rounds; i++) {
            OthelloBoard board = new OthelloBoard();
            while (board.gameOver() == OthelloBoard.GAME_IN_PROGRESS) {
                if (board.getTurn()) {
                    int optimal = BoardTree.alphaBetaMulti(board, 8);
                    board.updateBoard(optimal);
                    System.out.println("Black move finished, board configuration:");
                    board.render();
                } else {
                    int optimal = BoardTree.alphaBetaSillyImpl(board, 7);
                    board.updateBoard(optimal);
                    System.out.println("White move finished, board configuration:");
                    board.render();
                }
            }

            System.out.println("Game finished, here is the stats: ");
            int blPieces = board.getNumBlack();
            int wtPieces = board.getNumWhite();
            System.out.println("Black pieces: " + blPieces);
            System.out.println("White pieces: " + wtPieces);
            if (blPieces > wtPieces)
            {
                blWins++;
            }
            else if (wtPieces > blPieces)
            {
                wtWins++;
            }
            else
            {
                drawCnt++;
            }


            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Self run finished, black wins: " +
                blWins + ", white wins: " + wtWins + ", draws: " + drawCnt);
    }
}
