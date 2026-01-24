package com.trading.indicators.momentum;

import com.trading.datafeed.Timeframe;
import com.trading.ib.LockManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MACDIndicator {

    private final Map<Timeframe, Map<String, Double>> emaFast = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> emaSlow = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> macdLine = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> signalLine = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    public MACDIndicator(LockManager lockManager) {
        this.lockManager = lockManager;
        for (Timeframe tf : Timeframe.values()) {
            emaFast.put(tf, new ConcurrentHashMap<>());
            emaSlow.put(tf, new ConcurrentHashMap<>());
            macdLine.put(tf, new ConcurrentHashMap<>());
            signalLine.put(tf, new ConcurrentHashMap<>());
        }
    }

    public void update(String symbol, double price, Timeframe tf, int fastPeriod, int slowPeriod, int signalPeriod) {
        lockManager.lock(symbol, tf);
        try {
            double fast = emaFast.get(tf).getOrDefault(symbol, price);

            double slow = emaSlow.get(tf).getOrDefault(symbol, price);

            double kFast = 2.0 / (fastPeriod + 1);
            double kSlow = 2.0 / (slowPeriod + 1);

            fast = fast + kFast * (price - fast);
            slow = slow + kSlow * (price - slow);

            emaFast.get(tf).put(symbol, fast);
            emaSlow.get(tf).put(symbol, slow);

            double macd = fast - slow;
            double prevSignal = signalLine.get(tf).getOrDefault(symbol, macd);
            double kSignal = 2.0 / (signalPeriod + 1);
            double newSignal = prevSignal + kSignal * (macd - prevSignal);

            macdLine.get(tf).put(symbol, macd);
            signalLine.get(tf).put(symbol, newSignal);
        } finally {
            lockManager.unlock(symbol, tf);
        }
    }

    public double getHistogram(String symbol, Timeframe tf) {
        double macd = macdLine.getOrDefault(tf, Map.of()).getOrDefault(symbol, 0.0);
        double signal = signalLine.getOrDefault(tf, Map.of()).getOrDefault(symbol, 0.0);
        return macd - signal;
    }
}