from enum import Enum
from typing import List, Optional
from datetime import datetime

from pydantic import BaseModel, Field, validator


class InteractionType(str, Enum):
    VIEW = 'VIEW'
    LIKE = 'LIKE'
    IGNORE = 'IGNORE'
    OTHER = 'OTHER'


class LocationDto(BaseModel):
    lat: float = Field(..., description='Latitude of the room or interaction')
    lon: float = Field(..., description='Longitude of the room or interaction')


class RoomDto(BaseModel):
    id: str
    name: str
    tags: List[str] = Field(default_factory=list)
    isPrivate: bool
    isTimeCapsule: bool
    lat: float
    lon: float
    description: Optional[str] = None
    embedding: Optional[List[float]] = Field(
        default_factory=list,
        description='Vector embedding of room content',
    )
    dwellTime: Optional[int] = Field(
        0,
        description='Dwell time in seconds - how long the user stayed in the room',
        ge=0,
    )

    @validator('tags', each_item=True)
    def normalize_tag(cls, value: str) -> str:
        return value.strip().lower()


class InteractionDto(BaseModel):
    roomId: str
    dwellTimeSeconds: int = Field(..., ge=0)
    interactionType: InteractionType
    lat: float
    lon: float
    timestamp: Optional[datetime] = None


class UserPreferenceSchema(BaseModel):
    preferredTags: List[str] = Field(
        default_factory=list,
        description='User preferred tags collected from onboarding and behavior',
    )
    lastLat: Optional[float] = Field(
        None,
        description='Last known latitude of the user',
    )
    lastLon: Optional[float] = Field(
        None,
        description='Last known longitude of the user',
    )
    interactionRoomTypes: List[str] = Field(
        default_factory=list,
        description='Room type labels user interacted with',
    )
    preferenceEmbedding: Optional[List[float]] = Field(
        default_factory=list,
        description='Persistent user preference embedding',
    )

    @validator('preferredTags', each_item=True)
    def normalize_preferred_tag(cls, value: str) -> str:
        return value.strip().lower()


class HybridScoreRequest(BaseModel):
    sourceRoom: RoomDto
    targetRoom: RoomDto
    sourceInteraction: Optional[InteractionDto] = None
    targetInteraction: Optional[InteractionDto] = None


class HybridScoreResponse(BaseModel):
    score: int
    category: str
    needsReasoning: bool
    reasoning: Optional[str] = None
    details: dict
