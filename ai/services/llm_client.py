import json
import os
import requests

def _build_ollama_generate_url() -> str:
    """
    Supports both:
    - local dev: OLLAMA_URL is absent -> http://localhost:11434/api/generate
    - docker compose: OLLAMA_URL=http://ollama:11434 -> /api/generate is appended
    """
    raw_url = os.getenv("OLLAMA_URL", "http://localhost:11434").strip()

    if not raw_url:
        raw_url = "http://localhost:11434"

    if raw_url.endswith("/api/generate"):
        return raw_url

    if raw_url.endswith("/api"):
        return f"{raw_url}/generate"

    return f"{raw_url.rstrip('/')}/api/generate"


OLLAMA_URL = _build_ollama_generate_url()
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.1")

def ask_llm_ollama(transcript: str, score: float) -> dict:
    prompt = f"""
    You are an AI interview evaluator.

    Here is a candidate's interview answer:

    ---
    {transcript}
    ---
    The overall quality score of this answer is: {score} (0-100).

    Based on the answer and the score, generate:

    1) A short motivational English title (e.g. "Great Job", "Good Start", "Keep Improving")
    2) A one-line performance message (max 12 words)
    3) A bullet list (3-5 items) of candidate strengths
    4) A bullet list (3-5 items) of areas to improve
    5) One short actionable recommendation paragraph (max 2 sentences)

    Return ONLY valid JSON in EXACTLY this format, with double quotes and no extra text:

    {{
    "label": "",
    "message": "",
    "strengths": [],
    "areasToImprove": [],
    "recommendation": ""
    }}
    """

    payload = {
        "model": OLLAMA_MODEL,
        "prompt": prompt,
        "stream": False,
    }

    resp = requests.post(OLLAMA_URL, json=payload, timeout=120)
    resp.raise_for_status()
    data = resp.json()

    raw_text = data.get("response", "").strip()

    if raw_text.startswith("```"):
        raw_text = raw_text.strip("`")
        raw_text = raw_text.replace("json", "", 1).strip()

    try:
        parsed = json.loads(raw_text)
    except json.JSONDecodeError:
        print("[ask_llm_ollama] JSON parse error, raw:", raw_text)
        parsed = {
            "label": "AI Feedback",
            "message": "Here is your interview feedback.",
            "strengths": ["Answer contained relevant information."],
            "areasToImprove": ["Try to provide more structure and clear examples."],
            "recommendation": "Practice using the STAR method to structure your answers.",
        }

    return parsed
