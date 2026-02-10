# Momentum Bot

An intraday trading bot for Interactive Brokers (IBKR) built in Java 17. It scans for stocks matching momentum criteria, applies time-of-day strategies with configurable technical indicators, and manages entries/exits with dynamic risk controls.

## Prerequisites

- **Java 17+**
- **Interactive Brokers TWS** or **IB Gateway** running locally
  - Paper trading (port 7497) or live (port 7496)
  - API connections enabled in TWS: *Edit > Global Configuration > API > Settings*

## Quick Start

```bash
# Build the fat jar
./gradlew shadowJar

# Run in paper trading (SIM) mode
java -jar build/libs/momentum-bot-all.jar SIM

# Run in live mode
java -jar build/libs/momentum-bot-all.jar LIVE
```

The bot will:
1. Connect to TWS on localhost
2. Scan for top percent gainers matching volume/price filters
3. Subscribe to real-time market data for matched symbols
4. Evaluate buy/sell signals on each scheduler tick
5. Display a live ANSI dashboard in the terminal

Stop with `Ctrl+C` for a graceful shutdown.

## How It Works

### Scanner
The IBKR scanner runs on a configurable interval (default 5s), looking for stocks by scan code (default `TOP_PERC_GAIN`) within a price and volume range. New symbols are subscribed; dropped symbols are force-sold and unsubscribed.

### Strategies
The active strategy rotates by time of day:

| Time (ET)       | Strategy     |
|-----------------|-------------|
| 04:30 - 11:00   | MOMENTUM    |
| 11:00 - 13:30   | PULLBACK    |
| 13:30 - 15:30   | RANGE       |
| 15:30 - 17:00   | MOMENTUM    |
| Other           | NEWS        |

Each strategy defines which indicators to evaluate and on which timeframes (5s, 10s, 30s, 1min, tick-level order book).

### Indicators
- **MACD** - Moving Average Convergence Divergence
- **SMA** - Simple Moving Average trend direction
- **Bollinger Bands** - Price relative to upper/lower bands
- **VWAP** - Volume Weighted Average Price
- **RSI** - Relative Strength Index
- **Relative Volume** - Current vs. historical volume ratio
- **Trend** - Price trend with pullback detection
- **Volatility** - Price volatility measurement
- **Order Book** - Bid/ask imbalance from Level 2 data

### Risk Management
- Dynamic stop-loss and trailing stop per strategy type, scaled by price tier
- Position sizing based on account balance, max allocation fraction, and per-trade risk
- Configurable max shares per stock cap

## Configuration

### `application.yaml`
Controls connection, scanner, scheduler, and trading parameters. Defaults ship inside the JAR. To override, place an `application.yaml` in the working directory.

```yaml
connection:
  host: "127.0.0.1"
  livePort: 7496
  simPort: 7497
  clientId: 0

scanner:
  minPrice: 2.0
  maxPrice: 20.0
  scanCode: "TOP_PERC_GAIN"
  minVolume: 100000
  scanLimit: 5
  timeoutMs: 2000

scheduler:
  scanIntervalMs: 5000
  strategyIntervalMs: 1000
  printIntervalMs: 1000

trading:
  tradeLogPath: "data/trades.csv"
  maxAllocationFraction: 0.7
  maxSharesPerStock: 1000
  riskPerTradeFraction: 0.01
```

### `trading_config.yaml`
Defines per-strategy, per-timeframe indicator thresholds and calculation parameters (MACD periods, RSI period, Bollinger settings, etc.). Defaults ship inside the JAR. To override, place a `trading_config.yaml` in the working directory.

## Project Structure

```
src/main/java/com/trading/
  bot/            BotLauncher, ScalperBot (entry point & wiring)
  ib/             IBConnector, listener interfaces, SymbolData
  datafeed/       BarManager, OHLCV, Timeframe
  indicators/     IndicatorManager + individual indicator implementations
  signals/        SignalManager, SignalValidator
  strategy/       StrategyManager, StrategyType
  orders/         TradeExecutor, ExecutionHandler, DynamicRiskManager
  scanner/        IBKRScanner
  services/       ConnectionService, SchedulerService, ScannerService, etc.
  settings/       AppConfig, ConfigThreshold
  console/        Dashboard (ANSI terminal UI)

src/main/resources/
  application.yaml       Default app config
  trading_config.yaml    Default strategy thresholds
  logback.xml            Logging configuration
```

## Logging

Logs are written to both the console and `logs/momentum-bot.log` with 30-day rolling retention. The `com.trading` package logs at DEBUG level. Adjust `logback.xml` or place a custom one in the working directory to change levels.

## Build

```bash
# Full build
./gradlew build

# Fat jar only
./gradlew shadowJar

# Output
build/libs/momentum-bot-all.jar
```

Requires the IB TWS API jar in `libs/TwsApi.jar`.
