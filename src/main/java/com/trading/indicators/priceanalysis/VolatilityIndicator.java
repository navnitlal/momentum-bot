package com.trading.indicators.priceanalysis;

import com.trading.datafeed.Timeframe;
import com.trading.ib.LockManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VolatilityIndicator {
    private final Map<Timeframe, Map<String, Double>> meanMap = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> varSumMap = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Integer>> countMap = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    public VolatilityIndicator(LockManager lockManager) {
        this.lockManager = lockManager;
        for (Timeframe tf : Timeframe.values()) {
            meanMap.put(tf, new ConcurrentHashMap<>());
            varSumMap.put(tf, new ConcurrentHashMap<>());
            countMap.put(tf, new ConcurrentHashMap<>());
        }
    }

    public void update(String symbol, double price, Timeframe tf) {
        lockManager.lock(symbol, tf);
        try {
            double prevMean = meanMap.get(tf).getOrDefault(symbol, 0.0);

            double prevVarSum = varSumMap.get(tf).getOrDefault(symbol, 0.0);
            int prevCount = countMap.get(tf).getOrDefault(symbol, 0) + 1;

            double delta = price - prevMean;
            double newMean = prevMean + delta / prevCount;
            double newVarSum = prevVarSum + delta * (price - newMean);

            meanMap.get(tf).put(symbol, newMean);
            varSumMap.get(tf).put(symbol, newVarSum);
            countMap.get(tf).put(symbol, prevCount);
        } finally {
            lockManager.unlock(symbol, tf);
        }
    }

    private double getVariance(String symbol, Timeframe tf) {
        int count = countMap.get(tf).getOrDefault(symbol, 0);
        if (count < 2) return 0.0;
        return varSumMap.get(tf).getOrDefault(symbol, 0.0) / (count - 1);
    }

    public double getVolatility(String symbol, Timeframe tf) {
        return Math.sqrt(getVariance(symbol, tf));
    }
}