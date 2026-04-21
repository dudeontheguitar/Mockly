from pydantic import BaseModel
from typing import Optional, List

class EvaluateRequest(BaseModel):
    sessionId: str
    artifactUrl: str
    language: str | None = "en"
    transcript: str | None = None


class SpeechAnalysis(BaseModel):
    paceLabel: str
    paceScore: Optional[float] = None
    fillerWordsCount: int
    fillerWordRate: float


class Scores(BaseModel):
    communication: float
    technical: float
    confidence: float


class EvaluateResponse(BaseModel):
    overallScore: float
    overallLabel: str
    overallMessage: str
    strengths: List[str]
    areasToImprove: List[str]
    speechAnalysis: SpeechAnalysis
    summary: str
    recommendations: str
    scores: Optional[Scores] = None
    transcript: Optional[str] = None
