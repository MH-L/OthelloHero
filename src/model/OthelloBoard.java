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
    private List<Byte> mobility;
    private int totalPieces = 0;

    // Stores which positions are valid next moves for each line for white
    // The key of value (most significant) store valid locations, and its corresponding key is the flip pattern
    private static List<Map<Short, Map<Integer, Short>>> flipMapWhite;

    // Stores which positions are valid next moves for each line for black
    private static List<Map<Short, Map<Integer, Short>>> flipMapBlack;

    private static Map<String, Map<Integer, Set<Integer>>> cacheMapBlack;
    private static Map<String, Map<Integer, Set<Integer>>> cacheMapWhite;

    private static short[] flipPatternToAdder;

    static {
        // Use max in case we want rectangular boards (although I doubt it makes sense)
        flipMapWhite = new ArrayList<>(Math.max(WIDTH, HEIGHT));
        flipMapBlack = new ArrayList<>(Math.max(WIDTH, HEIGHT));
        for (int i = 0; i < Math.max(WIDTH, HEIGHT); i++) {
            flipMapWhite.add(new HashMap<>());
            flipMapBlack.add(new HashMap<>());
        }

        cacheMapBlack = new HashMap<>();
        cacheMapWhite = new HashMap<>();
        countFlipAll();

        initializeAdder();
    }

    private static void initializeAdder() {
        flipPatternToAdder = new short[256]; // 256 is the number of variations of byte
        // NO need to convert for 0 here
        for (int i = 1; i < flipPatternToAdder.length; i++) {
            String binString = Integer.toBinaryString(i);
            short result = 0;
            for (int j = 0; j < binString.length(); j++) {
                if (binString.charAt(j) == '1') {
                    result += (1 << (j * 2));
                }
            }

            flipPatternToAdder[i] = result;
        }
    }

    private static void calculateFlipMap() {
        // TODO how to enumerate all possibilities?
        for (int len = 8; len >= 3; len--) {
            for (int i = 0; i < Math.pow(3, len); i++) {
                String thirdString = Integer.toString(i, 3);
                short curLine = Short.parseShort(thirdString, 4);
                Map<Integer, Short> retVal = findFlipSequence(curLine, len, true);
                Map<Integer, Short> retVal2 = findFlipSequence(curLine, len, false);
                if (!retVal.isEmpty())
                    flipMapBlack.get(len).put(curLine, retVal);
                if (!retVal2.isEmpty())
                    flipMapWhite.get(len).put(curLine, retVal2);

            }
        }
    }

    private static Map<Integer, Short> findFlipSequence(short line, int length, boolean isFirst) {
        short selfPiece = isFirst ? BLACK_PIECE : WHITE_PIECE;
        short oppPiece = isFirst ? WHITE_PIECE : BLACK_PIECE;
        Map<Integer, Short> returnVal = new HashMap<>();
        for (int i = 0; i < length; i++) {
            short currentPiece = (short) ((line << (14 - i*2)) >> 14);
            if (currentPiece != NO_PIECE)
                continue;
            for (int j = i + 1; j < length; j++) {
                short curPiece = (short) ((line << (14 - j*2)) >> 14);
                if (curPiece == selfPiece && j > i + 1) {
                    short flipper = 0;
                    for (int k = i + 1; k < j; k++) {
                        flipper += (1 << (k*2));
                    }
                    returnVal.put(i, flipper);
                    break;
                } else if (curPiece == NO_PIECE)
                    break;
            }
        }
        // todo need another pass

        return returnVal;
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
                    cacheMapWhite.put(next, result);
                if (!result2.isEmpty())
                    cacheMapBlack.put(next, result);
            } while (!(next = nextStr(next)).isEmpty());
        }
    }

    /**
     * Find next 3-radix string using elementary-school arithmetic
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
                if (innerPiece == selfPiece && j > i + 1) {
                    Set<Integer> s = new HashSet<>();
                    for (int k = i + 1; k < j; k++) {
                        s.add(k);
                    }
                    returnVal.put(i, s);
                } else if (innerPiece == '0')
                    break;
            }
        }

        for (int i = str.length() - 1; i >= 0; i--) {
            char curPiece = str.charAt(i);
            if (curPiece != '0')
                continue;
            for (int j = i - 1; j >= 0 ; j--) {
                char innerPiece = str.charAt(j);
                if (innerPiece == selfPiece && j < i - 1) {
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
        mobility = new ArrayList<>();

        grid = new char[8][8];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = '0';
            }
        }
    }

    public boolean updateBoard(int move, boolean isFirst) {
        short stone = isFirst ? BLACK_PIECE : WHITE_PIECE;
        if (totalPieces < 4) {
            // During initial setup phase
            int rowIndex = move / WIDTH;
            int colIndex = move % WIDTH;
            rows[rowIndex] |= (stone << (colIndex * 2));
            cols[colIndex] |= (stone << (rowIndex * 2));
            int ltorIndex = getltorDiagIndex(move);
            int rtolIndex = getrtolDiagIndex(move);
            int indexOnLR = getIndexOnLtoR(move);
            int indexOnRL = getIndexOnRtoL(move);
            ltorDiags[ltorIndex] |= (stone << (indexOnLR * 2));
            rtolDiags[rtolIndex] |= (stone << (indexOnRL * 2));
        }

        // TODO
        return false;
    }

    private static int getDiagLen(int diagIndex) {
        return Math.min(diagIndex + 1, WIDTH + HEIGHT - 1 - diagIndex);
    }

    /**
     * Must be a valid input
     * @param move
     */
    private void flip(int move) {
        if (grid[move / WIDTH][move % WIDTH] == '1')
            grid[move / WIDTH][move % WIDTH] = '2';
        else
            grid[move / WIDTH][move % WIDTH] = '1';
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
}
