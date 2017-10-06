package app;

import model.OthelloBoard;

public class Game {
    public static void main(String[] args) {
        OthelloBoard ob = new OthelloBoard();
        ob.updateBoard(20, true);
        ob.updateBoard(19, false);
        ob.updateBoard(18, true);
        ob.updateBoard(11, false);
        ob.updateBoard(2, true);
        ob.updateBoard(17, false);
        ob.updateBoard(16, true);
        ob.updateBoard(9, false);
        ob.updateBoard(0, true);
        ob.updateBoard(21, false);
        ob.updateBoard(34, true);
        ob.render();
    }
}
