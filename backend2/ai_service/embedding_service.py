import os
import sys
from typing import List
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

import numpy as np
from openai import OpenAI

from embedding_schemas import (
    EmbeddingRequest,
    EmbeddingResponse,
    SemanticSearchRequest,
    SemanticSearchResponse,
    SearchResult,
)


class OpenAIConfigError(RuntimeError):
    pass


def get_openai_client() -> OpenAI:
    api_key = os.getenv('OPENAI_API_KEY')
    if not api_key:
        raise OpenAIConfigError('OPENAI_API_KEY must be set for embeddings.')
    return OpenAI(api_key=api_key)


def generate_embedding(text: str) -> List[float]:
    """
    Generate 1536-dimensional embedding using OpenAI text-embedding-3-small model.
    """
    client = get_openai_client()
    try:
        response = client.embeddings.create(
            model='text-embedding-3-small',
            input=text.strip(),
        )
        if response.data and len(response.data) > 0:
            return response.data[0].embedding
        raise ValueError('OpenAI returned empty embedding')
    except Exception as exc:
        raise RuntimeError(f'Embedding generation failed: {exc}')


def embed_room_content(room_name: str, tags: List[str]) -> List[float]:
    """
    Combine room name and tags, then generate embedding.
    """
    combined_text = f'{room_name} {" ".join(tags)}'
    return generate_embedding(combined_text)


def cosine_similarity(vec_a: np.ndarray, vec_b: np.ndarray) -> float:
    """
    Calculate cosine similarity between two vectors.
    Returns value between -1 and 1 (typically 0 to 1 for embeddings).
    """
    norm_a = np.linalg.norm(vec_a)
    norm_b = np.linalg.norm(vec_b)
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return float(np.dot(vec_a, vec_b) / (norm_a * norm_b))


def semantic_search_rooms(
    query_text: str,
    candidate_rooms: List[dict],
    top_k: int = 5,
) -> List[dict]:
    """
    Perform semantic search on room embeddings using cosine similarity.
    
    Args:
        query_text: User search query
        candidate_rooms: List of dicts with {'roomId', 'name', 'embedding': [float]}
        top_k: Number of results to return
    
    Returns:
        List of results sorted by similarity (descending)
    """
    query_embedding = generate_embedding(query_text)
    query_vec = np.array(query_embedding, dtype=np.float32)
    
    results = []
    for room in candidate_rooms:
        if 'embedding' not in room or not room['embedding']:
            continue
        room_vec = np.array(room['embedding'], dtype=np.float32)
        similarity = cosine_similarity(query_vec, room_vec)
        results.append({
            'roomId': room.get('roomId') or room.get('id'),
            'name': room.get('name'),
            'similarity': similarity,
        })
    
    sorted_results = sorted(results, key=lambda x: x['similarity'], reverse=True)
    return sorted_results[:top_k]


def process_embedding_request(request: EmbeddingRequest) -> EmbeddingResponse:
    embedding = generate_embedding(request.text)
    return EmbeddingResponse(
        embedding=embedding,
        text=request.text,
        dimension=len(embedding),
    )


def process_semantic_search_request(request: SemanticSearchRequest) -> SemanticSearchResponse:
    results = semantic_search_rooms(
        request.queryText,
        request.candidateEmbeddings,
        request.topK,
    )
    search_results = [
        SearchResult(
            roomId=r['roomId'],
            name=r['name'],
            similarity=r['similarity'],
        )
        for r in results
    ]
    return SemanticSearchResponse(
        query=request.queryText,
        results=search_results,
        count=len(search_results),
    )
