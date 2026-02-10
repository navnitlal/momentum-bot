package com.trading.ib;

import com.trading.strategy.StrategyType;

public interface OrderBookListener {
    void onOrderBookUpdate(String symbol, boolean isBid, double price, long size, int operation, StrategyType currentStrategy);
}
