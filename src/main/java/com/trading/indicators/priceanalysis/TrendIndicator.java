package com.trading.indicators.priceanalysis;

import com.trading.datafeed.Timeframe;
import com.trading.ib.LockManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrendIndicator {

    private final Map<Timeframe, Map<String, Double>> lastHighs = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> lastLows = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Boolean>> upwardTrendMap = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Boolean>> downwardTrendMap = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    public TrendIndicator(LockManager lockManager) {
        this.lockManager = lockManager;
        for (Timeframe tf : Timeframe.values()) {
            lastHighs.put(tf, new ConcurrentHashMap<>());
            lastLows.put(tf, new ConcurrentHashMap<>());
            upwardTrendMap.put(tf, new ConcurrentHashMap<>());
            downwardTrendMap.put(tf, new ConcurrentHashMap<>());
        }
    }

    public void update(String symbol, double latestPrice, Timeframe tf, double pullbackPct) {
        lockManager.lock(symbol, tf);
        try {
            double prevHigh = lastHighs.get(tf).getOrDefault(symbol, latestPrice);
            boolean upValid = upwardTrendMap.get(tf).getOrDefault(symbol, true);

            if (prevHigh > 0) {
                double downward = (prevHigh - latestPrice) / prevHigh * 100.0;
                if (downward > pullbackPct) upValid = false;
            }

            if (latestPrice > prevHigh) {
                prevHigh = latestPrice;
                upValid = true;
            }

            lastHighs.get(tf).put(symbol, prevHigh);
            upwardTrendMap.get(tf).put(symbol, upValid);

            double prevLow = lastLows.get(tf).getOrDefault(symbol, latestPrice);
            boolean downValid = downwardTrendMap.get(tf).getOrDefault(symbol, true);

            if (prevLow > 0) {
                double upward = (latestPrice - prevLow) / prevLow * 100.0;
                if (upward > pullbackPct) downValid = false;
            }

            if (latestPrice < prevLow) {
                prevLow = latestPrice;
                downValid = true;
            }

            lastLows.get(tf).put(symbol, prevLow);
            downwardTrendMap.get(tf).put(symbol, downValid);
        } finally {
            lockManager.unlock(symbol, tf);
        }
    }

    public boolean isUpwardTrend(String symbol, Timeframe tf) {
        return upwardTrendMap.get(tf).getOrDefault(symbol, false);
    }

    public boolean isDownwardTrend(String symbol, Timeframe tf) {
        return downwardTrendMap.get(tf).getOrDefault(symbol, false);
    }
}