# app/core/events.py
import asyncio
from collections import defaultdict
from typing import Dict, Set

class Broker:
    def __init__(self):
        self.channels: Dict[str, Set[asyncio.Queue]] = defaultdict(set)
        self.lock = asyncio.Lock()

    async def subscribe(self, key: str) -> asyncio.Queue:
        q = asyncio.Queue(maxsize=100)
        async with self.lock:
            self.channels[key].add(q)
        return q

    async def unsubscribe(self, key: str, q: asyncio.Queue):
        async with self.lock:
            if key in self.channels:
                self.channels[key].discard(q)
                if not self.channels[key]:
                    del self.channels[key]

    async def publish(self, key: str, message: dict):
        async with self.lock:
            targets = list(self.channels.get(key, []))
        for q in targets:
            try:
                q.put_nowait(message)
            except asyncio.QueueFull:
                try: q.get_nowait()
                except Exception: pass
                try: q.put_nowait(message)
                except Exception: pass

broker = Broker()