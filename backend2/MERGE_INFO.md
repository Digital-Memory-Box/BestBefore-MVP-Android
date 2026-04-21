# backend-merged — Merge Info

**Created:** 2026-04-18  
**Base:** `backend-main` (production backend)  
**AI layer:** `backend-sevde-feature` (built by Sevde)

---

## What This Is

This folder is the result of merging Sevde's AI feature branch into the main backend. It is a complete, runnable Node.js backend that includes all features from both branches.

---

## What Was Merged From Sevde's Branch

### New files (additive — no conflicts)
| File | Purpose |
|------|---------|
| `src/controllers/semanticSearchController.js` | Embedding generation + semantic search via AI service |
| `src/controllers/suggestionController.js` | Room suggestions (AI + fallback), accept/reject with room linking |
| `src/services/generativeService.js` | Auto-generates nostalgic room descriptions via AI |
| `src/services/userPreferenceService.js` | Updates user preference profile from interaction signals |
| `src/routes/searchRoutes.js` | `POST /search/search` — semantic search endpoint |
| `src/routes/suggestionRoutes.js` | Suggestion routes under `/rooms/:roomId/suggestions` |
| `ai_service/` | Python FastAPI microservice (port 8000) — handles embeddings, scoring, description generation |

### Modified files (manual merge)
| File | What changed |
|------|-------------|
| `src/app.js` | Added `searchRoutes` + `suggestionRoutes` imports and mounts; kept `tagsRoutes` and `knock` from main |
| `src/controllers/roomController.js` | Added AI import headers, helper functions (haversine, rate limiter, cache), `getInitialDiscoveryRooms`, VIEW preference tracking in `getRoomById`, embedding + description generation hooks in `createRoom` and `updateRoom` |
| `src/routes/roomRoutes.js` | Added `POST /rooms/discover/initial` route |
| `src/controllers/inviteController.js` | Replaced with Sevde's version — handshake invites now use a 2-digit passcode and expire in 10 minutes instead of 24 hours |

### Kept from main (Sevde had removed these)
- `src/controllers/knockController.js` — Knock feature (`POST /users/:email/knock`)
- `src/controllers/tagsController.js` — Tag management
- `src/routes/tagsRoutes.js`

---

## New Routes Added

| Route | Method | Auth | Description |
|-------|--------|------|-------------|
| `/rooms/discover/initial` | POST | Yes | Onboarding discovery — semantic search by preferred tags + location, falls back to recency |
| `/rooms/:roomId/suggestions` | GET | Yes | AI-powered similar room suggestions with fallback scoring |
| `/rooms/:roomId/suggestions/:suggestionId/accept` | POST | Yes | Accept suggestion — creates bidirectional `connectedRooms` link |
| `/rooms/:roomId/suggestions/:suggestionId/reject` | POST | Yes | Reject suggestion — stores in `rejectedSuggestions` |
| `/search/search` | POST | Yes | Semantic search across public rooms by query text + optional location |

---

## Handshake Invite Change (Breaking for iOS)

Sevde's `inviteController.js` changes the handshake accept flow:

**Before (main):**
- Invite valid for 24 hours
- Accept requires no code — just `POST /handshake-invites/:inviteId/accept`

**After (merged):**
- Invite valid for 10 minutes
- Accept requires passcode: `POST /handshake-invites/:inviteId/accept` with `{ "code": "42" }`
- Passcode is returned in the `createHandshakeInvite` response as `passcode`

**iOS action required:** Update `InviteManager.swift` and accept flow to pass `code` in the request body.

---

## New Schema Fields

### Room (MongoDB)
```
embedding: number[]           — 1536-dim vector (text-embedding-3-small)
embeddingUpdatedAt: Date
generatedDescription: string  — AI-generated 2-sentence description
connectedRooms: string[]      — roomIds of accepted suggestions
rejectedSuggestions: string[] — roomIds of rejected suggestions
```

### User (MongoDB)
```
preferredTags: string[]
preferenceTagWeights: { [tag]: number }
preferenceRoomTypes: string[]
preferenceEmbedding: number[]
preferenceInteractions: [{ type, roomId, at }]
preferenceUpdatedAt: Date
lastLat: number
lastLon: number
```

These fields are added lazily — no migration needed for existing documents. They populate on first interaction.

---

## Running This Backend

### Node API (port 3000)
```bash
npm start
```

### Python AI Microservice (port 8000) — required for AI features
```bash
cd ai_service
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The Node backend gracefully falls back to tag/distance scoring if the AI service is unavailable.

### Environment variables
Copy `.env.example` to `.env` and fill in values. `OPENAI_API_KEY` is required for the Python service only.

---

## Feature Requirements Addressed

| FR | Feature | Status |
|----|---------|--------|
| FR-009 | Room Flow & Linking | ✅ `connectedRooms` via suggestion accept |
| FR-010 | AI Connection Suggestion | ✅ Semantic similarity + hybrid scoring |
| FR-012 | Search & Location | ✅ Semantic search + location-aware discovery |
