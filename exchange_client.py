"""
ccxt kitabxanasi ustunde nazik (thin) wrapper.
Testnet/sandbox destekleyen borsalar ucun avtomatik sandbox mode acir.
"""

import ccxt


class ExchangeClient:
    def __init__(self, exchange_id: str, api_key: str, api_secret: str, testnet: bool = True):
        self.testnet = testnet
        exchange_class = getattr(ccxt, exchange_id)

        self.exchange = exchange_class({
            "apiKey": api_key,
            "secret": api_secret,
            "enableRateLimit": True,
        })

        if testnet:
            # ccxt-in coxu borsa ucun sandbox/testnet mode-u var
            try:
                self.exchange.set_sandbox_mode(True)
            except Exception:
                print(
                    f"XEBERDARLIQ: {exchange_id} ucun ccxt sandbox mode "
                    "avtomatik aktivlesmedi - manual yoxlayin."
                )

    def fetch_ohlcv(self, symbol: str, timeframe: str, limit: int = 50):
        return self.exchange.fetch_ohlcv(symbol, timeframe=timeframe, limit=limit)

    def fetch_balance(self):
        return self.exchange.fetch_balance()

    def create_market_order(self, symbol: str, side: str, amount: float):
        """side: 'buy' ve ya 'sell'"""
        return self.exchange.create_order(
            symbol=symbol,
            type="market",
            side=side,
            amount=amount,
        )
