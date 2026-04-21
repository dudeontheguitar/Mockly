from faster_whisper import WhisperModel
from pathlib import Path

print("[speech] Loading Whisper model...")

# Tiny или base — быстрее. Потом можно обновить на medium/large.
MODEL_NAME = "base"

model = WhisperModel(MODEL_NAME, device="cpu", compute_type="int8")

print("[speech] Whisper loaded successfully")


def transcribe_audio(audio_path: str) -> str:
    """
    Converts audio file to text using Whisper.
    """
    audio_path = Path(audio_path)

    if not audio_path.exists():
        raise FileNotFoundError(f"Audio file not found: {audio_path}")

    print(f"[speech] Transcribing: {audio_path.name}")

    segments, info = model.transcribe(str(audio_path))

    transcript = " ".join(segment.text.strip() for segment in segments)

    print(f"[speech] Transcript: {transcript}")
    return transcript
