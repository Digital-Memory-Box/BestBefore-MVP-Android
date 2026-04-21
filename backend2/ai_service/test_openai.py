#!/usr/bin/env python3
"""
Test OpenAI API key connectivity and functionality
Run: python test_openai.py
"""

import os
import sys
from openai import OpenAI

def test_api_key():
    """Test if OpenAI API key is valid and working"""
    api_key = os.getenv('OPENAI_API_KEY')
    
    if not api_key:
        print("❌ OPENAI_API_KEY not set in environment")
        return False
    
    if not api_key.startswith('sk-'):
        print("❌ OPENAI_API_KEY format invalid (should start with 'sk-')")
        return False
    
    print(f"✓ API Key found: {api_key[:20]}...")
    
    try:
        client = OpenAI(api_key=api_key)
        print("✓ OpenAI client initialized")
    except Exception as e:
        print(f"❌ Failed to initialize client: {e}")
        return False
    
    try:
        # Test embedding
        print("\n🔄 Testing text-embedding-3-small...")
        response = client.embeddings.create(
            model='text-embedding-3-small',
            input='Test message for BestBefore'
        )
        
        if response.data and len(response.data) > 0:
            embedding = response.data[0].embedding
            print(f"✓ Embedding successful! Dimension: {len(embedding)}")
        else:
            print("❌ No embedding data returned")
            return False
            
    except Exception as e:
        print(f"❌ Embedding test failed: {e}")
        return False
    
    try:
        # Test GPT-4o-mini
        print("\n🔄 Testing gpt-4o-mini...")
        response = client.chat.completions.create(
            model='gpt-4o-mini',
            max_tokens=100,
            messages=[
                {
                    'role': 'user',
                    'content': 'Write a 2-sentence nostalgic description for a memory room called "Sunset Beach".'
                }
            ]
        )
        
        if response.choices and len(response.choices) > 0:
            content = response.choices[0].message.content
            print(f"✓ GPT-4o-mini successful!")
            print(f"Response: {content[:100]}...")
        else:
            print("❌ No response from GPT-4o-mini")
            return False
            
    except Exception as e:
        print(f"❌ GPT-4o-mini test failed: {e}")
        return False
    
    print("\n" + "="*50)
    print("✅ All tests passed! API key is working.")
    print("="*50)
    return True


if __name__ == '__main__':
    success = test_api_key()
    sys.exit(0 if success else 1)
