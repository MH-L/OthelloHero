package model;

import java.util.Random;

public class Hasher {
    private static final long[][] blackRNs;
    private static final long[][] whiteRNs;

    static {
        blackRNs = new long[8][8];
        whiteRNs = new long[8][8];

        for (int i = 0; i < blackRNs.length; i++)
        {
            for (int j = 0; j < blackRNs[i].length; j++)
            {
                long blackRN = new Random().nextLong();
                long whiteRN = new Random().nextLong();

                blackRNs[i][j] = blackRN;
                whiteRNs[i][j] = whiteRN;
            }
        }
    }

    public static long hash(OthelloBoard bd)
    {
        long hashValue = 0;
        char[][] bdGrid = bd.grid;
        for (int i = 0; i < bdGrid.length; i++)
        {
            for (int j = 0; j < bdGrid[i].length; j++)
            {
                if (bdGrid[i][j] == '1')
                {
                    hashValue ^= blackRNs[i][j];
                }
                else if (bdGrid[i][j] == '2')
                {
                    hashValue ^= whiteRNs[i][j];
                }
            }
        }

        return hashValue;
    }
}
