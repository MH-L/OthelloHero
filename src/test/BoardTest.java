package test;

import algorithm.BoardTree;
import model.OthelloBoard;
import org.junit.Before;
import org.junit.Test;

public class BoardTest {
    OthelloBoard ob;
    @Before
    public void initialization() {
        ob = new OthelloBoard();
    }

    @Test
    public void testEvaluation() {
        ob.updateBoard(19);
        ob.updateBoard(18);
        ob.updateBoard(26);
        ob.updateBoard(20);
        System.out.println(ob.evaluateIntermediate());
        ob.render();
    }

    @Test
    public void testRidiculous() {
        ob.updateBoard(37);
        ob.updateBoard(45);
        ob.updateBoard(26);
        ob.updateBoard(34);
        ob.updateBoard(42);
        ob.updateBoard(33);
        ob.updateBoard(41);
//        ob.updateBoard(49);
        System.out.println(ob.evaluateIntermediate());
        System.out.println(ob.evaluateSimple());

        System.out.println(BoardTree.alphaBetaMulti(ob, 7));
        ob.render();
    }
}
