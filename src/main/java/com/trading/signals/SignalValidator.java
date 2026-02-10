package com.trading.signals;

import com.trading.datafeed.Timeframe;
import com.trading.indicators.Indicator;
import com.trading.settings.ConfigThreshold;
import com.trading.settings.Threshold;
import com.trading.strategy.StrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SignalValidator {

    private static final Logger log = LoggerFactory.getLogger(SignalValidator.class);

    private final ConfigThreshold configThreshold;
    private final Map<Indicator, Map<Timeframe, Object>> actualValues = new ConcurrentHashMap<>();

    public SignalValidator(ConfigThreshold configThreshold) {
        this.configThreshold = configThreshold;
    }

    public void setActualValue(Indicator indicator, Timeframe timeframe, Object value) {
        actualValues.computeIfAbsent(indicator, k -> new ConcurrentHashMap<>())
                .put(timeframe, value);
    }

    public boolean meetsBuyCondition(String symbol, Map<Timeframe, Set<Indicator>> tfInds, StrategyType strategyType) {
        return checkCondition(symbol, tfInds, strategyType, true);
    }

    public boolean meetsSellCondition(String symbol, Map<Timeframe, Set<Indicator>> tfInds, StrategyType strategyType) {
        return checkCondition(symbol, tfInds, strategyType, false);
    }

    private boolean checkCondition(String symbol, Map<Timeframe, Set<Indicator>> tfInds, StrategyType strategyType, boolean isBuy) {
        for (Map.Entry<Timeframe, Set<Indicator>> entry : tfInds.entrySet()) {
            Timeframe timeframe = entry.getKey();
            Set<Indicator> indicators = entry.getValue();

            for (Indicator indicator : indicators) {
                Object actual = actualValues.get(indicator).get(timeframe);

                Threshold threshold = configThreshold.getThreshold(strategyType, timeframe);

                boolean conditionMet = isBuy ? meetsBuyThreshold(symbol, indicator, timeframe, actual, threshold)
                        : meetsSellThreshold(symbol, indicator, timeframe, actual, threshold);

                if (!conditionMet) return false;
            }
        }
        return true;
    }

    private boolean meetsBuyThreshold(String symbol, Indicator indicator, Timeframe tf, Object actualValue, Threshold threshold) {
        boolean result = switch (indicator) {
            case RELVOL -> (double) actualValue >= threshold.minRelativeVolume();
            case ORDERBOOK -> (double) actualValue >= threshold.minObImbalance();
            case RSI -> (double) actualValue <= threshold.rsiOversold();
            case MACD -> (double) actualValue >= threshold.minMACDDeviation();
            case VOLATILITY -> (double) actualValue >= threshold.minVolatility();
            case BOLLINGER -> (boolean) actualValue == threshold.bbTrendOK();
            case SMA -> (boolean) actualValue == threshold.smaTrendOK();
            case VWAP -> (boolean) actualValue == threshold.vwapTrendOK();
            case TREND -> (boolean) actualValue == threshold.trendOK();
        };

        log.trace("[BUY] SYMBOL: {} | Indicator: {} | TF: {} | Actual: {} | Threshold: {} | Result: {}",
                symbol, indicator, tf, actualValue, formatThreshold(indicator, threshold), result);

        return result;
    }

    private boolean meetsSellThreshold(String symbol, Indicator indicator, Timeframe tf, Object actualValue, Threshold threshold) {
        boolean result = switch (indicator) {
            case RELVOL -> (double) actualValue < threshold.minRelativeVolume();
            case ORDERBOOK -> (double) actualValue < threshold.minObImbalance();
            case RSI -> (double) actualValue >= threshold.rsiOversold();
            case MACD -> (double) actualValue <= -threshold.minMACDDeviation();
            case VOLATILITY -> (double) actualValue < threshold.minVolatility();
            case BOLLINGER -> (boolean) actualValue == !threshold.bbTrendOK();
            case SMA -> (boolean) actualValue == !threshold.smaTrendOK();
            case VWAP -> (boolean) actualValue == !threshold.vwapTrendOK();
            case TREND -> (boolean) actualValue == !threshold.trendOK();
        };

        log.trace("[SELL] SYMBOL: {} | Indicator: {} | TF: {} | Actual: {} | Threshold: {} | Result: {}",
                symbol, indicator, tf, actualValue, formatThreshold(indicator, threshold), result);

        return result;
    }

    private String formatThreshold(Indicator indicator, Threshold threshold) {
        return switch (indicator) {
            case RELVOL -> "minRelVol=" + threshold.minRelativeVolume();
            case ORDERBOOK -> "minOB=" + threshold.minObImbalance();
            case RSI -> "rsiOversold=" + threshold.rsiOversold();
            case MACD -> "minMACD=" + threshold.minMACDDeviation();
            case VOLATILITY -> "minVolatility=" + threshold.minVolatility();
            case BOLLINGER -> "bbTrendOK=" + threshold.bbTrendOK();
            case SMA -> "smaTrendOK=" + threshold.smaTrendOK();
            case VWAP -> "vwapTrendOK=" + threshold.vwapTrendOK();
            case TREND -> "trendOK=" + threshold.trendOK();
        };
    }
}
