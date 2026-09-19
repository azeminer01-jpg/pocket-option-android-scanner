"""
Cox sade SMA (Simple Moving Average) crossover strategiyasi.
- Fast MA, Slow MA-nin ustune kecdikde -> BUY
- Fast MA, Slow MA-nin altina dusdukde -> SELL
- Aksi halda -> HOLD

Bu, tanish/basic bir strategiyadir - real pul ile istifade etmezden
evvel backtest etmeden guvenmeyin.
"""

from typing import List


def sma(values: List[float], period: int) -> float:
    if len(values) < period:
        raise ValueError("Kifayet qeder data yoxdur SMA hesablamaq ucun")
    return sum(values[-period:]) / period


def sma_crossover_signal(closes: List[float], fast_period: int, slow_period: int) -> str:
    if len(closes) < slow_period + 1:
        return "HOLD"

    fast_now = sma(closes, fast_period)
    slow_now = sma(closes, slow_period)

    fast_prev = sma(closes[:-1], fast_period)
    slow_prev = sma(closes[:-1], slow_period)

    crossed_up = fast_prev <= slow_prev and fast_now > slow_now
    crossed_down = fast_prev >= slow_prev and fast_now < slow_now

    if crossed_up:
        return "BUY"
    elif crossed_down:
        return "SELL"
    else:
        return "HOLD"
