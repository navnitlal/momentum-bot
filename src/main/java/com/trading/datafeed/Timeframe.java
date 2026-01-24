package com.trading.datafeed;

import java.util.Arrays;
import java.util.List;

public enum Timeframe {
    TICK(0, 0),
    SEC5(5, 720),
    SEC10(10, 360),
    SEC30(30, 120),
    MIN1(60, 60);

    private final int seconds;
    private final int maxBars;

    Timeframe(int seconds, int maxBars) {
        this.seconds = seconds;
        this.maxBars = maxBars;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getMaxBars() {
        return maxBars;
    }

    public static List<Timeframe> getHierarchy() {
        return Arrays.asList(SEC5, SEC10, SEC30, MIN1);
    }

    public static List<Timeframe> hierarchyFrom(Timeframe base) {
        List<Timeframe> full = getHierarchy();
        int index = full.indexOf(base);
        if (index < 0 || index == full.size() - 1) return List.of();
        return full.subList(index + 1, full.size());
    }
}