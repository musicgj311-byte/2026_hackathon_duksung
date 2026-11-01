"""외부 API 결과를 잠시 저장해 두는 메모리 캐시.

같은 링크를 여러 번 조회하지 않게 해서 VirusTotal 무료 한도(분당 4회)를 아낍니다.
서버를 다시 켜면 비워집니다.
"""

import time
from typing import Any

_MISSING = object()


class TTLCache:
    def __init__(self, ttl_seconds: float = 3600, max_items: int = 5000) -> None:
        self.ttl = ttl_seconds
        self.max_items = max_items
        self._data: dict[str, tuple[float, Any]] = {}

    def get(self, key: str) -> Any:
        item = self._data.get(key)
        if item is None:
            return _MISSING
        expires, value = item
        if expires < time.monotonic():
            del self._data[key]
            return _MISSING
        return value

    def set(self, key: str, value: Any) -> None:
        if len(self._data) >= self.max_items:
            # 가장 오래 전에 넣은 항목부터 버림
            self._data.pop(next(iter(self._data)))
        self._data[key] = (time.monotonic() + self.ttl, value)

    def clear(self) -> None:
        self._data.clear()


def is_missing(value: Any) -> bool:
    return value is _MISSING


url_cache = TTLCache()
