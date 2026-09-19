"""
Basit crypto trading bot prototipi.
- ccxt kitabxanasi ile istenilen borsaya (default: Binance) qosulur
- SMA (Simple Moving Average) crossover strategiyasi ile siqnal verir
- DEFAULT olaraq DRY_RUN=True -> real emeliyyat etmir, sadece log yazir
- Real pul ile islemek ucun .env-de DRY_RUN=False edib API key/secret qoymaq lazimdir

XEBERDARLIQ: Bu, tehsil/prototip meqsedli koddur. Real hesabla ISTIFADE ETMEZDEN
EVVEL mutleq exchange-in TESTNET/SANDBOX mühitinde sinaqdan kecirin.
Avtomatik trading pul itkisi riski dasiyir - Claude maliyye meslehetcisi deyil.
"""

import os
import time
import logging
from dotenv import load_dotenv

from exchange_client import ExchangeClient
from strategy import sma_crossover_signal

load_dotenv()

SYMBOL = os.getenv("SYMBOL", "BTC/USDT")
TIMEFRAME = os.getenv("TIMEFRAME", "1h")
TRADE_AMOUNT = float(os.getenv("TRADE_AMOUNT", "0.001"))
FAST_MA = int(os.getenv("FAST_MA", "9"))
SLOW_MA = int(os.getenv("SLOW_MA", "21"))
POLL_SECONDS = int(os.getenv("POLL_SECONDS", "60"))
DRY_RUN = os.getenv("DRY_RUN", "True").lower() == "true"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
log = logging.getLogger("tradebot")


def main():
    client = ExchangeClient(
        exchange_id=os.getenv("EXCHANGE", "binance"),
        api_key=os.getenv("API_KEY", ""),
        api_secret=os.getenv("API_SECRET", ""),
        testnet=os.getenv("USE_TESTNET", "True").lower() == "true",
    )

    log.info(
        f"Bot basladi | symbol={SYMBOL} timeframe={TIMEFRAME} "
        f"dry_run={DRY_RUN} testnet={client.testnet}"
    )

    position_open = False

    while True:
        try:
            candles = client.fetch_ohlcv(SYMBOL, TIMEFRAME, limit=SLOW_MA + 5)
            closes = [c[4] for c in candles]

            signal = sma_crossover_signal(closes, FAST_MA, SLOW_MA)
            price = closes[-1]
            log.info(f"Qiymet={price:.2f} | Siqnal={signal}")

            if signal == "BUY" and not position_open:
                log.info(f"AL siqnali -> {TRADE_AMOUNT} {SYMBOL}")
                if not DRY_RUN:
                    client.create_market_order(SYMBOL, "buy", TRADE_AMOUNT)
                position_open = True

            elif signal == "SELL" and position_open:
                log.info(f"SAT siqnali -> {TRADE_AMOUNT} {SYMBOL}")
                if not DRY_RUN:
                    client.create_market_order(SYMBOL, "sell", TRADE_AMOUNT)
                position_open = False

            else:
                log.info("Emeliyyat yoxdur, gozleyirik.")

        except Exception as e:
            log.error(f"Xeta bas verdi: {e}")

        time.sleep(POLL_SECONDS)


if __name__ == "__main__":
    main()
