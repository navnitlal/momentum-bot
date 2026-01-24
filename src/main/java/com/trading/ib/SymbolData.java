package com.trading.ib;

import com.trading.strategy.StrategyType;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAdder;

public class SymbolData {

    private final ConcurrentLinkedDeque<Double> prices = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> volumes = new ConcurrentLinkedDeque<>();

    private final AtomicReference<StrategyType> strategyType = new AtomicReference<>();
    private final AtomicReference<Double> lastPrice = new AtomicReference<>(0.0);
    private final AtomicReference<Long> lastVolume = new AtomicReference<>(0L);

    private final AtomicInteger position = new AtomicInteger(0);
    private final AtomicReference<Double> entryPrice = new AtomicReference<>(0.0);
    private final DoubleAdder realizedPnl = new DoubleAdder();
    private final AtomicReference<Double> peakPrice = new AtomicReference<>(0.0);
    private final AtomicReference<Double> stopLossDelta = new AtomicReference<>(0.0);
    private final AtomicReference<Double> stopTrailingPriceDelta = new AtomicReference<>(0.0);

    private static final int HISTORY_LIMIT = 100;

    public void addPrice(double price) {
        prices.addLast(price);
        lastPrice.set(price);
        if (prices.size() > HISTORY_LIMIT) prices.pollFirst();
    }

    public void addVolume(long volume) {
        volumes.addLast(volume);
        lastVolume.set(volume);
        if (volumes.size() > HISTORY_LIMIT) volumes.pollFirst();
    }

    public double getLastPrice() { return lastPrice.get(); }
    public long getLastVolume() { return lastVolume.get(); }

    public Deque<Double> getPricesSnapshot() { return new ConcurrentLinkedDeque<>(prices); }
    public Deque<Long> getVolumesSnapshot() { return new ConcurrentLinkedDeque<>(volumes); }

    public void setPosition(int pos) { position.set(pos); }
    public int getPosition() { return position.get(); }

    public void setEntryPrice(double price) { entryPrice.set(price); }
    public double getEntryPrice() { return entryPrice.get(); }

    public void addRealizedPnl(double delta) { realizedPnl.add(delta); }
    public double getRealizedPnl() { return realizedPnl.sum(); }

    public void setPeakPrice(double price) { peakPrice.set(price); }
    public double getPeakPrice() { return peakPrice.get(); }

    public void setStopLossDelta(double price) { stopLossDelta.set(price); }
    public double getStopLossDelta() { return stopLossDelta.get(); }

    public void setStopTrailingPriceDelta(double price) { stopTrailingPriceDelta.set(price); }
    public double getStopTrailingPriceDelta() { return stopTrailingPriceDelta.get(); }

    public void setStrategyType(StrategyType st) { strategyType.set(st); }
    public StrategyType getStrategyType() { return strategyType.get(); }
}