package com.trading.ib;

import com.trading.strategy.StrategyType;

public interface ExecutionListener {
    void onExecutionConfirmed(String symbol, int filledQty, double fillPrice, StrategyType strategyType);
}
