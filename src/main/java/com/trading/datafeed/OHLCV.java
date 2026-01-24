package com.trading.datafeed;

public class OHLCV {
    public final long startTime;
    public double open;
    public double high;
    public double low;
    public double close;
    public long volume;
    public long closeTime;

    public OHLCV(double open, double high, double low, double close, long volume, long startTime) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.startTime = startTime;
        this.closeTime = 0;
    }

    public OHLCV copy() {
        OHLCV bar = new OHLCV(open, high, low, close, volume, startTime);
        bar.closeTime = this.closeTime;
        return bar;
    }

    public void updateFrom(OHLCV other) {
        this.high = Math.max(this.high, other.high);
        this.low = Math.min(this.low, other.low);
        this.close = other.close;
        this.volume += other.volume;
    }

    public void close(long intervalSeconds) {
        this.closeTime = startTime + intervalSeconds;
    }
}