def ema(values: list[float], alpha: float=0.4) -> float | None:
    if not values: return None
    v = values[0]
    for s in values[1:]:
        v = alpha*s + (1-alpha)*v
    return v