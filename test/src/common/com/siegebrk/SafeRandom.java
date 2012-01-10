/* Copyright 2009 Michael Dalton */
package com.siegebrk;

import java.util.Random;

public class SafeRandom extends Random
{
    public SafeRandom() { super(); }
    public SafeRandom(long seed) { super(seed); }

    public int nextInt(int n) {
        if (n <= 0) return 0;
        return super.nextInt(n);
    }
}
