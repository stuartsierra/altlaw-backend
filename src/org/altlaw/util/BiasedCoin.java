package org.altlaw.util;

import java.util.Random;

/** Simulates flipping a biased coin with fixed probability. */
public class BiasedCoin {
    private double n;
    private Random random;

    /** Creates a biased coin.
     *
     * @param heads   the probability that this coin returns "heads"
     */
    public BiasedCoin(double heads) {
        this.n = heads;
        random = new Random();
    }

    /** Simulates a coin flip; returns true if the coin is "heads". */
    public boolean flip() {
        return random.nextDouble() < n;
    }
}
