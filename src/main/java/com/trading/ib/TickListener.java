package com.trading.ib;

import com.trading.strategy.StrategyType;

public interface TickListener {
    void onTick(String symbol, double price, long volume, long timestamp, StrategyType currentStrategy);
}
