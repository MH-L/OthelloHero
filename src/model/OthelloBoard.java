package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OthelloBoard {
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;
    private static final short BLACK_PIECE = 1;
    private static final short WHITE_PIECE = 2;
    private static final short NO_PIECE = 0;
    private short[] rows;
    private short[] cols;
    private short[] ltorDiags;
    private short[] rtolDiags;
    private List<Byte> mobility;
    private int totalPieces = 0;

    // Stores which positions are valid next moves for each line for white
    // The key of value (most significant) store valid locations, and its corresponding key is the flip pattern
    private static List<Map<Short, Map<Integer, Short>>> flipMapWhite;

    // Stores which positions are valid next moves for each line for black
    private static List<Map<Short, Map<Integer, Short>>> flipMapBlack;

    private static short[] flipPatternToAdder;

    static {
        // Use max in case we want rectangular boards (although I doubt it makes sense)
        flipMapWhite = new ArrayList<>(Math.max(WIDTH, HEIGHT));
        flipMapBlack = new ArrayList<>(Math.max(WIDTH, HEIGHT));
        for (int i = 0; i < Math.max(WIDTH, HEIGHT); i++) {
            flipMapWhite.add(new HashMap<>());
            flipMapBlack.add(new HashMap<>());
        }

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
        }

        // TODO
        return false;
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
