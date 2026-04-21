import numpy as np
from typing import List

from pydantic import BaseModel, Field


class EmbeddingRequest(BaseModel):
    text: str = Field(..., description='Text to embed')


class EmbeddingResponse(BaseModel):
    embedding: List[float]
    text: str
    dimension: int


class SemanticSearchRequest(BaseModel):
    queryText: str = Field(..., description='User search query')
    candidateEmbeddings: List[dict] = Field(
        ...,
        description='List of room objects with id, name, embedding (list of floats)',
    )
    topK: int = Field(5, description='Number of results to return', ge=1, le=50)


class SearchResult(BaseModel):
    roomId: str
    name: str
    similarity: float


class SemanticSearchResponse(BaseModel):
    query: str
    results: List[SearchResult]
    count: int
