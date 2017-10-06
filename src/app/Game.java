package app;

import algorithm.BoardTree;
import model.OthelloBoard;

import java.util.Scanner;

public class Game {
    public static void main(String[] args) {
        playInteractively();
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
//        System.out.println("Move suggested by board tree: " + BoardTree.alphaBetaSilly(ob, 10));
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
                int optimal = BoardTree.alphaBetaSilly(board, 10);
                board.updateBoard(optimal);
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
                } catch (Exception e) {
                    System.out.println("That is not a valid choice!");
                    continue;
                }
            }
        }
    }
}
