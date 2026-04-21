from sentence_transformers import SentenceTransformer, util
import math

model = SentenceTransformer("intfloat/multilingual-e5-small")


def encode(text: str):
    return model.encode(text, convert_to_tensor=True, normalize_embeddings=True)


def normalize_similarity(sim: float) -> float:
    return 1 / (1 + math.exp(-10 * (sim - 0.6)))


def calculate_similarity(answer: str, reference: str) -> float:
    emb_answer = encode(f"query: {answer}")
    emb_reference = encode(f"passage: {reference}")

    raw_sim = util.cos_sim(emb_answer, emb_reference).item()

    scaled = normalize_similarity(raw_sim)

    return round(float(scaled), 3)
