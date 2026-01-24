package com.trading.settings;

public record Config(double pullbackPercent, int bbPeriod, double bbMultiplier,
                     int macdFast, int macdSlow, int macdSignal,
                     int smaPeriod, int rsiPeriod) {
}