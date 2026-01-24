package com.trading.indicators.momentum;

import com.trading.datafeed.Timeframe;
import com.trading.ib.LockManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RSIIndicator {

    private final Map<Timeframe, Map<String, Double>> avgGain = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> avgLoss = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> rsiMap = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    public RSIIndicator(LockManager lockManager) {
        this.lockManager = lockManager;
        for (Timeframe tf : Timeframe.values()) {
            avgGain.put(tf, new ConcurrentHashMap<>());
            avgLoss.put(tf, new ConcurrentHashMap<>());
            rsiMap.put(tf, new ConcurrentHashMap<>());
        }
    }

    public void update(String symbol, double closePrice, double prevClose, Timeframe tf, int period) {
        lockManager.lock(symbol, tf);
        try {
            double change = closePrice - prevClose;
            double gain = Math.max(change, 0);
            double loss = Math.max(-change, 0);

            double prevGain = avgGain.get(tf).getOrDefault(symbol, gain);
            double prevLoss = avgLoss.get(tf).getOrDefault(symbol, loss);

            double newAvgGain = (prevGain * (period - 1) + gain) / period;
            double newAvgLoss = (prevLoss * (period - 1) + loss) / period;

            avgGain.get(tf).put(symbol, newAvgGain);
            avgLoss.get(tf).put(symbol, newAvgLoss);

            double rs = newAvgLoss == 0 ? Double.POSITIVE_INFINITY : newAvgGain / newAvgLoss;
            double rsi = 100 - (100 / (1.0 + rs));
            rsiMap.get(tf).put(symbol, rsi);
        } finally {
            lockManager.unlock(symbol, tf);
        }
    }

    public double getRSI(String symbol, Timeframe tf) {
        return rsiMap.get(tf).getOrDefault(symbol, 50.0);
    }
}