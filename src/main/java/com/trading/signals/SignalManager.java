package com.trading.signals;

import com.trading.datafeed.Timeframe;
import com.trading.indicators.Indicator;
import com.trading.indicators.IndicatorManager;
import com.trading.settings.ConfigThreshold;
import com.trading.strategy.StrategyType;

import java.util.Map;
import java.util.Set;

public class SignalManager {

    private final IndicatorManager indicatorManager;
    private final ConfigThreshold threshold;

    public SignalManager(IndicatorManager indicatorManager, ConfigThreshold threshold) {
        this.threshold = threshold;
        this.indicatorManager = indicatorManager;
    }

    public boolean isBuySignal(String symbol, StrategyType strategy) {
        if (symbol == null || strategy == null || indicatorManager.AnyFirstBarIncomplete(symbol, strategy))
            return false;
        SignalValidator validator = new SignalValidator(threshold);

        indicatorManager.getLockManager().lock(symbol);
        Map<Timeframe, Set<Indicator>> tfInds = strategy.getTimeframeIndicators();
        try {

            tfInds.forEach((timeframe, indicators) ->
                    indicators.forEach(indicator -> {
                        Object value = fetchBuyIndicatorValue(symbol, indicator, timeframe);
                        validator.setActualValue(indicator, timeframe, value);
                    })
            );
        } finally {
            indicatorManager.getLockManager().unlock(symbol);
        }

        return validator.meetsBuyCondition(symbol, tfInds, strategy);
    }

    public boolean isSellSignal(String symbol, StrategyType strategy) {
        if (symbol == null || strategy == null || indicatorManager.AnyFirstBarIncomplete(symbol, strategy)) return false;

        SignalValidator validator = new SignalValidator(threshold);

        indicatorManager.getLockManager().lock(symbol);
        Map<Timeframe, Set<Indicator>> tfInds = strategy.getTimeframeIndicators();
        try {
            tfInds.forEach((timeframe, indicators) ->
                    indicators.forEach(indicator -> {
                        Object value = fetchSellIndicatorValue(symbol, indicator, timeframe);
                        validator.setActualValue(indicator, timeframe, value);
                    })
            );
        } finally {
            indicatorManager.getLockManager().unlock(symbol);
        }
        return validator.meetsSellCondition(symbol, tfInds, strategy);
    }

    private Object fetchBuyIndicatorValue(String symbol, Indicator indicator, Timeframe tf) {

        return switch (indicator) {
            case MACD -> indicatorManager.getMACDHistogram(symbol, tf);
            case RELVOL -> indicatorManager.getRelativeVolume(symbol, tf);
            case VWAP -> indicatorManager.isPriceAboveVWAP(symbol, tf);
            case TREND -> indicatorManager.isUptrend(symbol, tf);
            case VOLATILITY -> indicatorManager.getVolatility(symbol, tf);
            case ORDERBOOK -> indicatorManager.getOrderBookImbalance(symbol);
            case RSI -> indicatorManager.getRSI(symbol, tf);
            case BOLLINGER -> indicatorManager.isBBAboveUpper(symbol, tf);
            case SMA -> indicatorManager.isSMAUpTrend(symbol, tf);
        };
    }

    private Object fetchSellIndicatorValue(String symbol, Indicator indicator, Timeframe tf) {
        return switch (indicator) {
            case MACD -> indicatorManager.getMACDHistogram(symbol, tf);
            case RELVOL -> indicatorManager.getRelativeVolume(symbol, tf);
            case VWAP -> indicatorManager.isPriceBelowVWAP(symbol, tf);
            case TREND -> indicatorManager.isDowntrend(symbol, tf);
            case VOLATILITY -> indicatorManager.getVolatility(symbol, tf);
            case ORDERBOOK -> indicatorManager.getOrderBookImbalance(symbol);
            case RSI -> indicatorManager.getRSI(symbol, tf);
            case BOLLINGER -> indicatorManager.isBBBelowLower(symbol, tf);
            case SMA -> indicatorManager.isSMADownTrend(symbol, tf);
        };
    }
}