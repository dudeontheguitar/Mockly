from pathlib import Path
import os
import tempfile
import requests
import wave
from typing import Optional
from urllib.parse import urlparse, unquote

try:
    import av
except Exception:  # pragma: no cover
    av = None


KNOWN_AUDIO_SUFFIXES = {
    ".wav",
    ".mp3",
    ".m4a",
    ".ogg",
    ".webm",
    ".mp4",
    ".aac",
    ".flac",
}


def _infer_temp_suffix(url: str) -> str:
    parsed = urlparse(url)
    suffix = Path(unquote(parsed.path)).suffix.lower()
    if suffix in KNOWN_AUDIO_SUFFIXES:
        return suffix
    return ".bin"

def download_audio(url: str) -> Path:
    response = requests.get(url, stream=True)
    response.raise_for_status()

    fd, tmp_path = tempfile.mkstemp(suffix=_infer_temp_suffix(url))
    os.close(fd)
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
    except wave.Error:
        # Expected for non-WAV files like MP3/M4A.
        pass
    except Exception as e:
        print(f"[get_audio_duration_seconds] WAV parser failed: {e}")

    if av is None:
        return None

    try:
        with av.open(str(path)) as container:
            if container.duration is not None and container.time_base is not None:
                return float(container.duration * container.time_base)

            for stream in container.streams:
                if stream.type != "audio":
                    continue
                if stream.duration is None or stream.time_base is None:
                    continue
                return float(stream.duration * stream.time_base)
    except Exception as e:
        print(f"[get_audio_duration_seconds] Container parser failed: {e}")

    return None
