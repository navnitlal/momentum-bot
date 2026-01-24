package com.trading.services;

import com.trading.ib.IBConnector;

public class ConnectionService {
    private final IBConnector ib;
    private final boolean liveMode;

    public ConnectionService(IBConnector ib, boolean liveMode) {
        this.ib = ib;
        this.liveMode = liveMode;
    }

    public void connect() {
        int port = liveMode ? 7496 : 7497;
        ib.connect("127.0.0.1", port, 0);
        System.out.println("Connected to IBKR " + (liveMode ? "LIVE" : "SIM"));
    }

    public void disconnect() {
        ib.disconnect();
        System.out.println("Disconnected from IBKR.");
    }
}