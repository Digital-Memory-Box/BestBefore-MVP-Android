from typing import List, Optional
from pydantic import BaseModel, Field

from schemas import RoomDto, UserPreferenceSchema


class GenerateSuggestionsRequest(BaseModel):
    sourceRoomId: Optional[str] = None
    sourceRoom: Optional[RoomDto] = None
    userProfile: Optional[UserPreferenceSchema] = None
    candidateRooms: List[RoomDto] = Field(
        ...,
        description='List of rooms to score against source room',
    )
    userLat: Optional[float] = None
    userLon: Optional[float] = None


class RoomSuggestion(BaseModel):
    targetRoomId: str
    targetRoomName: str
    score: int
    category: str
    reasoning: Optional[str] = None
    similarity: float = Field(
        ...,
        description='Combined score + embedding similarity',
    )


class GenerateSuggestionsResponse(BaseModel):
    sourceRoomId: str
    suggestions: List[RoomSuggestion]
    count: int
