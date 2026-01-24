package com.trading.indicators.momentum;

import com.trading.datafeed.Timeframe;
import com.trading.ib.LockManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RelativeVolumeIndicator {

    private final Map<Timeframe, Map<String, Double>> avgVolumeMap = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, Double>> relVolumeMap = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    public RelativeVolumeIndicator(LockManager lockManager) {
        this.lockManager = lockManager;
        for (Timeframe tf : Timeframe.values()) {
            avgVolumeMap.put(tf, new ConcurrentHashMap<>());
            relVolumeMap.put(tf, new ConcurrentHashMap<>());
        }
    }

    public void update(String symbol, double volume, Timeframe tf) {
        lockManager.lock(symbol, tf);
        try {
            double prevAvg = avgVolumeMap.get(tf).getOrDefault(symbol, volume);
            double newAvg = prevAvg == 0.0 ? volume : prevAvg + (volume - prevAvg) / 2.0;

            avgVolumeMap.get(tf).put(symbol, newAvg);
            relVolumeMap.get(tf).put(symbol, newAvg == 0.0 ? 0.0 : volume / newAvg);
        } finally {
            lockManager.unlock(symbol, tf);
        }
    }

    public double getRelativeVolume(String symbol, Timeframe tf) {
        return relVolumeMap.get(tf).getOrDefault(symbol, 1.0);
    }
}