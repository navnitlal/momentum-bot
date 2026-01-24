package com.trading.strategy;

import com.trading.datafeed.Timeframe;
import com.trading.indicators.Indicator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.trading.datafeed.Timeframe.*;
import static com.trading.indicators.Indicator.*;

public enum StrategyType {

    MOMENTUM(mapOf(
            TICK, Set.of(ORDERBOOK),
            SEC5, Set.of(MACD, RELVOL, TREND, VOLATILITY),
            MIN1, Set.of(TREND)
    )),

    PULLBACK(mapOf(
            SEC10, Set.of(SMA, VWAP),
            SEC30, Set.of(SMA, TREND),
            MIN1, Set.of(TREND)
    )),

    RANGE(mapOf(
            SEC5, Set.of(TREND),
            SEC30, Set.of(SMA, VOLATILITY),
            MIN1, Set.of(SMA, VOLATILITY)
    )),

    NEWS(mapOf(
            TICK, Set.of(ORDERBOOK),
            SEC5, Set.of(RELVOL, VOLATILITY),
            SEC10, Set.of(RELVOL, VOLATILITY)
    ));

    private final Map<Timeframe, Set<Indicator>> timeframeIndicators;

    StrategyType(Map<Timeframe, Set<Indicator>> timeframeIndicators) {
        this.timeframeIndicators = timeframeIndicators;
    }

    public Map<Timeframe, Set<Indicator>> getTimeframeIndicators() {
        return timeframeIndicators;
    }

    public Set<Indicator> getIndicatorsFor(Timeframe timeframe) {
        return timeframeIndicators.getOrDefault(timeframe, Collections.emptySet());
    }

    private static Map<Timeframe, Set<Indicator>> mapOf(Object... entries) {
        Map<Timeframe, Set<Indicator>> map = new ConcurrentHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            Timeframe timeframe = (Timeframe) entries[i];
            Set<Indicator> indicators = (Set<Indicator>) entries[i + 1];
            map.put(timeframe, indicators);
        }
        return Collections.unmodifiableMap(map);
    }
}