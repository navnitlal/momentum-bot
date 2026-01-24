package com.trading.settings;

public record Threshold(double minRelativeVolume, double minObImbalance, double minVolatility,
                        double minMACDDeviation, double rsiOverbought, double rsiOversold, boolean vwapTrendOK,
                        boolean bbTrendOK, boolean trendOK, boolean smaTrendOK) {
}