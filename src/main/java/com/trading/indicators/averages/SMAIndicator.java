package com.trading.indicators.averages;

import com.trading.datafeed.Timeframe;
import com.trading.ib.LockManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SMAIndicator {

    private final Map<Timeframe, Map<String, ConcurrentLinkedDeque<Double>>> priceHistory = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> smaMap = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> rollingSum = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    public SMAIndicator(LockManager lockManager) {
        this.lockManager = lockManager;
        for (Timeframe tf : Timeframe.values()) {
            priceHistory.put(tf, new ConcurrentHashMap<>());
            smaMap.put(tf, new ConcurrentHashMap<>());
            rollingSum.put(tf, new ConcurrentHashMap<>());
        }
    }

    public void update(String symbol, double price, Timeframe tf, int period) {
        lockManager.lock(symbol, tf);
        try {
            ConcurrentLinkedDeque<Double> prices = priceHistory.get(tf).computeIfAbsent(symbol, k -> new ConcurrentLinkedDeque<>());
            prices.addLast(price);

            double sum = rollingSum.get(tf).getOrDefault(symbol, 0.0) + price;

            if (prices.size() > period) {
                double removed = prices.pollFirst();
                sum -= removed;
            }

            rollingSum.get(tf).put(symbol, sum);

            if (prices.size() < period) return;

            double sma = sum / period;
            smaMap.get(tf).put(symbol, sma);
        } finally {
            lockManager.unlock(symbol, tf);
        }
    }

    public boolean isUpwardTrend(String symbol, Timeframe tf) {
        Double sma = smaMap.get(tf).get(symbol);
        ConcurrentLinkedDeque<Double> prices = priceHistory.get(tf).get(symbol);
        if (sma == null || prices == null || prices.isEmpty()) return false;
        return prices.peekLast() < sma;
    }

    public boolean isDownwardTrend(String symbol, Timeframe tf) {
        Double sma = smaMap.get(tf).get(symbol);
        ConcurrentLinkedDeque<Double> prices = priceHistory.get(tf).get(symbol);
        if (sma == null || prices == null || prices.isEmpty()) return false;
        return prices.peekLast() > sma;
    }
}