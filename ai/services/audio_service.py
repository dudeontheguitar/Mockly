from pathlib import Path
import tempfile
import requests
import wave
from typing import Optional

def download_audio(url: str) -> Path:
    response = requests.get(url, stream=True)
    response.raise_for_status()

    fd, tmp_path = tempfile.mkstemp(suffix=".wav")
    path = Path(tmp_path)
    with path.open("wb") as f:
        for chunk in response.iter_content(8192):
            if chunk:
                f.write(chunk)

    print(f"[download_audio] Saved file to {path}")
    return path


def get_audio_duration_seconds(path: Path) -> Optional[float]:
    try:
        with wave.open(str(path), "rb") as f:
            frames = f.getnframes()
            rate = f.getframerate()
            return frames / float(rate)
    except Exception as e:
        print(f"[get_audio_duration_seconds] Failed: {e}")
        return None
