package com.trading.indicators.averages;

import com.trading.datafeed.Timeframe;
import com.trading.ib.LockManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Map;

public class BollingerBandsIndicator {

    private final Map<Timeframe, Map<String, ConcurrentLinkedDeque<Double>>> priceHistory = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, BollingerBands>> bands = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> rollingSum = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> rollingSumSquares = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    public BollingerBandsIndicator(LockManager lockManager) {
        this.lockManager = lockManager;
        for (Timeframe tf : Timeframe.values()) {
            priceHistory.put(tf, new ConcurrentHashMap<>());
            bands.put(tf, new ConcurrentHashMap<>());
            rollingSum.put(tf, new ConcurrentHashMap<>());
            rollingSumSquares.put(tf, new ConcurrentHashMap<>());
        }
    }

    public void update(String symbol, double closePrice, Timeframe tf, int period, double multiplier) {
        lockManager.lock(symbol, tf);
        try {
            ConcurrentLinkedDeque<Double> prices = priceHistory.get(tf).computeIfAbsent(symbol, k -> new ConcurrentLinkedDeque<>());
            prices.addLast(closePrice);

            double sum = rollingSum.get(tf).getOrDefault(symbol, 0.0) + closePrice;
            double sumSq = rollingSumSquares.get(tf).getOrDefault(symbol, 0.0) + closePrice * closePrice;

            if (prices.size() > period) {
                double removed = prices.pollFirst();
                sum -= removed;
                sumSq -= removed * removed;
            }

            rollingSum.get(tf).put(symbol, sum);
            rollingSumSquares.get(tf).put(symbol, sumSq);

            if (prices.size() < period) return;

            double mean = sum / prices.size();
            double variance = Math.max(0.0, sumSq / prices.size() - mean * mean);
            double stdDev = Math.sqrt(variance);

            bands.get(tf).put(symbol, new BollingerBands(
                    mean + multiplier * stdDev,
                    mean,
                    mean - multiplier * stdDev
            ));
        } finally {
            lockManager.unlock(symbol, tf);
        }
    }

    public boolean isAboveUpper(String symbol, Timeframe tf) {
        BollingerBands bb = bands.get(tf).get(symbol);
        ConcurrentLinkedDeque<Double> prices = priceHistory.get(tf).get(symbol);
        if (bb == null || prices == null || prices.isEmpty()) return false;
        return prices.peekLast() > bb.upper();
    }

    public boolean isBelowLower(String symbol, Timeframe tf) {
        BollingerBands bb = bands.get(tf).get(symbol);
        ConcurrentLinkedDeque<Double> prices = priceHistory.get(tf).get(symbol);
        if (bb == null || prices == null || prices.isEmpty()) return false;
        return prices.peekLast() < bb.lower();
    }
}