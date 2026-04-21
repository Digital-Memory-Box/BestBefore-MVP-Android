import os
import sys
import base64
import re
from typing import List, Optional
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from openai import OpenAI

from generative_schemas import GenerateDescriptionRequest, GenerateDescriptionResponse


class OpenAIConfigError(RuntimeError):
    pass


def get_openai_client() -> OpenAI:
    api_key = os.getenv('OPENAI_API_KEY')
    if not api_key:
        raise OpenAIConfigError('OPENAI_API_KEY must be set for generative features.')
    return OpenAI(api_key=api_key)


def build_room_description_prompt(room_name: str, tags: List[str]) -> str:
    tags_str = ', '.join(tags) if tags else 'no specific tags'
    prompt = (
        f'In BestBefore, a memory room application, a new room is being created.\n\n'
        f'Room Name: {room_name}\n'
        f'Tags: {tags_str}\n\n'
        f'Please write ,according to rooms tags, a description for this room in exactly 2 sentences. '
        f'The description should be poetic, touching, and aligned with BestBefore concepts (past memories, time capsule, nostalgia, etc.). '
        f'Make it evocative and concise.\n\n'
        f'Response: exactly 2 sentences, no additional text.'
    )
    return prompt


def prepare_multimodal_content(
    room_name: str,
    tags: List[str],
    image_urls: Optional[List[str]] = None,
    image_base64_list: Optional[List[str]] = None,
) -> List[dict]:
    """
    Prepare multimodal content for OpenAI GPT-4o-mini.
    Returns array of content blocks (text + images).
    """
    content = []

    prompt = build_room_description_prompt(room_name, tags)
    content.append({'type': 'text', 'text': prompt})

    if image_urls:
        for url in image_urls[:4]:
            if url and isinstance(url, str) and (url.startswith('http://') or url.startswith('https://')):
                content.append({'type': 'image_url', 'image_url': {'url': url}})

    if image_base64_list:
        for b64_image in image_base64_list[:4]:
            if b64_image and isinstance(b64_image, str):
                if b64_image.startswith('data:'):
                    media_type = extract_media_type(b64_image)
                    data = b64_image.split(',', 1)[1] if ',' in b64_image else b64_image
                else:
                    media_type = 'image/jpeg'
                    data = b64_image

                content.append({
                    'type': 'image_url',
                    'image_url': {
                        'url': f'data:{media_type};base64,{data}',
                    },
                })

    return content


def extract_media_type(data_url: str) -> str:
    """Extract media type from data URL."""
    match = re.match(r'data:([^;]+);base64', data_url)
    if match:
        return match.group(1)
    return 'image/jpeg'


def generate_room_description(request: GenerateDescriptionRequest) -> GenerateDescriptionResponse:
    """
    Generate nostalgic, emotional 2-sentence description for room using GPT-4o-mini.
    Supports multimodal input (text + images).
    """
    client = get_openai_client()

    content = prepare_multimodal_content(
        request.roomName,
        request.tags,
        request.imageUrls,
        request.imageBase64,
    )

    has_images = bool(request.imageUrls or request.imageBase64)

    try:
        response = client.chat.completions.create(
            model='gpt-4o-mini',
            max_tokens=256,
            messages=[
                {
                    'role': 'user',
                    'content': content,
                }
            ],
        )

        if response.content and len(response.content) > 0:
            description = response.content[0].text.strip()
        else:
            description = f'A special moment captured in "{request.roomName}".'

        return GenerateDescriptionResponse(
            roomName=request.roomName,
            description=description,
            hasImages=has_images,
        )
    except Exception as exc:
        raise RuntimeError(f'Description generation failed: {exc}')
