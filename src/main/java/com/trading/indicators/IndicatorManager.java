package com.trading.indicators;

import com.trading.datafeed.Timeframe;
import com.trading.datafeed.OHLCV;
import com.trading.ib.IBConnector;
import com.trading.ib.LockManager;
import com.trading.ib.OrderBookListener;
import com.trading.indicators.averages.*;
import com.trading.indicators.momentum.*;
import com.trading.indicators.orderbook.OrderBookIndicator;
import com.trading.indicators.priceanalysis.*;
import com.trading.settings.ConfigThreshold;
import com.trading.strategy.StrategyType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IndicatorManager implements OrderBookListener {

    private final ConfigThreshold config;
    private final LockManager lockManager;

    private final TrendIndicator trend;
    private final VolatilityIndicator volatility;
    private final MACDIndicator macd;
    private final SMAIndicator sma;
    private final BollingerBandsIndicator bollingerBand;
    private final RSIIndicator rsi;
    private final OrderBookIndicator orderBook;
    private final VWAPIndicator vwap;
    private final RelativeVolumeIndicator relVolume;

    private final Map<String, Set<Timeframe>> firstBarsCompleted = new ConcurrentHashMap<>();

    public IndicatorManager(ConfigThreshold config, IBConnector ib, LockManager lockManager) {
        this.config = config;
        this.lockManager = lockManager;
        this.trend = new TrendIndicator(lockManager);
        this.volatility = new VolatilityIndicator(lockManager);
        this.macd = new MACDIndicator(lockManager);
        this.sma = new SMAIndicator(lockManager);
        this.bollingerBand = new BollingerBandsIndicator(lockManager);
        this.rsi = new RSIIndicator(lockManager);
        this.orderBook = new OrderBookIndicator(lockManager);
        this.vwap = new VWAPIndicator(lockManager);
        this.relVolume = new RelativeVolumeIndicator(lockManager);

        ib.addOrderBookListener(this);
    }

    public void updateOnBarClose(String symbol, OHLCV bar, Timeframe tf, Set<Indicator> indicatorsToUpdate, StrategyType strategyType) {
        if (bar == null || indicatorsToUpdate == null || strategyType == null || tf == Timeframe.TICK) return;

        var cg = config.getConfig(strategyType, tf);

        if (indicatorsToUpdate.contains(Indicator.TREND)) trend.update(symbol, bar.close, tf, cg.pullbackPercent());
        if (indicatorsToUpdate.contains(Indicator.VOLATILITY)) volatility.update(symbol, bar.close, tf);
        if (indicatorsToUpdate.contains(Indicator.MACD)) macd.update(symbol, bar.close, tf, cg.macdFast(), cg.macdSlow(), cg.macdSignal());
        if (indicatorsToUpdate.contains(Indicator.SMA)) sma.update(symbol, bar.close, tf, cg.smaPeriod());
        if (indicatorsToUpdate.contains(Indicator.BOLLINGER)) bollingerBand.update(symbol, bar.close, tf, cg.bbPeriod(), cg.bbMultiplier());
        if (indicatorsToUpdate.contains(Indicator.RSI)) rsi.update(symbol, bar.close, bar.close, tf, cg.rsiPeriod());
        if (indicatorsToUpdate.contains(Indicator.RELVOL)) relVolume.update(symbol, bar.volume, tf);
        if (indicatorsToUpdate.contains(Indicator.VWAP)) vwap.update(symbol, bar, tf);

        firstBarsCompleted.computeIfAbsent(symbol, k -> ConcurrentHashMap.newKeySet()).add(tf);
    }

    public boolean AnyFirstBarIncomplete(String symbol, StrategyType strategy) {
        Set<Timeframe> completed = firstBarsCompleted.getOrDefault(symbol, Set.of());
        return !strategy.getTimeframeIndicators().keySet().stream()
                .filter(tf -> tf != Timeframe.TICK)
                .allMatch(completed::contains);
    }

    public LockManager getLockManager(){
        return lockManager;
    }

    @Override
    public void onOrderBookUpdate(String symbol, boolean isBid, double price, long size, int operation, StrategyType strategy) {
        if (strategy == null || symbol == null) return;
        if (strategy.getIndicatorsFor(Timeframe.TICK).contains(Indicator.ORDERBOOK))
            orderBook.updateOrderBook(symbol, isBid, price, size, operation);
    }

    public double getVolatility(String symbol, Timeframe tf) { return volatility.getVolatility(symbol, tf); }
    public double getMACDHistogram(String symbol, Timeframe tf) { return macd.getHistogram(symbol, tf); }
    public double getRSI(String symbol, Timeframe tf) { return rsi.getRSI(symbol, tf); }
    public double getRelativeVolume(String symbol, Timeframe tf) { return relVolume.getRelativeVolume(symbol, tf); }
    public double getOrderBookImbalance(String symbol) { return orderBook.getOrderBookImbalance(symbol); }

    public boolean isSMAUpTrend(String symbol, Timeframe tf) { return sma.isUpwardTrend(symbol, tf); }
    public boolean isSMADownTrend(String symbol, Timeframe tf) { return sma.isDownwardTrend(symbol, tf); }
    public boolean isBBAboveUpper(String symbol, Timeframe tf) { return bollingerBand.isAboveUpper(symbol, tf); }
    public boolean isBBBelowLower(String symbol, Timeframe tf) { return bollingerBand.isBelowLower(symbol, tf); }
    public boolean isPriceAboveVWAP(String symbol, Timeframe tf) { return vwap.isAboveVWAP(symbol, tf); }
    public boolean isPriceBelowVWAP(String symbol, Timeframe tf) { return vwap.isBelowVWAP(symbol, tf); }
    public boolean isUptrend(String symbol, Timeframe tf) { return trend.isUpwardTrend(symbol, tf); }
    public boolean isDowntrend(String symbol, Timeframe tf) { return trend.isDownwardTrend(symbol, tf); }
}