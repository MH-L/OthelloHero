package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OthelloBoard {
    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;
    private static final int BLACK_PIECE = 1;
    private static final int WHITE_PIECE = 2;
    private short[] rows;
    private short[] cols;
    private short[] ltorDiags;
    private short[] rtolDiags;
    private List<Byte> mobility;

    // Stores which positions are valid next moves for each line for white
    // The first 8 bits of value (most significant) store flip pattern, and the last 8 bits store validity of positions
    private static List<Map<Short, Short>> flipMapWhite;

    // Stores which positions are valid next moves for each line for black
    private static List<Map<Short, Short>> flipMapBlack;

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

    }

    public OthelloBoard() {
        rows = new short[HEIGHT];
        cols = new short[WIDTH];
        ltorDiags = new short[WIDTH];
        rtolDiags = new short[HEIGHT];
    }
}
