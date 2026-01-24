package com.trading.bot;

public class BotLauncher {

    public static void main(String[] args) throws Exception {
        boolean liveMode;
        if (args.length == 0) {
            System.err.println("Please specify mode: LIVE or SIM");
            return;
        }
        switch (args[0].toUpperCase()) {
            case "LIVE" -> liveMode = true;
            case "SIM" -> liveMode = false;
            default -> {
                System.err.println("Invalid mode. Use LIVE or SIM.");
                return;
            }
        }

        ScalperBot bot = new ScalperBot(liveMode);
        bot.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received. Stopping ScalperBot...");
            bot.stop();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}