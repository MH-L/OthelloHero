package model;

import java.util.*;

public class OthelloBoard {
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;
    public static final int GAME_IN_PROGRESS = 10;
    public static final int GAME_FINISHED_TIE = 12;
    public static final int GAME_FINISHED_BLACK_WINS = 14;
    public static final int GAME_FINISHED_WHITE_WINS = 16;
    private static final int NUM_LR_DIAGS = WIDTH + HEIGHT - 1;
    private static final int NUM_RL_DIAGS = WIDTH + HEIGHT - 1;
    private char[][] grid;
    private Set<Integer> blackMobility;
    private Set<Integer> whiteMobility;
    private List<Integer> moveSequence;
    private List<Boolean> turnHistory;
    private Map<Integer, Set<Integer>> historicalFlips;
    private int numBlackPieces = 0;
    private int numWhitePieces = 0;

    // active player: true = black, false = white
    private boolean turn = true;

    // key is the line, value is the valid position -> pieces to flip map
    private static Map<String, Map<Integer, Set<Integer>>> cacheMapBlack;
    private static Map<String, Map<Integer, Set<Integer>>> cacheMapWhite;

    static {
        cacheMapBlack = new HashMap<>();
        cacheMapWhite = new HashMap<>();
        countFlipAll();
    }

    private static void countFlipAll() {
        for (int len = 3; len <= WIDTH; len++) {
            String next = "";
            for (int j = 0; j < len; j++) {
                next += '0';
            }

            do {
                Map<Integer, Set<Integer>> result = countFlipSingle(next, true);
                Map<Integer, Set<Integer>> result2 = countFlipSingle(next, false);
                if (!result.isEmpty())
                    cacheMapBlack.put(next, result);
                if (!result2.isEmpty())
                    cacheMapWhite.put(next, result2);
            } while (!(next = nextStr(next)).isEmpty());
        }
    }

    /**
     * Find next 3-radix string using elementary-school arithmetic
     *
     * @param str
     * @return
     */
    private static String nextStr(String str) {
        StringBuilder sb = new StringBuilder(str);
        int counter = 0;
        while (counter < sb.length()) {
            char cur = sb.charAt(counter);
            if (cur != '2') {
                if (cur == '0')
                    sb.setCharAt(counter, '1');
                else
                    sb.setCharAt(counter, '2');
                return sb.toString();
            } else
                sb.setCharAt(counter, '0');
            counter++;
        }

        return "";
    }

    private static Map<Integer, Set<Integer>> countFlipSingle(String str, boolean isFirst) {
        Map<Integer, Set<Integer>> returnVal = new HashMap<>();
        char selfPiece = isFirst ? '1' : '2';
        char oppPiece = isFirst ? '2' : '1';
        for (int i = 0; i < str.length(); i++) {
            char curPiece = str.charAt(i);
            if (curPiece != '0')
                continue;
            for (int j = i + 1; j < str.length(); j++) {
                char innerPiece = str.charAt(j);
                if (innerPiece == selfPiece) {
                    if (j > i + 1) {
                        Set<Integer> s = new HashSet<>();
                        for (int k = i + 1; k < j; k++) {
                            s.add(k);
                        }
                        returnVal.put(i, s);
                    }
                    break;
                } else if (innerPiece == '0')
                    break;
            }
        }

        for (int i = str.length() - 1; i >= 0; i--) {
            char curPiece = str.charAt(i);
            if (curPiece != '0')
                continue;
            for (int j = i - 1; j >= 0; j--) {
                char innerPiece = str.charAt(j);
                if (innerPiece == selfPiece) {
                    if (j < i - 1) {
                        Set<Integer> s = new HashSet<>();
                        for (int k = i - 1; k > j; k--) {
                            s.add(k);
                        }
                        if (!returnVal.containsKey(i))
                            returnVal.put(i, s);
                        else {
                            // Can flip from two directions
                            s.addAll(returnVal.get(i));
                            returnVal.put(i, s);
                        }
                    }
                    break;
                } else if (innerPiece == '0')
                    break;
            }
        }

        return returnVal;
    }

    /**
     * Constructor
     */
    public OthelloBoard() {
        blackMobility = new HashSet<>();
        whiteMobility = new HashSet<>();
        historicalFlips = new HashMap<>();
        moveSequence = new ArrayList<>();
        turnHistory = new ArrayList<>();

        grid = new char[8][8];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = '0';
            }
        }
        initialUpdate();
    }

    public int gameOver() {
        if (blackMobility.isEmpty() && whiteMobility.isEmpty()) {
            if (numBlackPieces == numWhitePieces)
                return GAME_FINISHED_TIE;
            else if (numBlackPieces > numWhitePieces)
                return GAME_FINISHED_BLACK_WINS;
            else
                return GAME_FINISHED_WHITE_WINS;
        }

        return GAME_IN_PROGRESS;
    }

    public boolean getTurn() {
        return turn;
    }

    public int evaluateSimple() {
        return numBlackPieces - numWhitePieces;
    }

    public Set<Integer> getMobility() {
        return turn ? blackMobility : whiteMobility;
    }

    public int getPieceCount() {
        return moveSequence.size();
    }

    public void reset() {
        blackMobility.clear();
        whiteMobility.clear();
        historicalFlips.clear();
        moveSequence.clear();
        turnHistory.clear();
        turn = true;

        // reset grid
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = '0';
            }
        }
        initialUpdate();
    }

    private void initialUpdate() {
        grid[WIDTH / 2 - 1][HEIGHT / 2 - 1] = '1';
        grid[WIDTH / 2 - 1][HEIGHT / 2] = '2';
        grid[WIDTH / 2][HEIGHT / 2 - 1] = '2';
        grid[WIDTH / 2][HEIGHT / 2] = '1';
        countMobility(true);
        numBlackPieces = 2;
        numWhitePieces = 2;
        blackMobility = countMobility(true);
        whiteMobility = countMobility(false);
    }

    public boolean updateBoard(int move) {
        if ((turn && !blackMobility.contains(move)) || (!turn && !whiteMobility.contains(move)))
            return false;
        char piece = turn ? '1' : '2';

        int rowIndex = move / WIDTH;
        int colIndex = move % WIDTH;
        Set<Integer> flipSet = getFlipSet(move);
        moveSequence.add(move);
        historicalFlips.put(move, flipSet);
        for (int p : flipSet)
            flip(p);
        grid[rowIndex][colIndex] = piece;
        turnHistory.add(turn);

        // Don't forget to update the count here
        if (turn) numBlackPieces++;
        else numWhitePieces++;

        // Next turn is the opponent's
        whiteMobility = countMobility(false);
        blackMobility = countMobility(true);

        turn = !turn;
        // The player with no mobility has to give up their turn
        if (whiteMobility.isEmpty())
            turn = true;
        else if (blackMobility.isEmpty())
            turn = false;

        return true;
    }

    public Set<Integer> getFlipSet(int move) {
        Map<String, Map<Integer, Set<Integer>>> map = turn ? cacheMapBlack : cacheMapWhite;
        int rowIndex = move / WIDTH;
        int colIndex = move % WIDTH;
        int lrDiagIdx = getltorDiagIndex(move);
        int rlDiagIdx = getrtolDiagIndex(move);
        int idxOnLR = getIndexOnLtoR(move);
        int idxOnRL = getIndexOnRtoL(move);
        String row = getRow(rowIndex);
        String col = getCol(colIndex);
        String lrDiag = getLRDiag(lrDiagIdx);
        String rlDiag = getRLDiag(rlDiagIdx);
        Set<Integer> flipSet = new HashSet<>();

        // map does not necessarily contain row, col, diag since there might not be capture on all lines
        if (map.containsKey(row) && map.get(row).containsKey(colIndex))
            for (int i : map.get(row).get(colIndex))
                flipSet.add(rowIndex * WIDTH + i);
        if (map.containsKey(col) && map.get(col).containsKey(rowIndex))
            for (int j : map.get(col).get(rowIndex))
                flipSet.add(j * WIDTH + colIndex);
        if (map.containsKey(lrDiag) && map.get(lrDiag).containsKey(idxOnLR))
            for (int m : map.get(lrDiag).get(idxOnLR))
                flipSet.add(lrDiagToBoardPosition(lrDiagIdx, m));
        if (map.containsKey(rlDiag) && map.get(rlDiag).containsKey(idxOnRL))
            for (int n : map.get(rlDiag).get(idxOnRL))
                flipSet.add(rlDiagToBoardPosition(rlDiagIdx, n));
        return flipSet;
    }

    /** withdraw is only valid if the last move is withdrawn
     *  withdraw the last move
     */
    public void withdraw() {
        if (moveSequence.isEmpty())
            return;
        // TODO potential inefficiency
        int lastMove = moveSequence.get(moveSequence.size() - 1);
        moveSequence.remove((Integer) lastMove);
        Set<Integer> flipped = historicalFlips.remove(lastMove);
        if (grid[lastMove / WIDTH][lastMove % WIDTH] == '1')
            numBlackPieces--;
        else
            numWhitePieces--;
        grid[lastMove / WIDTH][lastMove % WIDTH] = '0';
        for (int loc : flipped) {
            flip(loc);
        }

        blackMobility = countMobility(true);
        whiteMobility = countMobility(false);
        turn = turnHistory.get(turnHistory.size() - 1);
        turnHistory.remove(turnHistory.size() - 1);
    }

    private Set<Integer> countMobility(boolean isFirst) {
        Set<Integer> mobility = new HashSet<>();
        Map<String, Map<Integer, Set<Integer>>> map = isFirst ? cacheMapBlack : cacheMapWhite;

        // Rows
        for (int i = 0; i < HEIGHT; i++) {
            String row = getRow(i);
            if (map.containsKey(row)) {
                Set<Integer> mobSet = map.get(row).keySet();
                for (int mob : mobSet)
                    mobility.add(i * WIDTH + mob);
            }
        }

        // Cols
        for (int i = 0; i < WIDTH; i++) {
            String col = getCol(i);
            if (map.containsKey(col)) {
                Set<Integer> mobSet = map.get(col).keySet();
                for (int mob : mobSet)
                    mobility.add(mob * WIDTH + i);
            }
        }

        // LR diagonals
        for (int i = 0; i < NUM_LR_DIAGS; i++) {
            String lrDiag = getLRDiag(i);
            if (map.containsKey(lrDiag)) {
                Set<Integer> mobSet = map.get(lrDiag).keySet();
                for (int mob : mobSet)
                    mobility.add(lrDiagToBoardPosition(i, mob));
            }
        }

        // RL diagonals
        for (int i = 0; i < NUM_RL_DIAGS; i++) {
            String rlDiag = getRLDiag(i);
            if (map.containsKey(rlDiag)) {
                Set<Integer> mobSet = map.get(rlDiag).keySet();
                for (int mob : mobSet)
                    mobility.add(rlDiagToBoardPosition(i, mob));
            }
        }

        return mobility;
    }

    private String getRow(int index) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < WIDTH; j++) {
            sb.append(grid[index][j]);
        }
        return sb.toString();
    }

    private String getCol(int index) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < HEIGHT; j++) {
            sb.append(grid[j][index]);
        }
        return sb.toString();
    }

    private String getLRDiag(int index) {
        int diagLen = getDiagLen(index);
        int startRowIdx = index < WIDTH ? 0 : index - WIDTH + 1; // can use Math.max(..., ...)
        int startColIdx = index < WIDTH ? WIDTH - 1 - index : 0; // can use Math.min(..., ...)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < diagLen; i++) {
            sb.append(grid[startRowIdx + i][startColIdx + i]);
        }
        return sb.toString();
    }

    private String getRLDiag(int index) {
        int diagLen = getDiagLen(index);
        int startRowIdx = index < WIDTH ? 0 : index - WIDTH + 1;
        int startColIdx = index < WIDTH ? index : WIDTH - 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < diagLen; i++) {
            sb.append(grid[startRowIdx + i][startColIdx - i]);
        }
        return sb.toString();
    }

    private static int getDiagLen(int diagIndex) {
        return Math.min(diagIndex + 1, WIDTH + HEIGHT - 1 - diagIndex);
    }

    /**
     * Must be a valid input
     *
     * @param move
     */
    private void flip(int move) {
        if (grid[move / WIDTH][move % WIDTH] == '1') {
            grid[move / WIDTH][move % WIDTH] = '2';
            numBlackPieces--;
            numWhitePieces++;
        } else {
            grid[move / WIDTH][move % WIDTH] = '1';
            numBlackPieces++;
            numWhitePieces--;
        }
    }

    private int getltorDiagIndex(int position) {
        int rowIndex = position / WIDTH;
        int colIndex = position % WIDTH;
        return rowIndex - colIndex + WIDTH - 1;
    }

    private int getrtolDiagIndex(int position) {
        int rowIndex = position / WIDTH;
        int colIndex = position % WIDTH;
        return rowIndex + colIndex;
    }

    private int getIndexOnLtoR(int position) {
        int rowIndex = position / WIDTH;
        int colIndex = position % WIDTH;
        int ltorIdx = getltorDiagIndex(position);
        return ltorIdx < WIDTH ? rowIndex : colIndex;
    }

    private int getIndexOnRtoL(int position) {
        int rowIndex = position / WIDTH;
        int colIndex = position % WIDTH;
        int rtolIdx = getrtolDiagIndex(position);
        return rtolIdx < WIDTH ? rowIndex : WIDTH - 1 - colIndex;
    }

    private int lrDiagToBoardPosition(int lrIndex, int indexOnLR) {
        if (lrIndex < WIDTH) {
            int rowIndex = indexOnLR;
            int colIndex = (WIDTH - 1 - lrIndex) + indexOnLR;
            return rowIndex * WIDTH + colIndex;
        } else {
            int rowIndex = (lrIndex - WIDTH + 1) + indexOnLR;
            int colIndex = indexOnLR;
            return rowIndex * WIDTH + colIndex;
        }
    }

    private int rlDiagToBoardPosition(int rlIndex, int indexOnRL) {
        if (rlIndex < WIDTH) {
            int rowIndex = indexOnRL;
            int colIndex = rlIndex - indexOnRL;
            return rowIndex * WIDTH + colIndex;
        } else {
            int rowIndex = (rlIndex - WIDTH + 1) + indexOnRL;
            int colIndex = WIDTH - 1 - indexOnRL;
            return rowIndex * WIDTH + colIndex;
        }
    }

    public void render() {
        System.out.println("   1 2 3 4 5 6 7 8");
        char firstPlayerChar = '\u25CF';
        char secondPlayerChar = '\u25CB';
        char emptyLocChar = '\u25A1';

        for (int i = 0; i < grid.length; i++) {
            System.out.print(i + 1);
            if (i < 9)
                System.out.print("\u0020\u0020");
            else
                System.out.print("\u0020");
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] == '0')
                    System.out.print(emptyLocChar + "\u0020");
                else if (grid[i][j] == '1')
                    System.out.print(firstPlayerChar + "\u0020");
                else
                    System.out.print(secondPlayerChar + "\u0020");
            }
            System.out.print('\n');
        }
    }
}
