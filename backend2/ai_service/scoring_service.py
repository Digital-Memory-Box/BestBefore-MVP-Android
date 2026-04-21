import math
import os
import sys
from datetime import datetime
from typing import Optional
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from openai import OpenAI
from openai import APIError as OpenAIError

from schemas import (
    HybridScoreRequest,
    HybridScoreResponse,
    InteractionDto,
    InteractionType,
)


class OpenAIConfigError(RuntimeError):
    pass


def get_openai_client() -> OpenAI:
    api_key = os.getenv('OPENAI_API_KEY')
    if not api_key:
        raise OpenAIConfigError('OPENAI_API_KEY must be set for semantic reasoning.')
    return OpenAI(api_key=api_key)


def calculate_tag_score(source_tags: list[str], target_tags: list[str]) -> int:
    common = set([tag.strip().lower() for tag in source_tags]) & set([tag.strip().lower() for tag in target_tags])
    return min(len(common) * 10, 30)


def calculate_dwell_score(room_dwell_seconds: Optional[int]) -> int:
    if not room_dwell_seconds or room_dwell_seconds <= 0:
        return 0
    minutes = math.floor(room_dwell_seconds / 60)
    return min(minutes, 2)


def calculate_interaction_score(source_interaction: Optional[InteractionDto], target_interaction: Optional[InteractionDto]) -> int:
    for interaction in [source_interaction, target_interaction]:
        if interaction:
            if interaction.interactionType == InteractionType.LIKE:
                return 8
            elif interaction.interactionType == InteractionType.IGNORE:
                return -6
    return 0


def haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    radius_km = 6371.0
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)

    a = math.sin(delta_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

    return radius_km * c


def calculate_location_score(source_room: dict, target_room: dict) -> int:
    distance_km = haversine_distance(source_room['lat'], source_room['lon'], target_room['lat'], target_room['lon'])
    if distance_km < 1:
        return 20
    if distance_km < 5:
        return 10
    if distance_km < 20:
        return 5
    return 0


def get_time_bucket(timestamp: Optional[datetime]) -> Optional[str]:
    if not timestamp:
        return None
    hour = timestamp.hour
    if 5 <= hour < 12:
        return 'morning'
    if 12 <= hour < 17:
        return 'afternoon'
    if 17 <= hour < 21:
        return 'evening'
    return 'night'


def calculate_time_score(source_interaction: Optional[InteractionDto], target_interaction: Optional[InteractionDto]) -> int:
    if not source_interaction or not target_interaction:
        return 0
    source_bucket = get_time_bucket(source_interaction.timestamp)
    target_bucket = get_time_bucket(target_interaction.timestamp)
    if source_bucket and target_bucket and source_bucket == target_bucket:
        return 10
    return 0


def calculate_hybrid_score(request: HybridScoreRequest) -> dict:
    tag_score = calculate_tag_score(request.sourceRoom.tags, request.targetRoom.tags)
    dwell_score = calculate_dwell_score(request.targetRoom.dwellTime)
    interaction_score = calculate_interaction_score(request.sourceInteraction, request.targetInteraction)
    location_score = calculate_location_score(request.sourceRoom.dict(), request.targetRoom.dict())
    time_score = calculate_time_score(request.sourceInteraction, request.targetInteraction)

    # Semantic-first hybrid: tag/location dominate, interaction is a light rerank signal.
    score = tag_score + dwell_score + interaction_score + location_score + time_score
    score = max(0, min(score, 100))

    if score >= 80:
        category = 'direct_match'
    elif score >= 40:
        category = 'critical_zone'
    else:
        category = 'reject'

    return {
        'score': score,
        'category': category,
        'needs_reasoning': category == 'critical_zone',
        'details': {
            'tagScore': tag_score,
            'dwellScore': dwell_score,
            'interactionScore': interaction_score,
            'locationScore': location_score,
            'timeScore': time_score,
        },
    }


def build_reasoning_prompt(request: HybridScoreRequest, score_details: dict) -> str:
    source_tags = ', '.join(request.sourceRoom.tags or [])
    target_tags = ', '.join(request.targetRoom.tags or [])
    prompt = (
        'Is there a semantic connection between these two rooms? Use the information below to provide a concise answer.\n\n'
        f'Room A: id={request.sourceRoom.id}, name={request.sourceRoom.name}, tags=[{source_tags}], isPrivate={request.sourceRoom.isPrivate}, isTimeCapsule={request.sourceRoom.isTimeCapsule}, dwellTime={request.sourceRoom.dwellTime}s, lat={request.sourceRoom.lat}, lon={request.sourceRoom.lon}, description={request.sourceRoom.description}\n'
        f'Room B: id={request.targetRoom.id}, name={request.targetRoom.name}, tags=[{target_tags}], isPrivate={request.targetRoom.isPrivate}, isTimeCapsule={request.targetRoom.isTimeCapsule}, dwellTime={request.targetRoom.dwellTime}s, lat={request.targetRoom.lat}, lon={request.targetRoom.lon}, description={request.targetRoom.description}\n\n'
        f'Score breakdown: Tag={score_details["tagScore"]}, Dwell={score_details["dwellScore"]}, Interaction={score_details["interactionScore"]}, Location={score_details["locationScore"]}, Time={score_details["timeScore"]}.\n\n'
        'If there is a connection, briefly explain it. If not, explain why.'
    )
    return prompt


def ask_gpt_for_semantic_connection(request: HybridScoreRequest, score_details: dict) -> str:
    client = get_openai_client()
    prompt = build_reasoning_prompt(request, score_details)
    try:
        response = client.chat.completions.create(
            model='gpt-4o-mini',
            max_tokens=256,
            messages=[
                {
                    'role': 'user',
                    'content': prompt,
                },
            ],
        )
        if hasattr(response, 'choices') and response.choices:
            return str(response.choices[0].message.content).strip()
    except OpenAIError as exc:
        return f'AI reasoning failed: {exc}'
    return 'OpenAI response could not be parsed.'


def score_with_reasoning(request: HybridScoreRequest) -> HybridScoreResponse:
    score_result = calculate_hybrid_score(request)
    reasoning_text = None
    if score_result['needs_reasoning']:
        reasoning_text = ask_gpt_for_semantic_connection(request, score_result['details'])

    return HybridScoreResponse(
        score=score_result['score'],
        category=score_result['category'],
        needsReasoning=score_result['needs_reasoning'],
        reasoning=reasoning_text,
        details=score_result['details'],
    )
