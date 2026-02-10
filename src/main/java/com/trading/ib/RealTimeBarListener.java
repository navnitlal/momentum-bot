package com.trading.ib;

public interface RealTimeBarListener {
    void onRealTimeBar(String symbol, long startTime, double open, double high,
                       double low, double close, long volume);
}
