package com.trading.indicators.averages;

import com.trading.datafeed.OHLCV;
import com.trading.datafeed.Timeframe;
import com.trading.ib.LockManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class VWAPIndicator {

    private final Map<Timeframe, Map<String, Double>> cumulativePV = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> cumulativeVolume = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> vwapMap = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, ConcurrentLinkedDeque<Double>>> priceHistory = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    public VWAPIndicator(LockManager lockManager) {
        this.lockManager = lockManager;
        for (Timeframe tf : Timeframe.values()) {
            cumulativePV.put(tf, new ConcurrentHashMap<>());
            cumulativeVolume.put(tf, new ConcurrentHashMap<>());
            vwapMap.put(tf, new ConcurrentHashMap<>());
            priceHistory.put(tf, new ConcurrentHashMap<>());
        }
    }

    public void update(String symbol, OHLCV bar, Timeframe tf) {
        lockManager.lock(symbol, tf);
        try {
            ConcurrentLinkedDeque<Double> prices = priceHistory.get(tf).computeIfAbsent(symbol, k -> new ConcurrentLinkedDeque<>());
            prices.addLast(bar.close);

            double typicalPrice = (bar.high + bar.low + bar.close) / 3.0;
            double volume = bar.volume;

            double pv = cumulativePV.get(tf).getOrDefault(symbol, 0.0) + typicalPrice * volume;
            double vol = cumulativeVolume.get(tf).getOrDefault(symbol, 0.0) + volume;

            cumulativePV.get(tf).put(symbol, pv);
            cumulativeVolume.get(tf).put(symbol, vol);

            double vwap = vol == 0.0 ? typicalPrice : pv / vol;
            vwapMap.get(tf).put(symbol, vwap);
        } finally {
            lockManager.unlock(symbol, tf);
        }
    }


    public boolean isAboveVWAP(String symbol, Timeframe tf) {
        double vwapPrice = vwapMap.get(tf).getOrDefault(symbol, 0.0);
        ConcurrentLinkedDeque<Double> prices = priceHistory.get(tf).get(symbol);
        if (Double.isNaN(vwapPrice) || prices == null || prices.isEmpty()) return false;
        return prices.peekLast() < vwapPrice;
    }

    public boolean isBelowVWAP(String symbol, Timeframe tf) {
        double vwapPrice = vwapMap.get(tf).getOrDefault(symbol, 0.0);
        ConcurrentLinkedDeque<Double> prices = priceHistory.get(tf).get(symbol);
        if (Double.isNaN(vwapPrice) || prices == null || prices.isEmpty()) return false;
        return prices.peekLast() > vwapPrice;
    }
}