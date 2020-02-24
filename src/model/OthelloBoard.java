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
    private static final Set<Integer> corners;
    protected char[][] grid; // '0' for unoccupied, '1' for black, and '2' for white
    private Set<Integer> blackMobility;
    private Set<Integer> whiteMobility;
    private List<Integer> moveSequence;
    private List<Boolean> turnHistory;
    private Map<Integer, Set<Integer>> historicalFlips;
    private int numBlackPieces = 0;
    private int numWhitePieces = 0;
    private int blackCorners = 0;
    private int whiteCorners = 0;
    private int blackInstability = 0;
    private int whiteInstability = 0;

    // Original scores:
    // 20, -3, 11, 8, -7, 4, 1, 2, 2, -3
    // The above scores are taken from someone else's work
    private static final int TYPE_1_PV = 25;
    private static final int TYPE_2_PV = -3;
    private static final int TYPE_3_PV = 9;
    private static final int TYPE_4_PV = 6;
    private static final int TYPE_5_PV = -12;
    private static final int TYPE_6_PV = -4;
    private static final int TYPE_7_PV = 1;
    private static final int TYPE_8_PV = 3;
    private static final int TYPE_9_PV = 2;
    private static final int TYPE_0_PV = -3;
    private static final int[][] PIECE_VALUE_CHART =
            {
                    {TYPE_1_PV, TYPE_2_PV, TYPE_3_PV, TYPE_4_PV, TYPE_4_PV, TYPE_3_PV, TYPE_2_PV, TYPE_1_PV},
                    {TYPE_2_PV, TYPE_5_PV, TYPE_6_PV, TYPE_7_PV, TYPE_7_PV, TYPE_6_PV, TYPE_5_PV, TYPE_2_PV},
                    {TYPE_3_PV, TYPE_6_PV, TYPE_8_PV, TYPE_9_PV, TYPE_9_PV, TYPE_8_PV, TYPE_6_PV, TYPE_3_PV},
                    {TYPE_4_PV, TYPE_7_PV, TYPE_9_PV, TYPE_0_PV, TYPE_0_PV, TYPE_9_PV, TYPE_7_PV, TYPE_4_PV},
                    {TYPE_4_PV, TYPE_7_PV, TYPE_9_PV, TYPE_0_PV, TYPE_0_PV, TYPE_9_PV, TYPE_7_PV, TYPE_4_PV},
                    {TYPE_3_PV, TYPE_6_PV, TYPE_8_PV, TYPE_9_PV, TYPE_9_PV, TYPE_8_PV, TYPE_6_PV, TYPE_3_PV},
                    {TYPE_2_PV, TYPE_5_PV, TYPE_6_PV, TYPE_7_PV, TYPE_7_PV, TYPE_6_PV, TYPE_5_PV, TYPE_2_PV},
                    {TYPE_1_PV, TYPE_2_PV, TYPE_3_PV, TYPE_4_PV, TYPE_4_PV, TYPE_3_PV, TYPE_2_PV, TYPE_1_PV},
            };

    // active player: true = black, false = white
    private boolean turn = true;

    // key is the line, value is the valid position -> pieces to flip map
    // they are fundamentals to our calculation so they should be static
    private static Map<String, Map<Integer, Set<Integer>>> cacheMapBlack;
    private static Map<String, Map<Integer, Set<Integer>>> cacheMapWhite;

    static {
        cacheMapBlack = new HashMap<>();
        cacheMapWhite = new HashMap<>();
        countFlipAll();
        corners = new HashSet<>();
        corners.add(0);
        corners.add(WIDTH - 1);
        corners.add(WIDTH*HEIGHT - 1);
        corners.add(WIDTH*(HEIGHT - 1));
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
     * @return the next 3-radix string
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

    public OthelloBoard(OthelloBoard other) {
        blackMobility = new HashSet<>();
        blackMobility.addAll(other.blackMobility);
        whiteMobility = new HashSet<>();
        whiteMobility.addAll(other.whiteMobility);
        // Historical flips ignored because it's not important
        historicalFlips = new HashMap<>();
        moveSequence = new ArrayList<>();
        moveSequence.addAll(other.moveSequence);
        turnHistory = new ArrayList<>();
        turnHistory.addAll(other.turnHistory);
        grid = new char[8][8];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = other.grid[i][j];
            }
        }

        numBlackPieces = other.numBlackPieces;
        numWhitePieces = other.numWhitePieces;
        blackCorners = other.blackCorners;
        whiteCorners = other.whiteCorners;
        blackInstability = other.blackInstability;
        whiteInstability = other.whiteInstability;
        turn = other.turn;
    }

    public int getNumBlack() {
        return numBlackPieces;
    }

    public int getNumWhite() {
        return numWhitePieces;
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

    public int evaluateIntermediate() {
        // Considering the following 4 metrics:
        // 1) pieces for each side
        // 2) mobilities for each side (how many moves can be played for the next move)
        // 3) corners occupied for each side
        // 4) unstable pieces for each side
//        int pieceDiff = (int) (100 * (double) (numBlackPieces - numWhitePieces) / (double) (numBlackPieces + numWhitePieces));

        // TODO calculate frontier disks
        int pieceDiff = getPieceDiff() * 2; // originally, the constant was 10
        int mobilityDiff = (int) (100 * ((double) (blackMobility.size() - whiteMobility.size()) / (double) (blackMobility.size() + whiteMobility.size())));
        int cornerDiff = 25 * (blackCorners - whiteCorners);
        int flipProneDiff = (int) (100 * (double) (blackInstability - whiteInstability) / (double) (blackInstability + whiteInstability));
        int cornerAdj = 100 * getCornerAdjacency();
//        int badLocDiff = getBadLocOffset() * (getremainingPieces() / 4);

        return (int) Math.round((double) (pieceDiff + 5 * mobilityDiff + 40 * cornerDiff - 0.5 * flipProneDiff - 5 * cornerAdj) / 10.0);
    }

    private int getPieceDiff() {
        int absScore = 0;
        for (int i = 0; i < grid.length; i++)
        {
            for (int j = 0; j < grid[i].length; j++)
            {
                if (grid[i][j] != '0')
                {
                    absScore += (grid[i][j] == '1' ? PIECE_VALUE_CHART[i][j] : -PIECE_VALUE_CHART[i][j]);
                }
            }
        }
        return absScore;
    }

    private int getCornerAdjacency() {
        int cornerAdj = 0;

        if (grid[0][0] == '0')
        {
            if (grid[0][1] != '0')
            {
                cornerAdj += grid[0][1] == '1' ? 1 : -1;
            }

            if (grid[1][0] != '0')
            {
                cornerAdj += grid[1][0] == '1' ? 1 : -1;
            }

            if (grid[1][1] != '0')
            {
                cornerAdj += grid[1][1] == '1' ? 2 : -2; // X squares are +-2
            }
        }

        if (grid[0][WIDTH - 1] == '0')
        {
            if (grid[0][WIDTH - 2] != '0')
            {
                cornerAdj += grid[0][WIDTH - 2] == '1' ? 1 : -1;
            }

            if (grid[1][WIDTH - 1] != '0')
            {
                cornerAdj += grid[1][WIDTH - 1] == '1' ? 1 : -1;
            }

            if (grid[1][WIDTH - 2] != '0')
            {
                cornerAdj += grid[1][WIDTH - 2] == '1' ? 2 : -2; // X squares are +-2
            }
        }

        if (grid[HEIGHT - 1][0] == '0')
        {
            if (grid[HEIGHT - 1][1] != '0')
            {
                cornerAdj += grid[HEIGHT - 1][1] == '1' ? 1 : -1;
            }

            if (grid[HEIGHT - 2][0] != '0')
            {
                cornerAdj += grid[HEIGHT - 2][0] == '1' ? 1 : -1;
            }

            if (grid[HEIGHT - 2][1] != '0')
            {
                cornerAdj += grid[HEIGHT - 2][1] == '1' ? 2 : -2; // X squares are +-2
            }
        }

        if (grid[HEIGHT - 1][WIDTH - 1] == '0')
        {
            if (grid[HEIGHT - 2][WIDTH - 1] != '0')
            {
                cornerAdj += grid[HEIGHT - 2][WIDTH - 1] == '1' ? 1 : -1;
            }

            if (grid[HEIGHT - 1][WIDTH - 2] != '0')
            {
                cornerAdj += grid[HEIGHT - 1][WIDTH - 2] == '1' ? 1 : -1;
            }

            if (grid[HEIGHT - 2][WIDTH - 2] != '0')
            {
                cornerAdj += grid[HEIGHT - 2][WIDTH - 2] == '1' ? 2 : -2; // X squares are +-2
            }
        }

        return cornerAdj;
    }

    public int getremainingPieces() {
        return WIDTH * HEIGHT - moveSequence.size() - 4; // initially there were 4 pieces on the board
    }

    private int getBadLocOffset() {
        char[] corners = {grid[1][1], grid[1][WIDTH - 2], grid[HEIGHT - 2][1], grid[HEIGHT - 2][WIDTH - 2]};
        int blackBad = 0, whiteBad = 0;
        for (int i = 0; i < 4; i++) {
            if (corners[i] == '1')
                blackBad++;
            else if (corners[i] == '2')
                whiteBad++;
        }

        return blackBad - whiteBad;
    }

    public Set<Integer> getMobility() {
        return turn ? blackMobility : whiteMobility;
    }

    public void reset() {
        blackMobility.clear();
        whiteMobility.clear();
        historicalFlips.clear();
        moveSequence.clear();
        turnHistory.clear();
        turn = true;
        blackCorners = 0;
        whiteCorners = 0;
        blackInstability = 0;
        whiteInstability = 0;

        // reset grid
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = '0';
            }
        }
        initialUpdate();
    }

    private void initialUpdate() {
        grid[WIDTH / 2 - 1][HEIGHT / 2 - 1] = '2';
        grid[WIDTH / 2 - 1][HEIGHT / 2] = '1';
        grid[WIDTH / 2][HEIGHT / 2 - 1] = '1';
        grid[WIDTH / 2][HEIGHT / 2] = '2';
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
        if (corners.contains(move)) {
            if (turn)
                blackCorners++;
            else
                whiteCorners++;
        }

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

    public int getInc(int move) {
        // TODO potential deficiency here
        int prev = evaluateIntermediate();
        updateBoard(move);
        int now = evaluateIntermediate();
        withdraw();
        return now - prev;
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

    /**
     * withdraw is only valid if the last move is withdrawn
     * withdraw the last move
     */
    public void withdraw() {
        if (moveSequence.isEmpty())
            return;
        // TODO potential inefficiency
        int lastMove = moveSequence.get(moveSequence.size() - 1);
        moveSequence.remove((Integer) lastMove);
        if (historicalFlips.containsKey(lastMove)) {
            Set<Integer> flipped = historicalFlips.remove(lastMove);
            if (grid[lastMove / WIDTH][lastMove % WIDTH] == '1') {
                numBlackPieces--;
                if (corners.contains(lastMove))
                    blackCorners--;
            } else {
                numWhitePieces--;
                if (corners.contains(lastMove))
                    whiteCorners--;
            }
            grid[lastMove / WIDTH][lastMove % WIDTH] = '0';
            for (int loc : flipped) {
                flip(loc);
            }
        }

        blackMobility = countMobility(true);
        whiteMobility = countMobility(false);
        turn = turnHistory.get(turnHistory.size() - 1);
        turnHistory.remove(turnHistory.size() - 1);
    }

    private Set<Integer> countMobility(boolean isFirst) {
        Set<Integer> mobility = new HashSet<>();
        Set<Integer> flipProne = new HashSet<>();
        Map<String, Map<Integer, Set<Integer>>> map = isFirst ? cacheMapBlack : cacheMapWhite;

        // Rows
        for (int i = 0; i < HEIGHT; i++) {
            String row = getRow(i);
            if (map.containsKey(row)) {
                for (Map.Entry<Integer, Set<Integer>> entry : map.get(row).entrySet()) {
                    mobility.add(i * WIDTH + entry.getKey());
                    for (int canFlip : entry.getValue()) {
                        flipProne.add(i * WIDTH + canFlip);
                    }
                }
            }
        }

        // Cols
        for (int i = 0; i < WIDTH; i++) {
            String col = getCol(i);
            if (map.containsKey(col)) {
                for (Map.Entry<Integer, Set<Integer>> entry : map.get(col).entrySet()) {
                    mobility.add(entry.getKey() * WIDTH + i);
                    for (int canFlip : entry.getValue()) {
                        flipProne.add(canFlip * WIDTH + i);
                    }
                }
            }
        }

        // LR diagonals
        for (int i = 0; i < NUM_LR_DIAGS; i++) {
            String lrDiag = getLRDiag(i);
            if (map.containsKey(lrDiag)) {
                for (Map.Entry<Integer, Set<Integer>> entry : map.get(lrDiag).entrySet()) {
                    mobility.add(lrDiagToBoardPosition(i, entry.getKey()));
                    for (int canFlip : entry.getValue()) {
                        flipProne.add(lrDiagToBoardPosition(i, canFlip));
                    }
                }
            }
        }

        // RL diagonals
        for (int i = 0; i < NUM_RL_DIAGS; i++) {
            String rlDiag = getRLDiag(i);
            if (map.containsKey(rlDiag)) {
                for (Map.Entry<Integer, Set<Integer>> entry : map.get(rlDiag).entrySet()) {
                    mobility.add(rlDiagToBoardPosition(i, entry.getKey()));
                    for (int canFlip : entry.getValue()) {
                        flipProne.add(rlDiagToBoardPosition(i, canFlip));
                    }
                }
            }
        }

        if (isFirst) whiteInstability = flipProne.size();
        else blackInstability = flipProne.size();

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
        System.out.println("   A B C D E F G H");
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
