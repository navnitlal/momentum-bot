package com.trading.settings;

import com.trading.datafeed.Timeframe;
import com.trading.strategy.StrategyType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

public class ConfigThreshold {

    private static final String DEFAULT_CONFIG_PATH = "trading_config.yaml";

    private final Map<StrategyType, Map<Timeframe, Threshold>> thresholds = new EnumMap<>(StrategyType.class);
    private final Map<StrategyType, Map<Timeframe, Config>> configs = new EnumMap<>(StrategyType.class);

    public ConfigThreshold() throws Exception {
        this(DEFAULT_CONFIG_PATH);
    }

    public ConfigThreshold(String configPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        Map<String, Map<String, Map<String, Object>>> data;

        // Try filesystem first (for overrides), then classpath
        File externalFile = new File(configPath);
        if (externalFile.exists()) {
            data = mapper.readValue(externalFile, Map.class);
        } else {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(configPath)) {
                if (is == null) {
                    throw new IllegalStateException(configPath + " not found on classpath or filesystem");
                }
                data = mapper.readValue(is, Map.class);
            }
        }

        for (var strategyEntry : data.entrySet()) {
            StrategyType strategy = StrategyType.valueOf(strategyEntry.getKey());
            thresholds.put(strategy, new EnumMap<>(Timeframe.class));
            configs.put(strategy, new EnumMap<>(Timeframe.class));

            for (var tfEntry : strategyEntry.getValue().entrySet()) {
                Timeframe tf = Timeframe.valueOf(tfEntry.getKey());
                Map<String, Object> tfValues = tfEntry.getValue();

                Map<String, Object> thresholdMap = (Map<String, Object>) tfValues.get("threshold");
                Map<String, Object> configMap = (Map<String, Object>) tfValues.get("config");

                Threshold threshold = new Threshold(
                        ((Number) thresholdMap.getOrDefault("minRelativeVolume", 0.0)).doubleValue(),
                        ((Number) thresholdMap.getOrDefault("minObImbalance", 0.0)).doubleValue(),
                        ((Number) thresholdMap.getOrDefault("minVolatility", 0.0)).doubleValue(),
                        ((Number) thresholdMap.getOrDefault("minMACDDeviation", 0.0)).doubleValue(),
                        ((Number) thresholdMap.getOrDefault("rsiOverbought", 0)).intValue(),
                        ((Number) thresholdMap.getOrDefault("rsiOversold", 0)).intValue(),
                        (Boolean) thresholdMap.getOrDefault("vwapTrendOK", true),
                        (Boolean) thresholdMap.getOrDefault("bbTrendOK", true),
                        (Boolean) thresholdMap.getOrDefault("trendOK", true),
                        (Boolean) thresholdMap.getOrDefault("smaTrendOK", true)
                );
                thresholds.get(strategy).put(tf, threshold);

                if (configMap.isEmpty()) continue;

                Config config = new Config(
                        ((Number) configMap.getOrDefault("pullbackPercent", 0.0)).doubleValue(),
                        ((Number) configMap.getOrDefault("bbPeriod", 0)).intValue(),
                        ((Number) configMap.getOrDefault("bbMultiplier", 0.0)).doubleValue(),
                        ((Number) configMap.getOrDefault("macdFast", 0)).intValue(),
                        ((Number) configMap.getOrDefault("macdSlow", 0)).intValue(),
                        ((Number) configMap.getOrDefault("macdSignal", 0)).intValue(),
                        ((Number) configMap.getOrDefault("smaPeriod", 0)).intValue(),
                        ((Number) configMap.getOrDefault("rsiPeriod", 0)).intValue()
                );
                configs.get(strategy).put(tf, config);
            }
        }
    }

    public Threshold getThreshold(StrategyType strategy, Timeframe timeframe) {
        return thresholds.getOrDefault(strategy, Map.of()).get(timeframe);
    }

    public Config getConfig(StrategyType strategy, Timeframe timeframe) {
        return configs.getOrDefault(strategy, Map.of()).get(timeframe);
    }
}
