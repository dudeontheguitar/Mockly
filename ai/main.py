from fastapi import FastAPI
from schemas import EvaluateRequest, EvaluateResponse
from services.evaluator import evaluate_request

app = FastAPI(
    title="Mockly ML Service",
    description="ML + LLM interview evaluation service (E5 + Ollama)",
    version="0.3.0",
)

@app.get("/ping")
def ping():
    return {"status": "ok", "message": "ml-service is alive"}

@app.post("/api/evaluate", response_model=EvaluateResponse)
def evaluate(req: EvaluateRequest) -> EvaluateResponse:
    return evaluate_request(req)
