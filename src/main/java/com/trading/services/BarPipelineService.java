package com.trading.services;

import com.trading.datafeed.BarManager;
import com.trading.datafeed.Timeframe;
import com.trading.indicators.Indicator;
import com.trading.indicators.IndicatorManager;
import com.trading.strategy.StrategyType;
import java.util.Set;

public class BarPipelineService {

    private final BarManager barManager;
    private final IndicatorManager indicatorManager;

    public BarPipelineService(BarManager barManager, IndicatorManager indicatorManager) {
        this.barManager = barManager;
        this.indicatorManager = indicatorManager;
    }

    public void registerSymbol(String symbol, StrategyType strategy) {
        for (var entry : strategy.getTimeframeIndicators().entrySet()) {
            Timeframe tf = entry.getKey();
            if (tf == Timeframe.TICK) continue;

            Set<Indicator> indicators = entry.getValue();
            barManager.addBarCloseListener(tf, (s, bar) -> {
                if (!s.equals(symbol)) return;
                indicatorManager.updateOnBarClose(symbol, bar.copy(), tf, indicators, strategy);
            });
        }
    }

    public void clearSymbol(String symbol) {
        barManager.clearOHLCVData(symbol);
    }
}