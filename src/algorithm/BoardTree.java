package algorithm;

import model.OthelloBoard;

import java.util.*;

public class BoardTree {
    public static final int WINNING_SCORE = 10000; // A winning score is still needed in order to make the result more compelling.
    public static int updateCount = 0;
    public static int withdrawCount = 0;

    public static int alphaBetaSilly(OthelloBoard bd, int depth) {
        return alphaBetaSilly(bd, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, new int[]{0});
    }

    private static int alphaBetaSilly(OthelloBoard bd, int depth, int alpha,
                                     int beta, int[] value) {
        int gameRes = bd.gameOver();
        if (gameRes != OthelloBoard.GAME_IN_PROGRESS) {
            // Need to give winning score for more compelling results
            int heuValue = bd.evaluateSimple();
            if (heuValue > 0)
                value[0] = WINNING_SCORE;
            else if (heuValue == 0)
                value[0] = 0;
            else
                value[0] = -WINNING_SCORE;
            return -1;
        }

        if (depth == 0) {
            value[0] = bd.evaluateSimple();
            return -1;
        }

        // Just so this function doesn't look weird
        boolean maximizing = bd.getTurn();
        Set<Integer> nextMoves = bd.getMobility();

        List<Integer> nmsorted = new ArrayList<>(nextMoves);
        Map<Integer, Integer> incMap = new HashMap<>();
        for (int mv : nextMoves) {
            int inc = bd.getFlipSet(mv).size();
            incMap.put(mv, inc);
            // TODO add direct pruning based on inc here after evaluation function is complete.
        }

        nmsorted.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int v1 = incMap.get(o1);
                int v2 = incMap.get(o2);
                if (v1 == v2)
                    return 0;
                return v1 > v2 ? -1 : 1;
            }
        });

        int bestMove = -1;
        if (maximizing) {
            int maxVal = Integer.MIN_VALUE;
            for (int move : nmsorted) {
                boolean result = bd.updateBoard(move);
                if (!result) {
                    System.out.println("Update rejected, move: " + move);
                    System.out.println("Turn: " + bd.getTurn());
                    bd.render();
                }
                updateCount++;
//                System.out.println("After update, pieces: " + bd.getPieceCount());
                alphaBetaSilly(bd, depth - 1, alpha, beta, value);
                if (value[0] > maxVal) {
                    maxVal = value[0];
                    bestMove = move;
                }

                bd.withdraw();
                withdrawCount++;
//                System.out.println("After withdraw, pieces: " + bd.getPieceCount());
                alpha = Math.max(alpha, maxVal);
                if (beta <= alpha)
                    break;
            }

            value[0] = maxVal;
        } else {
            int minVal = Integer.MAX_VALUE;
            for (int move : nmsorted) {
                boolean result = bd.updateBoard(move);
                if (!result) {
                    System.out.println("Update rejected, move: " + move);
                    System.out.println("Turn: " + bd.getTurn());
                    bd.render();
                }
                updateCount++;
//                System.out.println("After update, pieces: " + bd.getPieceCount());
                alphaBetaSilly(bd, depth - 1, alpha, beta, value);
                if (value[0] < minVal) {
                    minVal = value[0];
                    bestMove = move;
                }
                bd.withdraw();
                withdrawCount++;
//                System.out.println("After withdraw, pieces: " + bd.getPieceCount());
                beta = Math.min(beta, minVal);
                if (beta <= alpha)
                    break;
            }

            value[0] = minVal;
        }

        return bestMove;
    }
}
