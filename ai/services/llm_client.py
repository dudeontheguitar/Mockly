import json
import os
import requests


def _env_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


def _fallback_feedback(score: float) -> dict:
    if score >= 80:
        return {
            "label": "Great Job",
            "message": "Strong answer with clear structure and confidence.",
            "strengths": [
                "Relevant and focused response.",
                "Good clarity and confidence.",
                "Solid communication flow.",
            ],
            "areasToImprove": [
                "Add more measurable outcomes.",
                "Include deeper technical details where needed.",
            ],
            "recommendation": "Keep your structure and add one concrete impact metric in each answer.",
        }
    if score >= 60:
        return {
            "label": "Good Start",
            "message": "Decent answer, but depth can be improved.",
            "strengths": [
                "Clear main idea.",
                "Reasonably structured response.",
            ],
            "areasToImprove": [
                "Use more specific examples.",
                "Reduce filler words and repetition.",
                "Explain reasoning more explicitly.",
            ],
            "recommendation": "Practice STAR-style answers with one concrete example and result.",
        }

    return {
        "label": "Keep Improving",
        "message": "Answer needs more structure and detail.",
        "strengths": [
            "You attempted to address the question.",
        ],
        "areasToImprove": [
            "Improve structure and logical flow.",
            "Add specific examples from experience.",
            "Increase clarity and confidence.",
        ],
        "recommendation": "Prepare 3-5 structured examples and practice concise delivery.",
    }

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
OLLAMA_TIMEOUT_SECONDS = float(os.getenv("OLLAMA_TIMEOUT_SECONDS", "120"))
OLLAMA_ENABLED = _env_bool("OLLAMA_ENABLED", True)

def ask_llm_ollama(transcript: str, score: float) -> dict:
    if not OLLAMA_ENABLED:
        return _fallback_feedback(score)

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

    try:
        resp = requests.post(OLLAMA_URL, json=payload, timeout=OLLAMA_TIMEOUT_SECONDS)
        resp.raise_for_status()
        data = resp.json()
    except requests.RequestException as e:
        print(f"[ask_llm_ollama] Ollama unavailable, using fallback feedback: {e}")
        return _fallback_feedback(score)

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
