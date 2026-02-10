package com.trading.datafeed;

import com.trading.ib.IBConnector;
import com.trading.ib.LockManager;
import com.trading.ib.RealTimeBarListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class BarManager implements RealTimeBarListener {

    private final Map<Timeframe, Map<String, OHLCV>> currentOpenBars = new ConcurrentHashMap<>();
    private final Map<Timeframe, Map<String, ConcurrentLinkedDeque<OHLCV>>> ohlcvData = new ConcurrentHashMap<>();
    private final Map<Timeframe, List<BiConsumer<String, OHLCV>>> barCloseListeners = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    public BarManager(IBConnector ib, LockManager lockManager) {
        ib.addRealTimeBarListener(this);
        this.lockManager = lockManager;
        for (Timeframe tf : Timeframe.getHierarchy()) {
            currentOpenBars.put(tf, new ConcurrentHashMap<>());
            ohlcvData.put(tf, new ConcurrentHashMap<>());
            barCloseListeners.put(tf, new CopyOnWriteArrayList<>());
        }
    }

    public void addBarCloseListener(Timeframe tf, BiConsumer<String, OHLCV> listener) {
        barCloseListeners.get(tf).add(listener);
    }

    private long alignTimestamp(long timestamp, int intervalSeconds) {
        return timestamp - (timestamp % intervalSeconds);
    }

    @Override
    public void onRealTimeBar(String symbol, long timestamp, double open, double high, double low, double close, long volume) {
        if (symbol == null) return;

        lockManager.lock(symbol);
        try {
            OHLCV lowerBar = processTimeframe(symbol, timestamp, open, high, low, close, volume);

            for (Timeframe higherTf : Timeframe.hierarchyFrom(Timeframe.SEC5)) {
                long alignedStart = alignTimestamp(lowerBar.startTime, higherTf.getSeconds());
                OHLCV higherBar = currentOpenBars.get(higherTf).get(symbol);
                if (higherBar == null || higherBar.startTime != alignedStart) {
                    if (higherBar != null) closeBar(symbol, higherTf, higherBar);
                    higherBar = new OHLCV(lowerBar.open, lowerBar.high, lowerBar.low, lowerBar.close, lowerBar.volume, alignedStart);
                    currentOpenBars.get(higherTf).put(symbol, higherBar);
                } else
                    higherBar.updateFrom(lowerBar);
                lowerBar = higherBar;
            }
            forceCloseBars(symbol, timestamp);
        } finally {
            lockManager.unlock(symbol);
        }
    }

    private OHLCV processTimeframe(String symbol, long timestamp, double open, double high, double low, double close, long volume) {
        Map<String, OHLCV> openBars = currentOpenBars.get(Timeframe.SEC5);
        long alignedStart = alignTimestamp(timestamp, Timeframe.SEC5.getSeconds());

        OHLCV bar = openBars.get(symbol);
        if (bar == null || bar.startTime != alignedStart) {
            if (bar != null) closeBar(symbol, Timeframe.SEC5, bar);

            bar = new OHLCV(open, high, low, close, volume, alignedStart);
            openBars.put(symbol, bar);
        } else
            bar.updateFrom(new OHLCV(open, high, low, close, volume, alignedStart));

        return bar;
    }

    private void forceCloseBars(String symbol, long timestamp) {
        for (Timeframe tf : Timeframe.getHierarchy()) {
            OHLCV bar = currentOpenBars.get(tf).get(symbol);
            if (bar != null && timestamp >= bar.startTime + tf.getSeconds()) {
                closeBar(symbol, tf, bar);
                currentOpenBars.get(tf).remove(symbol);
            }
        }
    }

    private void closeBar(String symbol, Timeframe tf, OHLCV bar) {
        bar.close(tf.getSeconds());
        ConcurrentLinkedDeque<OHLCV> bars = ohlcvData.get(tf).computeIfAbsent(symbol, k -> new ConcurrentLinkedDeque<>());
        bars.addLast(bar.copy());
        if (bars.size() > tf.getMaxBars()) bars.removeFirst();
        OHLCV snapshot = bar.copy();
        for (var listener : barCloseListeners.get(tf)) {
            listener.accept(symbol, snapshot);
        }
    }

    public void clearOHLCVData(String symbol) {
        lockManager.lock(symbol);
        try {
            for (Timeframe tf : Timeframe.getHierarchy()) {
                currentOpenBars.get(tf).remove(symbol);
                ohlcvData.get(tf).remove(symbol);
            }
        } finally {
            lockManager.unlock(symbol);
        }
    }
}