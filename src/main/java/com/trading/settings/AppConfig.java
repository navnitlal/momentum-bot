package com.trading.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;

public record AppConfig(
        ConnectionConfig connection,
        ScannerConfig scanner,
        SchedulerConfig scheduler,
        TradingConfig trading
) {

    public record ConnectionConfig(
            String host,
            int livePort,
            int simPort,
            int clientId
    ) {}

    public record ScannerConfig(
            double minPrice,
            double maxPrice,
            String scanCode,
            int minVolume,
            int scanLimit,
            int timeoutMs
    ) {}

    public record SchedulerConfig(
            int scanIntervalMs,
            int scanStartDelayMs,
            int strategyIntervalMs,
            int strategyStartDelayMs,
            int printIntervalMs,
            int printStartDelayMs
    ) {}

    public record TradingConfig(
            String tradeLogPath,
            double maxAllocationFraction,
            int maxSharesPerStock,
            double riskPerTradeFraction
    ) {}

    public static AppConfig load() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        // Try external file first (for overrides)
        File externalFile = new File("application.yaml");
        if (externalFile.exists()) {
            return mapper.readValue(externalFile, AppConfig.class);
        }

        // Fall back to classpath resource
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("application.yaml")) {
            if (is == null) {
                throw new IllegalStateException("application.yaml not found on classpath or filesystem");
            }
            return mapper.readValue(is, AppConfig.class);
        }
    }
}
