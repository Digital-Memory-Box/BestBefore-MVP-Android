import os
import sys
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse

# Fix relative imports
sys.path.insert(0, str(Path(__file__).parent))

from schemas import HybridScoreRequest, HybridScoreResponse
from scoring_service import score_with_reasoning, OpenAIConfigError
from embedding_schemas import EmbeddingRequest, EmbeddingResponse, SemanticSearchRequest, SemanticSearchResponse
from embedding_service import process_embedding_request, process_semantic_search_request
from generative_schemas import GenerateDescriptionRequest, GenerateDescriptionResponse
from generative_service import generate_room_description
from suggestion_schemas import GenerateSuggestionsRequest, GenerateSuggestionsResponse
from suggestion_service import generate_room_suggestions
from preference_schemas import UpdatePreferenceRequest, UpdatePreferenceResponse
from preference_service import process_preference_update

app = FastAPI(
    title='AI Scoring, Embedding, Generative & Suggestion Service',
    description='Comprehensive AI microservice for hybrid scoring, semantic search, embedding, description generation, and room suggestions.',
    version='0.4.0',
)


@app.get('/health')
def health() -> JSONResponse:
    return JSONResponse({'status': 'ok'})


@app.post('/v1/score', response_model=HybridScoreResponse)
def score(request: HybridScoreRequest) -> HybridScoreResponse:
    try:
        return score_with_reasoning(request)
    except OpenAIConfigError as config_err:
        raise HTTPException(status_code=500, detail=str(config_err))
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f'Hybrid scoring failed: {exc}')


@app.post('/v1/embed', response_model=EmbeddingResponse)
def embed(request: EmbeddingRequest) -> EmbeddingResponse:
    try:
        return process_embedding_request(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f'Embedding generation failed: {exc}')


@app.post('/v1/semantic-search', response_model=SemanticSearchResponse)
def semantic_search(request: SemanticSearchRequest) -> SemanticSearchResponse:
    try:
        return process_semantic_search_request(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f'Semantic search failed: {exc}')


@app.post('/v1/generate-description', response_model=GenerateDescriptionResponse)
def generate_description(request: GenerateDescriptionRequest) -> GenerateDescriptionResponse:
    try:
        return generate_room_description(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f'Description generation failed: {exc}')


@app.post('/v1/suggestions', response_model=GenerateSuggestionsResponse)
def suggestions(request: GenerateSuggestionsRequest) -> GenerateSuggestionsResponse:
    try:
        return generate_room_suggestions(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f'Suggestion generation failed: {exc}')


@app.post('/v1/user-preference/update', response_model=UpdatePreferenceResponse)
def update_user_preference(request: UpdatePreferenceRequest) -> UpdatePreferenceResponse:
    try:
        return process_preference_update(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f'User preference update failed: {exc}')
