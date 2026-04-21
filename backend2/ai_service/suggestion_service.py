from typing import List, Optional
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

import numpy as np

from schemas import RoomDto, UserPreferenceSchema
from scoring_service import calculate_hybrid_score, HybridScoreRequest
from embedding_service import cosine_similarity, semantic_search_rooms
from suggestion_schemas import (
    GenerateSuggestionsRequest,
    GenerateSuggestionsResponse,
    RoomSuggestion,
)


def calculate_combined_similarity(
    source_room_embedding: Optional[List[float]],
    target_room_embedding: Optional[List[float]],
    hybrid_score: int,
) -> float:
    """
    Combine hybrid score (0-100) and embedding cosine similarity (0-1).
    Semantic-first weighted: 35% score, 65% similarity.
    """
    if not source_room_embedding or not target_room_embedding:
        return hybrid_score / 100.0

    embedding_sim = cosine_similarity(
        np.array(source_room_embedding, dtype=np.float32),
        np.array(target_room_embedding, dtype=np.float32),
    )

    combined = (hybrid_score / 100.0) * 0.35 + embedding_sim * 0.65
    return min(1.0, max(0.0, combined))


def build_profile_source_room(profile: UserPreferenceSchema) -> RoomDto:
    return RoomDto(
        id='user-profile',
        name='User Preference Profile',
        tags=profile.preferredTags or [],
        isPrivate=False,
        isTimeCapsule=False,
        lat=profile.lastLat if profile.lastLat is not None else 0.0,
        lon=profile.lastLon if profile.lastLon is not None else 0.0,
        description='Virtual source room from user profile',
        dwellTime=0,
        embedding=profile.preferenceEmbedding or [],
    )


def has_user_history(profile: Optional[UserPreferenceSchema]) -> bool:
    if not profile:
        return False
    return bool(profile.interactionRoomTypes)


def initial_discovery(
    profile: UserPreferenceSchema,
    candidate_rooms: List[RoomDto],
    top_k: int = 10,
) -> List[RoomSuggestion]:
    preferred_tags = profile.preferredTags or []
    if not preferred_tags:
        return []

    query_text = ' '.join(preferred_tags)
    searchable = [
        {
            'id': room.id,
            'name': room.name,
            'embedding': room.embedding,
        }
        for room in candidate_rooms
        if room.embedding
    ]
    if not searchable:
        return []

    semantic_hits = semantic_search_rooms(
        query_text=query_text,
        candidate_rooms=searchable,
        top_k=top_k,
    )
    similarity_by_room = {hit['roomId']: hit['similarity'] for hit in semantic_hits}
    indexed_rooms = {room.id: room for room in candidate_rooms}

    suggestions = []
    for room_id, similarity in similarity_by_room.items():
        room = indexed_rooms.get(room_id)
        if not room:
            continue
        score = int(max(0.0, min(1.0, similarity)) * 100)
        category = 'direct_match' if score >= 80 else ('critical_zone' if score >= 40 else 'reject')
        if category == 'reject':
            continue
        suggestions.append(
            RoomSuggestion(
                targetRoomId=room.id,
                targetRoomName=room.name,
                score=score,
                category=category,
                reasoning='Initial discovery generated from user interest tags via semantic search.',
                similarity=max(0.0, min(1.0, similarity)),
            )
        )

    return sorted(suggestions, key=lambda x: (x.score, x.similarity), reverse=True)[:top_k]


def generate_generic_recommendations(
    user_lat: Optional[float],
    user_lon: Optional[float],
    candidate_rooms: List[RoomDto],
    top_k: int = 10,
) -> List[RoomSuggestion]:
    if user_lat is None or user_lon is None:
        return []

    generic_source = RoomDto(
        id='generic-location-source',
        name='Generic Discovery',
        tags=[],
        isPrivate=False,
        isTimeCapsule=False,
        lat=user_lat,
        lon=user_lon,
        description='Generic source for discovery',
        dwellTime=0,
        embedding=[],
    )

    results: List[RoomSuggestion] = []
    for candidate in candidate_rooms:
        score_req = HybridScoreRequest(
            sourceRoom=generic_source,
            targetRoom=candidate,
        )
        score_result = calculate_hybrid_score(score_req)
        score = score_result['score']
        if score < 10:
            continue
        results.append(
            RoomSuggestion(
                targetRoomId=candidate.id,
                targetRoomName=candidate.name,
                score=score,
                category=score_result['category'],
                reasoning='Generic discovery based on user location and room quality score.',
                similarity=score / 100.0,
            )
        )

    return sorted(results, key=lambda x: (x.score, x.similarity), reverse=True)[:top_k]


def generate_room_suggestions(
    request: GenerateSuggestionsRequest,
) -> GenerateSuggestionsResponse:
    """
    Generate room connection suggestions using hybrid scoring + embeddings.
    Returns top suggestions sorted by combined similarity.
    """
    if not request.candidateRooms:
        return GenerateSuggestionsResponse(
            sourceRoomId=request.sourceRoomId or 'discovery',
            suggestions=[],
            count=0,
        )

    profile = request.userProfile
    if not profile and request.sourceRoom:
        profile = UserPreferenceSchema(
            preferredTags=request.sourceRoom.tags or [],
            lastLat=request.userLat if request.userLat is not None else request.sourceRoom.lat,
            lastLon=request.userLon if request.userLon is not None else request.sourceRoom.lon,
            interactionRoomTypes=['source-room'],
        )

    if profile and not has_user_history(profile):
        cold_start_suggestions = initial_discovery(profile, request.candidateRooms, top_k=10)
        if cold_start_suggestions:
            return GenerateSuggestionsResponse(
                sourceRoomId=request.sourceRoomId or 'initial-discovery',
                suggestions=cold_start_suggestions,
                count=len(cold_start_suggestions),
            )

    if not profile or (not profile.preferredTags and not profile.interactionRoomTypes):
        generic_suggestions = generate_generic_recommendations(
            user_lat=request.userLat if request.userLat is not None else (profile.lastLat if profile else None),
            user_lon=request.userLon if request.userLon is not None else (profile.lastLon if profile else None),
            candidate_rooms=request.candidateRooms,
            top_k=10,
        )
        return GenerateSuggestionsResponse(
            sourceRoomId=request.sourceRoomId or 'generic-discovery',
            suggestions=generic_suggestions,
            count=len(generic_suggestions),
        )

    source_profile_room = build_profile_source_room(profile)
    suggestions = []

    for candidate in request.candidateRooms:
        if request.sourceRoomId and candidate.id == request.sourceRoomId:
            continue

        score_req = HybridScoreRequest(
            sourceRoom=source_profile_room,
            targetRoom=candidate,
        )

        try:
            score_result = calculate_hybrid_score(score_req)
            score = score_result['score']
            category = score_result['category']
            needs_reasoning = score_result['needs_reasoning']
        except Exception as exc:
            continue

        reasoning = None
        if needs_reasoning and score >= 40:
            reasoning = (
                f"Shared tags and location similarity create a semantic connection between these rooms."
            )

        combined_sim = calculate_combined_similarity(source_profile_room.embedding, candidate.embedding, score)

        if score >= 35 or combined_sim >= 0.45:
            suggestions.append(
                RoomSuggestion(
                    targetRoomId=candidate.id,
                    targetRoomName=candidate.name,
                    score=score,
                    category=category,
                    reasoning=reasoning,
                    similarity=combined_sim,
                )
            )

    sorted_suggestions = sorted(
        suggestions,
        key=lambda x: (x.similarity, x.score),
        reverse=True,
    )

    return GenerateSuggestionsResponse(
        sourceRoomId=request.sourceRoomId or 'user-profile',
        suggestions=sorted_suggestions[:10],
        count=len(sorted_suggestions[:10]),
    )
