package algorithm;

import model.Hasher;
import model.OthelloBoard;

import java.util.*;

public class BoardTree {
    public static final int WINNING_SCORE = 10000; // A winning score is still needed in order to make the result more compelling.
    public static int updateCount = 0;
    public static int withdrawCount = 0;
    public static int cacheHits = 0;
    public static int totalEvals = 0;

    private static final Map<Long, Integer> evalCache = new HashMap<>();
//    private static final int[] EVAL_LIMITS = {100, 15, 12, 10, 8, 6, 5, 4, 3, 3, 3, 3, 3, 3, 3, 3};
    private static final int[] EVAL_LIMITS = {100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100};

    public static int alphaBetaSilly(OthelloBoard bd, int depth) {
        long ck1 = System.currentTimeMillis();
        int calcResult = alphaBetaSillyImpl(bd, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, new int[]{0}, 0, new HashMap<>());
        long ck2 = System.currentTimeMillis();
        System.out.println("Time elapsed (single-threaded): " + (ck2 - ck1) + " milliseconds");
        return calcResult;
    }

    public static int alphaBetaMulti(OthelloBoard bd, int depth) {
        Set<Integer> nmoves = bd.getMobility();
        int[] curBest = {Integer.MIN_VALUE};
        int[] curOutput = {-1};
        int numRemaining = bd.getremainingPieces();
        List<Thread> startedThreads = new ArrayList<>();
        for (int mv : nmoves) {
            OthelloBoard newCopy = new OthelloBoard(bd);
            newCopy.updateBoard(mv);
            int[] eval = {0};
            Thread t = new Thread(() -> {
                if (numRemaining < 16)
                {
                    System.out.println("Applying brute force search...");
                    bruteForce(newCopy, Integer.MIN_VALUE, Integer.MAX_VALUE, eval);
                }
                else
                {
                    alphaBetaSillyImpl(newCopy, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, eval, 1, new HashMap<>());
                }

                if (eval[0] > curBest[0]) {
                    curOutput[0] = mv;
                    curBest[0] = eval[0];
                }
            });

            t.start();
            startedThreads.add(t);
        }

        System.out.println("Started multithreading calculation, number of threads: " + startedThreads.size());
        long ck1 = System.currentTimeMillis();

        for (Thread tojoin : startedThreads) {
            try {
                tojoin.join();
            } catch (InterruptedException e) {
                System.out.println("Bad thread join!");
                e.printStackTrace();
            }
        }

        long ck2 = System.currentTimeMillis();
        System.out.println("Time elapsed (multithreading): " + (ck2 - ck1) + " milliseconds!");
        return curOutput[0];
    }

    /**
     * Doing alpha beta search in a naive way
     * @param bd board
     * @param depth number of plys to search, can't be over 15
     * @param alpha alpha value
     * @param beta beta value
     * @param value the heuristic value calculated by the search
     * @return a board position indicating the best move
     */
    private static int alphaBetaSillyImpl(OthelloBoard bd, int depth, int alpha,
                                          int beta, int[] value, int curDepth, HashMap<Long, Integer> evalCache) {
        // TODO after 40 moves or so, use exhaustive evaluation
        int gameRes = bd.gameOver();
        if (gameRes != OthelloBoard.GAME_IN_PROGRESS) {
            // Need to give winning score for more compelling results
            int heuValue = bd.evaluateSimple();

            // Want to win as many as possible still, so combine heuristics as well
            if (heuValue > 0)
                value[0] = WINNING_SCORE + heuValue;
            else if (heuValue == 0)
                value[0] = 0;
            else
                value[0] = -WINNING_SCORE + heuValue;
            return -1;
        }

        if (depth == 0) {
            if (bd.getremainingPieces() > depth + 2)
                value[0] = bd.evaluateIntermediate();
            else
                value[0] = bd.evaluateSimple();
            return -1;
        }

        // Just so this function doesn't look weird
        boolean maximizing = bd.getTurn();
        Set<Integer> nextMoves = bd.getMobility();

        List<Integer> nmsorted = new ArrayList<>();
        Map<Integer, Integer> incMap = new HashMap<>();
        for (int mv : nextMoves) {
            int inc = bd.getInc(mv); // Gets increment in heuristics
            incMap.put(mv, inc);
            // TODO investigate whether or not to implement move-filtering
            nmsorted.add(mv);
            // TODO add direct pruning based on inc here after evaluation function is complete.
        }

        if (nmsorted.isEmpty())
            nmsorted.addAll(nextMoves);
        nmsorted.sort(new Comparator<Integer>() { // Moves with more potentials get evaluated first
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
        long hashVal = Hasher.hash(bd);

        totalEvals++;
        // TODO board configuration doesn't necessarily dictate turn
        // so we might need to configure eval cache by color
        if (evalCache.containsKey(hashVal)) // First-level boards never hit this condition
        {
            value[0] = evalCache.get(hashVal);
            cacheHits++;
        }
        else {
            if (maximizing) {
                int maxVal = Integer.MIN_VALUE;
                int limit = EVAL_LIMITS[curDepth];
                int moveCtr = 0;
                for (int move : nmsorted) {
                    if (moveCtr > limit) {
                        break;
                    }
                    bd.updateBoard(move);
//                if (!result) {
//                    System.out.println("Update rejected, move: " + move);
//                    System.out.println("Turn: " + bd.getTurn());
//                    bd.render();
//                }
                    updateCount++;
                    alphaBetaSillyImpl(bd, depth - 1, alpha, beta, value, curDepth + 1, evalCache);
                    if (value[0] > maxVal) {
                        maxVal = value[0];
                        bestMove = move;
                    }

                    bd.withdraw();
                    withdrawCount++;
                    alpha = Math.max(alpha, maxVal);
                    if (beta <= alpha)
                        break;
                    moveCtr++;
                }

                if (curDepth == 1) {
                    int randomPurt = new Random().nextInt(11) - 5;
                    value[0] = maxVal + randomPurt;
                    System.out.println("Randomization in play here, score: " + (maxVal + randomPurt));
                } else {
                    value[0] = maxVal;
                    evalCache.put(hashVal, maxVal);
                }
            } else {
                int minVal = Integer.MAX_VALUE;
                int limit = EVAL_LIMITS[curDepth];
                int moveCtr = 0;
                for (int move : nmsorted) {
                    if (moveCtr > limit) {
                        break;
                    }
                    bd.updateBoard(move);
//                if (!result) {
//                    System.out.println("Update rejected, move: " + move);
//                    System.out.println("Turn: " + bd.getTurn());
//                    bd.render();
//                }
                    updateCount++;
                    alphaBetaSillyImpl(bd, depth - 1, alpha, beta, value, curDepth + 1, evalCache);
                    if (value[0] < minVal) {
                        minVal = value[0];
                        bestMove = move;
                    }
                    bd.withdraw();
                    withdrawCount++;
                    beta = Math.min(beta, minVal);
                    if (beta <= alpha)
                        break;
                    moveCtr++;
                }

                if (curDepth == 1) {
                    int randomPurt = new Random().nextInt(11) - 5;
                    value[0] = minVal + randomPurt;
                    System.out.println("Randomization in play here, score: " + (minVal + randomPurt));
                } else {
                    value[0] = minVal;
                    evalCache.put(hashVal, minVal);
                }
            }
        }

        return bestMove;
    }

    private static int bruteForce(OthelloBoard bd, int alpha, int beta, int[] endDiff)
    {
        if (bd.gameOver() !=  OthelloBoard.GAME_IN_PROGRESS)
        {
            endDiff[0] = bd.evaluateSimple();
            return -1;
        }

        Set<Integer> nmoves = bd.getMobility();
        boolean maximizing = bd.getTurn();

        int bestMove = -1;
        if (maximizing)
        {
            int maxVal = Integer.MIN_VALUE;
            for (int move : nmoves) {
                bd.updateBoard(move);
                updateCount++;
                bruteForce(bd, alpha, beta, endDiff);
                if (endDiff[0] > maxVal) {
                    maxVal = endDiff[0];
                    bestMove = move;
                }

                bd.withdraw();
                withdrawCount++;
                alpha = Math.max(alpha, maxVal);
                if (beta <= alpha)
                    break;
            }

            endDiff[0] = maxVal;
        }
        else
        {
            int minVal = Integer.MAX_VALUE;
            for (int move : nmoves) {
                boolean result = bd.updateBoard(move);
                updateCount++;
                bruteForce(bd, alpha, beta, endDiff);
                if (endDiff[0] < minVal) {
                    minVal = endDiff[0];
                    bestMove = move;
                }
                bd.withdraw();
                withdrawCount++;
                beta = Math.min(beta, minVal);
                if (beta <= alpha)
                    break;
            }
            endDiff[0] = minVal;
        }
        return bestMove;
    }
}
