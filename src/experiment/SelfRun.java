package experiment;

import algorithm.BoardTree;
import model.OthelloBoard;

public class SelfRun {
    public static void main(String[] args) {
        OthelloBoard experimentBoard = new OthelloBoard();
        for (int i = 0; i < 10000; i++) {
            experimentBoard.reset();
            System.out.println("Playing game " + (i + 1));
            while (experimentBoard.gameOver() == OthelloBoard.GAME_IN_PROGRESS) {
                int optimalMove = BoardTree.alphaBetaSilly(experimentBoard, 7);
                experimentBoard.updateBoard(optimalMove);
                experimentBoard.render();
            }

            System.out.println(String.format("Game finished, black: %s, white: %s",
                    experimentBoard.getNumBlack(), experimentBoard.getNumWhite()));
        }
    }
}
