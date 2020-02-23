package test;

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
        System.out.println(ob.evaluateIntermediate());
        ob.render();
    }
}
