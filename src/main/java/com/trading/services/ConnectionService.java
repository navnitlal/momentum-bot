package com.trading.services;

import com.trading.ib.IBConnector;
import com.trading.settings.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionService {

    private static final Logger log = LoggerFactory.getLogger(ConnectionService.class);

    private final IBConnector ib;
    private final boolean liveMode;
    private final AppConfig.ConnectionConfig config;

    public ConnectionService(IBConnector ib, boolean liveMode, AppConfig.ConnectionConfig config) {
        this.ib = ib;
        this.liveMode = liveMode;
        this.config = config;
    }

    public void connect() {
        int port = liveMode ? config.livePort() : config.simPort();
        ib.connect(config.host(), port, config.clientId());
        log.info("Connected to IBKR {}", liveMode ? "LIVE" : "SIM");
    }

    public void disconnect() {
        ib.disconnect();
        log.info("Disconnected from IBKR.");
    }
}
