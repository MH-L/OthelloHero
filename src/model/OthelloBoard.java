package model;

import java.util.*;

public class OthelloBoard {
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;
    private static final int NUM_LR_DIAGS = WIDTH + HEIGHT - 1;
    private static final int NUM_RL_DIAGS = WIDTH + HEIGHT - 1;
    private static final short BLACK_PIECE = 1;
    private static final short WHITE_PIECE = 2;
    private static final short NO_PIECE = 0;
    private short[] rows;
    private short[] cols;
    private short[] ltorDiags;
    private short[] rtolDiags;
    private char[][] grid;
    private Set<Integer> mobility;
    private int numWhitePieces = 0;
    private int numBlackPieces = 0;

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
                    } else break;
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
                    } else break;
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
        rows = new short[HEIGHT];
        cols = new short[WIDTH];
        ltorDiags = new short[HEIGHT + WIDTH - 1];
        rtolDiags = new short[HEIGHT + WIDTH - 1];
        mobility = new HashSet<>();

        grid = new char[8][8];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = '0';
            }
        }
        initialUpdate();
    }

    public void reset() {
        mobility.clear();

        // reset grid
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = '0';
            }
        }
        numWhitePieces = 0;
        numBlackPieces = 0;
    }

    private void initialUpdate() {
        grid[WIDTH / 2 - 1][HEIGHT / 2 - 1] = '1';
        grid[WIDTH / 2 - 1][HEIGHT / 2] = '2';
        grid[WIDTH / 2][HEIGHT / 2 - 1] = '2';
        grid[WIDTH / 2][HEIGHT / 2] = '1';
        countMobility(true);
        numBlackPieces = 2;
        numWhitePieces = 2;
    }

    public boolean updateBoard(int move, boolean isFirst) {
        if (!mobility.contains(move))
            return false;
        char piece = isFirst ? '1' : '2';
        Map<String, Map<Integer, Set<Integer>>> map = isFirst ? cacheMapBlack : cacheMapWhite;
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
        if (map.containsKey(row))
            for (int i : map.get(row).get(colIndex))
                flipSet.add(rowIndex * WIDTH + i);
        if (map.containsKey(col))
            for (int j : map.get(col).get(rowIndex))
                flipSet.add(j * WIDTH + colIndex);
        if (map.containsKey(lrDiag))
            for (int m : map.get(lrDiag).get(idxOnLR))
                flipSet.add(lrDiagToBoardPosition(lrDiagIdx, m));
        if (map.containsKey(rlDiag))
            for (int n : map.get(rlDiag).get(idxOnRL))
                flipSet.add(rlDiagToBoardPosition(rlDiagIdx, n));

        for (int p : flipSet)
            flip(p);
        grid[rowIndex][colIndex] = piece;

        // Don't forget to update the count here
        if (isFirst) numBlackPieces++;
        else numWhitePieces++;

        // Next turn is the opponent's
        countMobility(!isFirst);
        return true;
    }

    private void countMobility(boolean isFirst) {
        mobility.clear();
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
