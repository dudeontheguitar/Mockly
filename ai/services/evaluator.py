from typing import Optional
from pathlib import Path

from schemas import EvaluateRequest, EvaluateResponse, SpeechAnalysis
from services.audio_service import download_audio, get_audio_duration_seconds
from services.scoring import (
    calibrate_score,
    clamp_score,
    evaluate_pace,
    compute_subscores,
    ENGLISH_FILLERS,
)
from services.llm_client import ask_llm_ollama
from speech_to_text import transcribe_audio
from ml_core import calculate_similarity

REFERENCE_TEXT = (
        "A strong interview answer is clear, structured, confident and relevant "
        "to the question. It avoids repeating filler words or pauses. "
        "The answer should be logical and follow structure (such as the STAR method: "
        "Situation, Task, Action and Result). It includes real examples from experience, "
        "key decisions, reasoning and impact. Language is confident, concise and meaningful. "
        "The answer shows problem-solving, critical thinking, self-awareness, and domain knowledge."
    )

def evaluate_request(req: EvaluateRequest) -> EvaluateResponse:
    transcript: Optional[str] = None
    duration_seconds: Optional[float] = None

    if req.artifactUrl:
        try:
            audio_path = download_audio(req.artifactUrl)
            duration_seconds = get_audio_duration_seconds(audio_path)
            transcript = transcribe_audio(audio_path)
        except Exception as e:
            print(f"[evaluate] STT or duration failed: {e}")

    if not transcript:
        transcript = req.transcript

    if not transcript:
        return EvaluateResponse(
            overallScore=0,
            overallLabel="No Answer",
            overallMessage="We couldn't detect any interview answer.",
            strengths=[],
            areasToImprove=["Please provide an audio or text interview answer."],
            speechAnalysis=SpeechAnalysis(
                paceLabel="Unknown",
                paceScore=None,
                fillerWordsCount=0,
                fillerWordRate=0.0,
            ),
            summary="No transcript detected.",
            recommendations="Please provide an audio or text interview answer.",
            scores=None,
            transcript=None,
        )

    similarity = calculate_similarity(transcript, REFERENCE_TEXT) 
    base_score = calibrate_score(similarity)                  

    word_count = len(transcript.split())

    text_lower = transcript.lower()
    filler_count = sum(text_lower.count(fw) for fw in ENGLISH_FILLERS)
    filler_rate = round(filler_count / word_count, 3) if word_count > 0 else 0.0

    pace_label, pace_score = evaluate_pace(word_count, duration_seconds)

    speech = SpeechAnalysis(
        paceLabel=pace_label,
        paceScore=pace_score,
        fillerWordsCount=filler_count,
        fillerWordRate=filler_rate,
    )

    scores = compute_subscores(
        base_score=base_score,
        transcript=transcript,
        word_count=word_count,
        filler_rate=filler_rate,
        pace_label=pace_label,
    )

    overall_score = (
        scores.technical * 0.45 +
        scores.communication * 0.30 +
        scores.confidence * 0.25
    )
    overall_score = round(clamp_score(overall_score), 1)

    llm = ask_llm_ollama(transcript, overall_score)

    return EvaluateResponse(
        overallScore=overall_score,
        overallLabel=llm.get("label", "Interview Feedback"),
        overallMessage=llm.get("message", "Here is your performance feedback."),
        strengths=llm.get("strengths", []),
        areasToImprove=llm.get("areasToImprove", []),
        summary=f"Transcript detected ({word_count} words). AI evaluation completed.",
        recommendations=llm.get(
            "recommendation",
            "Keep practicing structured answers with clear examples.",
        ),
        speechAnalysis=speech,
        scores=scores,
        transcript=transcript,
    )
