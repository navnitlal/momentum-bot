package com.trading.indicators.orderbook;

import com.trading.datafeed.Level;
import com.trading.ib.LockManager;

import java.util.*;
import java.util.concurrent.*;

public class OrderBookIndicator {

    private final Map<String, NavigableMap<Double, Level>> bidBooks = new ConcurrentHashMap<>();
    private final Map<String, NavigableMap<Double, Level>> askBooks = new ConcurrentHashMap<>();
    private final Map<String, Double> obImbalance = new ConcurrentHashMap<>();
    private final Map<String, Double> obImbalanceDelta = new ConcurrentHashMap<>();

    private final LockManager lockManager;

    private final int depth = 5;

    public OrderBookIndicator(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public void updateOrderBook(String symbol, boolean isBid, double price, long size, int operation) {
        boolean lockAcquired = lockManager.tryLock(symbol);
        if (!lockAcquired) return;

        try{
            Map<String, NavigableMap<Double, Level>> bookMap = isBid ? bidBooks : askBooks;
            NavigableMap<Double, Level> book = bookMap.computeIfAbsent(symbol,
                    k -> isBid
                            ? new ConcurrentSkipListMap<>(Comparator.reverseOrder())
                            : new ConcurrentSkipListMap<>());

            if (operation == 2 || size == 0) book.remove(price);
            else book.put(price, new Level(price, size));

            while (book.size() > depth) {
                book.pollLastEntry();
            }

            updateImbalance(symbol);
        } finally {
            lockManager.unlock(symbol);
        }
    }

    private void updateImbalance(String symbol) {
        NavigableMap<Double, Level> bids = bidBooks.get(symbol);
        NavigableMap<Double, Level> asks = askBooks.get(symbol);

        if (bids == null || bids.isEmpty() || asks == null || asks.isEmpty()) {
            obImbalance.put(symbol, Double.NaN);
            return;
        }

        double midPrice = (bids.firstKey() + asks.firstKey()) / 2.0;
        double bidTotal = 0.0, askTotal = 0.0;
        int count = 0;

        for (Map.Entry<Double, Level> e : bids.entrySet()) {
            if (count++ >= depth) break;
            bidTotal += e.getValue().getSize() / (1.0 + Math.abs(e.getKey() - midPrice));
        }

        count = 0;
        for (Map.Entry<Double, Level> e : asks.entrySet()) {
            if (count++ >= depth) break;
            askTotal += e.getValue().getSize() / (1.0 + Math.abs(e.getKey() - midPrice));
        }

        double prev = obImbalance.getOrDefault(symbol, 0.0);
        double imbalance = (bidTotal + askTotal) == 0.0 ? Double.NaN : (bidTotal - askTotal) / (bidTotal + askTotal);
        obImbalance.put(symbol, imbalance);
        obImbalanceDelta.put(symbol, imbalance - prev);
    }

    public double getOrderBookImbalance(String symbol) {
        return obImbalance.getOrDefault(symbol, Double.NaN);
    }

    public double getOrderBookImbalanceDelta(String symbol) {
        return obImbalanceDelta.getOrDefault(symbol, 0.0);
    }
}