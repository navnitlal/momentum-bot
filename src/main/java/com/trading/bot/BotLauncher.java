package com.trading.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotLauncher {

    private static final Logger log = LoggerFactory.getLogger(BotLauncher.class);

    public static void main(String[] args) throws Exception {
        boolean liveMode;
        if (args.length == 0) {
            log.error("Please specify mode: LIVE or SIM");
            return;
        }
        switch (args[0].toUpperCase()) {
            case "LIVE" -> liveMode = true;
            case "SIM" -> liveMode = false;
            default -> {
                log.error("Invalid mode. Use LIVE or SIM.");
                return;
            }
        }

        ScalperBot bot = new ScalperBot(liveMode);
        bot.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received. Stopping ScalperBot...");
            bot.stop();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
