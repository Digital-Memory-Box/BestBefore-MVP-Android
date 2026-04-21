from typing import Dict, List

from embedding_service import generate_embedding
from preference_schemas import UpdatePreferenceRequest, UpdatePreferenceResponse


INTERACTION_WEIGHTS: Dict[str, float] = {
    'VIEW': 1.0,
    'LIKE': 3.0,
    'IGNORE': -2.0,
    'MEMORY_ADD': 2.0,
    'SUGGESTION_ACCEPT': 2.5,
    'SUGGESTION_REJECT': -1.5,
}


def _normalize_tags(tags: List[str]) -> List[str]:
    return [str(tag).strip().lower() for tag in tags if str(tag).strip()]


def _room_type(is_private: bool, is_time_capsule: bool) -> str:
    if is_time_capsule:
        return 'time_capsule'
    if is_private:
        return 'private'
    return 'public'


def _build_preference_embedding_input(preferred_tags: List[str], room_types: List[str]) -> str:
    return f'preferred tags: {" ".join(preferred_tags)} | room types: {" ".join(room_types)}'


def process_preference_update(request: UpdatePreferenceRequest) -> UpdatePreferenceResponse:
    profile = request.profile
    event = request.event

    tag_weights = dict(profile.preferenceTagWeights or {})
    delta = INTERACTION_WEIGHTS.get(event.interactionType, 0.0)
    for tag in _normalize_tags(event.tags):
        current = float(tag_weights.get(tag, 0.0))
        tag_weights[tag] = round(current + delta, 4)

    preferred_tags = [
        tag
        for tag, weight in sorted(tag_weights.items(), key=lambda item: item[1], reverse=True)
        if weight > 0
    ][: request.topKTags]

    room_types = list(dict.fromkeys((profile.preferenceRoomTypes or []) + [_room_type(event.isPrivate, event.isTimeCapsule)]))

    try:
        preference_embedding = generate_embedding(_build_preference_embedding_input(preferred_tags, room_types))
    except Exception:
        preference_embedding = profile.preferenceEmbedding or []

    return UpdatePreferenceResponse(
        preferredTags=preferred_tags,
        preferenceTagWeights=tag_weights,
        preferenceRoomTypes=room_types,
        preferenceEmbedding=preference_embedding,
        lastLat=event.lat if event.lat is not None else profile.lastLat,
        lastLon=event.lon if event.lon is not None else profile.lastLon,
    )
