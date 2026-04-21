from typing import Optional
from schemas import Scores

ENGLISH_FILLERS = [
    "uh", "um", "er", "eh", "hmm", "ah", "uhh", "umm",
    "like", "kinda", "sorta", "maybe", "probably", "basically",
    "literally", "actually", "honestly", "seriously", "really",
    "just", "kind of", "sort of", "i guess", "i think",
    "you know", "i mean", "you see", "i suppose", "i feel like",
    "you know what i mean", "you know what i'm saying",
    "a little", "somewhat", "perhaps",
    "well", "so", "okay", "alright", "right", "anyway",
    "so yeah", "so um", "right so", "so basically", "so like",
    "to be honest", "truth be told", "at the end of the day",
    "the thing is", "i mean like", "essentially",
    "let me think", "hold on", "give me a second",
    "how do i put this", "what i’m trying to say is",
    "the point is", "you know like", "i'm not sure but",
    "i guess that's it", "that's basically it",
    "and stuff", "and things like that",
]

HEDGING_PHRASES = [
    "maybe", "i guess", "i think", "i'm not sure",
    "i'm not really sure", "sort of", "kind of",
    "probably", "i suppose", "i feel like",
]

TECH_KEYWORDS = [
    "api", "database", "sql", "http", "rest", "cache",
    "kotlin", "java", "react", "spring", "docker", "kubernetes",
    "architecture", "scalability", "performance", "security",
    "testing", "unit test", "integration test", "design pattern",
]

def clamp_score(x: float) -> float:
    return max(0.0, min(100.0, x))


def calibrate_score(sim: float) -> float:
    sim = max(0.0, min(1.0, sim))

    if sim <= 0.3:
        return (sim / 0.3) * 10.0    
    elif sim >= 0.9:
        return 80.0
    else:
        return 10.0 + (sim - 0.3) * (70.0 / 0.6)


def evaluate_pace(word_count: int, duration_seconds: Optional[float]) -> tuple[str, Optional[float]]:
    if not duration_seconds or duration_seconds <= 0 or word_count <= 0:
        return "Unknown", None

    wpm = (word_count / duration_seconds) * 60.0

    if wpm < 110:
        return "Too Slow", 0.6
    elif 110 <= wpm <= 160:
        return "Optimal", 0.9
    elif 160 < wpm <= 190:
        return "Slightly Fast", 0.8
    else:
        return "Too Fast", 0.6


def compute_subscores(
    base_score: float,
    transcript: str,
    word_count: int,
    filler_rate: float,
    pace_label: str,
) -> Scores:
    text = transcript.lower()

    comm = base_score

    if pace_label in ("Too Fast", "Too Slow"):
        comm -= 7 

    if word_count < 40:
        comm -= 15   
    elif word_count < 70:
        comm -= 8   

    comm -= filler_rate * 45.0  

    comm = clamp_score(comm)

    tech = base_score

    if word_count < 80:
        tech -= 10 

    hits = sum(1 for kw in TECH_KEYWORDS if kw in text)
    
    tech += min(hits * 1.5, 8.0)  

    tech = clamp_score(tech)

    conf = base_score

    hedges_count = sum(text.count(h) for h in HEDGING_PHRASES)
    conf -= hedges_count * 3.5   

    conf -= filler_rate * 35.0  

    if word_count < 40:
        conf -= 10             

    if word_count > 220:
        conf -= 5             

    conf = clamp_score(conf)

    return Scores(
        communication=round(comm, 1),
        technical=round(tech, 1),
        confidence=round(conf, 1),
    )

