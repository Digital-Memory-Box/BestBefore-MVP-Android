from typing import List, Optional
from pydantic import BaseModel, Field


class GenerateDescriptionRequest(BaseModel):
    roomName: str = Field(..., description='Name of the room')
    tags: List[str] = Field(
        default_factory=list,
        description='Tags/categories for the room',
    )
    imageUrls: Optional[List[str]] = Field(
        None,
        description='Optional list of image URLs to include in description generation',
    )
    imageBase64: Optional[List[str]] = Field(
        None,
        description='Optional list of base64-encoded images (format: data:image/jpeg;base64,...)',
    )


class GenerateDescriptionResponse(BaseModel):
    roomName: str
    description: str = Field(
        ...,
        description='2-sentence nostalgic, emotional description for the room',
    )
    hasImages: bool
