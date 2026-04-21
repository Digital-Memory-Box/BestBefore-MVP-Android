from typing import Dict, List, Optional

from pydantic import BaseModel, Field


class PreferenceRoomEvent(BaseModel):
    tags: List[str] = Field(default_factory=list)
    isPrivate: bool = False
    isTimeCapsule: bool = False
    lat: Optional[float] = None
    lon: Optional[float] = None
    interactionType: str


class PreferenceProfileSnapshot(BaseModel):
    preferredTags: List[str] = Field(default_factory=list)
    preferenceTagWeights: Dict[str, float] = Field(default_factory=dict)
    preferenceRoomTypes: List[str] = Field(default_factory=list)
    preferenceEmbedding: List[float] = Field(default_factory=list)
    lastLat: Optional[float] = None
    lastLon: Optional[float] = None


class UpdatePreferenceRequest(BaseModel):
    profile: PreferenceProfileSnapshot
    event: PreferenceRoomEvent
    topKTags: int = Field(30, ge=5, le=100)


class UpdatePreferenceResponse(BaseModel):
    preferredTags: List[str]
    preferenceTagWeights: Dict[str, float]
    preferenceRoomTypes: List[str]
    preferenceEmbedding: List[float]
    lastLat: Optional[float]
    lastLon: Optional[float]
