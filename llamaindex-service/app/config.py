from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


@dataclass
class ServiceSettings:
    service_api_key: str = ""
    data_file: Path = Path("data/knowledge-base.json")
    max_top_k: int = 20

    @classmethod
    def from_env(cls) -> "ServiceSettings":
        root_dir = Path(__file__).resolve().parents[1]
        raw_data_file = os.getenv("LLAMAINDEX_DATA_FILE", "data/knowledge-base.json").strip()
        data_file = Path(raw_data_file)
        if not data_file.is_absolute():
            data_file = root_dir / data_file

        raw_max_top_k = os.getenv("LLAMAINDEX_MAX_TOP_K", "20").strip()
        try:
            max_top_k = int(raw_max_top_k)
        except ValueError:
            max_top_k = 20

        return cls(
            service_api_key=os.getenv("LLAMAINDEX_SERVICE_API_KEY", "").strip(),
            data_file=data_file,
            max_top_k=max(1, max_top_k),
        )

    @property
    def auth_enabled(self) -> bool:
        return bool(self.service_api_key)
