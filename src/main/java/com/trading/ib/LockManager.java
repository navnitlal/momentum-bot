package com.trading.ib;

import com.trading.datafeed.Timeframe;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private String makeKey(String symbol) {
        return symbol;
    }

    private String makeKey(String symbol, Timeframe tf) {
        return symbol + "-" + tf.name();
    }

    public void lock(String symbol) {
        ReentrantLock lock = locks.computeIfAbsent(makeKey(symbol), k -> new ReentrantLock());
        lock.lock();
    }

    public void unlock(String symbol) {
        ReentrantLock lock = locks.get(makeKey(symbol));
        if (lock != null) lock.unlock();
    }

    public boolean tryLock(String symbol) {
        ReentrantLock lock = locks.computeIfAbsent(makeKey(symbol), k -> new ReentrantLock());
        return lock.tryLock();
    }

    public void lock(String symbol, Timeframe tf) {
        ReentrantLock lock = locks.computeIfAbsent(makeKey(symbol, tf), k -> new ReentrantLock());
        lock.lock();
    }

    public void unlock(String symbol, Timeframe tf) {
        ReentrantLock lock = locks.get(makeKey(symbol, tf));
        if (lock != null) lock.unlock();
    }

    public boolean tryLock(String symbol, Timeframe tf) {
        ReentrantLock lock = locks.computeIfAbsent(makeKey(symbol, tf), k -> new ReentrantLock());
        return lock.tryLock();
    }
}