# Anime Ratings Comparison Specification

## 1. Feature Overview

Users can select any two anime and view them side by side in a structured comparison panel. The comparison highlights title, cover poster, score, episode count, media type, and genres so users can evaluate two titles at a glance.

Out of scope for this iteration:
- Comparing more than two anime at once
- Saving or sharing comparisons
- User authentication

Core behaviors:
- Each comparison slot has a search box; typing fetches results from the existing search endpoint
- Selecting an anime from results locks that slot and shows its details
- Both slots must be filled before the comparison table renders
- Either slot can be cleared and replaced at any time
- If a selected anime has no score or episode count, display "N/A" rather than blank

---

## 2. H2 Database Schema

No persistence required — comparison data is fetched live from Jikan and never stored.

---

## 3. REST Endpoints

Base path: `/api/anime`

---

### 3.1 — Fetch Comparison Data

| Field       | Value                                     |
|-------------|-------------------------------------------|
| **Method**  | `GET`                                     |
| **Path**    | `/api/anime/compare`                      |
| **Auth**    | None                                      |
| **Request** | Query params: `id1` (int), `id2` (int)    |

**Response `200 OK`:**
```json
{
  "left": {
    "malId": 5114,
    "title": "Fullmetal Alchemist: Brotherhood",
    "imageUrl": "https://cdn.myanimelist.net/images/anime/1223/96541.jpg",
    "score": 9.11,
    "episodes": 64,
    "type": "TV",
    "synopsis": "...",
    "genres": ["Action", "Adventure", "Drama"],
    "trailerUrl": "https://www.youtube.com/watch?v=..."
  },
  "right": {
    "malId": 1,
    "title": "Cowboy Bebop",
    "imageUrl": "https://cdn.myanimelist.net/images/anime/4/19644.jpg",
    "score": 8.76,
    "episodes": 26,
    "type": "TV",
    "synopsis": "...",
    "genres": ["Action", "Sci-Fi"],
    "trailerUrl": null
  }
}
```

**Response `400 Bad Request`** — if `id1` or `id2` is missing, not a valid integer, or `id1 == id2`.

**Response `404 Not Found`** — if either Jikan lookup returns no result. Body includes which ID failed.

---

## 4. Java Classes

All classes live under `src/main/java/com/anitracker/`.

---

### 4.1 DTO — `model/ComparisonDto.java`

A Java record wrapping two `AnimeDetailDto` instances:

```java
record ComparisonDto(AnimeDetailDto left, AnimeDetailDto right)
```

No persistence annotations needed — this is a pure response shape.

---

### 4.2 Service — `service/JikanService.java` (modified)

Add one public method:

| Signature | Responsibility |
|-----------|---------------|
| `getComparison(int id1, int id2) → ComparisonDto` | Calls `getAnimeDetail` for both IDs (sequentially), validates they are different, wraps results in `ComparisonDto`. Propagates `AnimeNotFoundException` from either lookup. |

No new class is needed — the method is added to the existing `JikanService`.

---

### 4.3 Controller — `controller/AnimeController.java` (modified)

Add one handler to the existing `AnimeController`:

| Handler method  | HTTP verb + path          | Return type     |
|-----------------|---------------------------|-----------------|
| `compare`       | `GET /api/anime/compare`  | `ComparisonDto` |

The handler accepts `@RequestParam int id1` and `@RequestParam int id2`, validates `id1 != id2` (throws `ResponseStatusException(BAD_REQUEST)` if equal), then delegates to `jikanService.getComparison(id1, id2)`.

---

## 5. Angular Pieces

All files live under `C:\funProjects\anitracker-ui\src\app\`.

---

### 5.1 Model — `models/anime.model.ts` (modified)

Add one interface:

```typescript
export interface ComparisonResult {
  left: AnimeDetail;
  right: AnimeDetail;
}
```

---

### 5.2 Service — `services/anime.service.ts` (modified)

Add one method:

| Method signature | HTTP call |
|-----------------|-----------|
| `compare(id1: number, id2: number): Observable<ComparisonResult>` | `GET /api/anime/compare?id1={id1}&id2={id2}` |

---

### 5.3 Compare Component — `compare/compare.component.ts`

Standalone component (`standalone: true`). Responsibilities:

- Two independent search slots, each containing a text input and a results dropdown
- Typing in either slot calls `AnimeService.search(query)` (debounced 300 ms)
- Selecting a result from the dropdown locks the slot with the chosen `AnimeDetail` (fetched via `AnimeService.getDetail(malId)`) and hides the dropdown
- A **Clear** button on each locked slot resets it to the search state
- When both slots are filled with different anime, calls `AnimeService.compare(id1, id2)` and renders the comparison table
- The comparison table shows: poster, title, score, episodes, type, genres — one row per field, one column per anime
- Loading state per slot and for the comparison fetch; error messages when Jikan is unreachable
- If `id1 == id2` (user picks the same anime twice), display an inline warning instead of calling the API

---

### 5.4 Template — `compare/compare.component.html`

Layout: two equal-width search panels above a full-width comparison table. The table has a label column on the left and one data column per anime. Genres render as comma-separated text. Null score/episodes render as "N/A".

---

### 5.5 Route

Add to `app.routes.ts`:

```typescript
{ path: 'compare', component: CompareComponent }
```

Add a **Compare** nav link in the app shell alongside Trending and Favorites.

---

## 6. Edge Cases

| # | Scenario | Expected behavior |
|---|----------|-------------------|
| 1 | `GET /api/anime/compare` with `id1 == id2` | Return `400 Bad Request`: "id1 and id2 must be different" |
| 2 | `GET /api/anime/compare` with a non-existent anime ID | Return `404 Not Found` propagated from `AnimeNotFoundException` |
| 3 | `GET /api/anime/compare` missing `id1` or `id2` param | Spring's `MissingServletRequestParameterException` → `400 Bad Request` |
| 4 | `id1` or `id2` is not a valid integer (e.g. `abc`) | Spring's `MethodArgumentTypeMismatchException` → `400 Bad Request` |
| 5 | Anime has `null` score | Backend passes `null` through; Angular renders "N/A" |
| 6 | Anime has `null` episodes | Backend passes `null` through; Angular renders "N/A" |
| 7 | User types in search box and picks same anime for both slots | Angular shows inline warning "Please choose two different anime"; does not call compare endpoint |
| 8 | User clears one slot while comparison is displayed | Comparison table is hidden; cleared slot returns to search state |
| 9 | Search returns empty results | Display "No results found" below the input |
| 10 | Jikan API is unreachable during comparison fetch | Display "Failed to load comparison. Please try again." and allow retry |
