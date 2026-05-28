# Favorites Feature Specification

## 1. Feature Overview

Users can save anime titles to a personal favorites list and remove them at will. Favorites are persisted in the database so they survive application restarts. The list is a single global list (no user authentication in this iteration); multi-user support is deferred to a future auth milestone.

Core behaviors:
- Add an anime to favorites (idempotent — adding a duplicate is a no-op, not an error)
- Remove an anime from favorites by its MyAnimeList ID (`malId`)
- Retrieve all favorited anime, ordered most-recently-added first
- Check whether a specific anime is already favorited (used by the UI to toggle button state)

---

## 2. H2 Database Schema

**Table name:** `favorites`

| Column       | Type          | Constraints                         | Notes                                       |
|--------------|---------------|-------------------------------------|---------------------------------------------|
| `id`         | BIGINT        | PRIMARY KEY, AUTO_INCREMENT         | Surrogate key                               |
| `mal_id`     | INTEGER       | NOT NULL, UNIQUE                    | MyAnimeList anime ID (from Jikan API)       |
| `title`      | VARCHAR(512)  | NOT NULL                            | English/romaji title, snapshotted at save   |
| `image_url`  | VARCHAR(1024) | NULLABLE                            | Cover image URL, snapshotted at save        |
| `score`      | DOUBLE        | NULLABLE                            | Score at time of save (informational)       |
| `added_at`   | TIMESTAMP     | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When the row was inserted                   |

**Design notes:**
- `mal_id` carries a `UNIQUE` constraint — the application layer also enforces no duplicates, but the DB constraint is the safety net.
- `title` and `image_url` are **snapshotted** at save time so the list remains stable even if Jikan data changes or the anime is temporarily unreachable.
- No foreign key to an `anime` table; anime data lives in Jikan's API, not locally.

---

## 3. REST Endpoints

Base path: `/api/favorites`

---

### 3.1 — List all favorites

| Field         | Value                        |
|---------------|------------------------------|
| **Method**    | `GET`                        |
| **Path**      | `/api/favorites`             |
| **Auth**      | None (single-user iteration) |
| **Request**   | No body                      |

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "malId": 5114,
    "title": "Fullmetal Alchemist: Brotherhood",
    "imageUrl": "https://cdn.myanimelist.net/images/anime/1223/96541.jpg",
    "score": 9.1,
    "addedAt": "2026-05-27T14:32:00"
  }
]
```
Returns an empty array `[]` when no favorites exist — never `404`.

---

### 3.2 — Add a favorite

| Field         | Value                     |
|---------------|---------------------------|
| **Method**    | `POST`                    |
| **Path**      | `/api/favorites`          |
| **Auth**      | None                      |
| **Content-Type** | `application/json`     |

**Request body:**
```json
{
  "malId": 5114,
  "title": "Fullmetal Alchemist: Brotherhood",
  "imageUrl": "https://cdn.myanimelist.net/images/anime/1223/96541.jpg",
  "score": 9.1
}
```
`malId` and `title` are required. `imageUrl` and `score` are optional.

**Response `201 Created`** — body is the newly created favorite (same shape as list item).

**Response `200 OK`** — returned instead of `201` if `malId` is already favorited (idempotent). Body is the existing record unchanged.

**Response `400 Bad Request`** — if `malId` is missing/null or `title` is blank.

---

### 3.3 — Remove a favorite

| Field      | Value                        |
|------------|------------------------------|
| **Method** | `DELETE`                     |
| **Path**   | `/api/favorites/{malId}`     |
| **Auth**   | None                         |

**Path variable:** `malId` — integer, the MyAnimeList ID.

**Response `204 No Content`** — success, no body.

**Response `404 Not Found`** — if no favorite with that `malId` exists.

**Response `400 Bad Request`** — if `malId` is not a valid integer.

---

### 3.4 — Check if an anime is favorited

| Field      | Value                        |
|------------|------------------------------|
| **Method** | `GET`                        |
| **Path**   | `/api/favorites/{malId}`     |
| **Auth**   | None                         |

**Response `200 OK`:**
```json
{ "favorited": true }
```
Always returns `200` — `favorited` is `false` when not found, not `404`. This keeps the Angular toggle logic simple.

---

## 4. Java Classes

All classes live under `src/main/java/com/anitracker/`.

---

### 4.1 Entity — `model/Favorite.java`

A plain JPA `@Entity` mapping to the `favorites` table. Fields: `id`, `malId`, `title`, `imageUrl`, `score`, `addedAt`. Use plain getters/setters (no Lombok). Annotate `addedAt` with `@Column(updatable = false)` and set it in a `@PrePersist` lifecycle callback so it is written once at insert time.

---

### 4.2 DTO — `model/FavoriteDto.java`

A Java **record** used for both request deserialization and response serialization:

```
record FavoriteDto(Long id, int malId, String title, String imageUrl, Double score, LocalDateTime addedAt)
```

The `id` and `addedAt` fields are `null` on inbound requests; they are populated on the outbound response. Validation annotations (`@NotNull`, `@NotBlank`) live here.

---

### 4.3 Repository — `repository/FavoriteRepository.java`

Extends `JpaRepository<Favorite, Long>`. Custom query methods needed:

- `Optional<Favorite> findByMalId(int malId)` — used by add (duplicate check) and delete
- `boolean existsByMalId(int malId)` — used by the status check endpoint
- `List<Favorite> findAllByOrderByAddedAtDesc()` — used by the list endpoint

---

### 4.4 Service — `service/FavoriteService.java`

Owns all business logic; the controller calls only service methods. Responsibilities:

- `getFavorites()` → `List<FavoriteDto>` — delegates to `findAllByOrderByAddedAtDesc()`, maps entities to DTOs
- `addFavorite(FavoriteDto request)` → `FavoriteDto` — checks for existing `malId`; if found, returns existing; otherwise saves new entity and returns the saved DTO
- `removeFavorite(int malId)` — finds by `malId`, throws `FavoriteNotFoundException` if absent, otherwise deletes
- `isFavorited(int malId)` → `boolean` — thin delegate to `existsByMalId`

`FavoriteNotFoundException` is a custom unchecked exception (in `service/` or a new `exception/` package) annotated `@ResponseStatus(HttpStatus.NOT_FOUND)`.

---

### 4.5 Controller — `controller/FavoriteController.java`

A `@RestController` at `@RequestMapping("/api/favorites")`. Thin layer: validates inputs, calls the service, returns appropriate HTTP status codes. Maps:

| Handler method     | HTTP verb + path          | Returns          |
|--------------------|---------------------------|------------------|
| `listFavorites`    | `GET /api/favorites`      | `200 List<FavoriteDto>` |
| `addFavorite`      | `POST /api/favorites`     | `201` or `200 FavoriteDto` |
| `removeFavorite`   | `DELETE /api/favorites/{malId}` | `204 void`  |
| `checkFavorited`   | `GET /api/favorites/{malId}` | `200 Map<String,Boolean>` |

`addFavorite` inspects whether the service found an existing record or created a new one — return `201 Created` for new, `200 OK` for idempotent hit. Use `ResponseEntity` to set status dynamically.

---

## 5. Angular Pieces

All files live under `C:\funProjects\anitracker-ui\src\app\`.

---

### 5.1 Service — `favorites/favorites.service.ts`

An `@Injectable({ providedIn: 'root' })` service wrapping `HttpClient`. Methods:

| Method signature                         | HTTP call                             |
|------------------------------------------|---------------------------------------|
| `getFavorites(): Observable<Favorite[]>` | `GET /api/favorites`                  |
| `addFavorite(anime: Anime): Observable<Favorite>` | `POST /api/favorites`        |
| `removeFavorite(malId: number): Observable<void>` | `DELETE /api/favorites/{malId}` |
| `isFavorited(malId: number): Observable<boolean>` | `GET /api/favorites/{malId}` — maps `{ favorited }` to `boolean` |

Define a `Favorite` TypeScript interface mirroring `FavoriteDto` (camelCase fields; `addedAt` as `string`).

---

### 5.2 Favorites List Component — `favorites/favorites.component.ts`

Standalone component (`standalone: true`). Responsibilities:

- On `ngOnInit`, call `FavoritesService.getFavorites()` and store results
- Display favorites in a card grid (reuse the same card layout as `TrendingComponent`)
- Each card has a **Remove** button; clicking it calls `removeFavorite(malId)` and splices the item from the local array optimistically
- Show an empty-state message ("No favorites yet — browse trending anime to add some!") when the list is empty
- Handle loading and error states with a simple flag + message

---

### 5.3 Favorite Toggle Button — `shared/favorite-button/favorite-button.component.ts`

Standalone component accepting `@Input() malId: number` and `@Input() anime: Anime`. Responsibilities:

- On init, calls `isFavorited(malId)` to set initial toggle state
- Clicking the button calls `addFavorite` or `removeFavorite` based on current state, then flips the local flag
- Emits `@Output() favoriteChanged = new EventEmitter<boolean>()` so parent components (e.g. `TrendingComponent`) can react
- Renders a filled/outlined heart or star icon to show state (CSS class swap, no library required)

This component is embedded in the existing anime card used by `TrendingComponent`, giving favorites access from both the trending and favorites pages.

---

### 5.4 Route

Add to the application routes array in `app.routes.ts`:

```
{ path: 'favorites', component: FavoritesComponent }
```

Add a **Favorites** nav link alongside the existing home/trending link in the shell/nav component.

---

## 6. Edge Cases to Handle

| # | Scenario | Expected behavior |
|---|----------|-------------------|
| 1 | `POST /api/favorites` with a `malId` that already exists | Return `200 OK` with the existing record; do **not** insert a duplicate or return an error |
| 2 | `DELETE /api/favorites/{malId}` for a `malId` not in the DB | Return `404 Not Found` with a descriptive error body |
| 3 | `POST /api/favorites` with missing or null `malId` | Return `400 Bad Request`; include field-level validation error message |
| 4 | `POST /api/favorites` with blank `title` | Return `400 Bad Request` |
| 5 | `{malId}` path variable is not a valid integer (e.g. `/api/favorites/abc`) | Spring's default `MethodArgumentTypeMismatchException` → `400 Bad Request` |
| 6 | `GET /api/favorites` when no favorites exist | Return `200 OK` with `[]`, not `404` |
| 7 | Angular add-favorite called while a previous request for the same `malId` is in-flight | Disable the button (set `loading = true`) until the Observable completes to prevent double submissions |
| 8 | Angular remove-favorite fails mid-flight (network error) | Roll back the optimistic UI update and display an inline error message |
| 9 | `imageUrl` exceeds `VARCHAR(1024)` | Truncate at the service layer before persisting (or increase column width) |
| 10 | Very large favorites list (hundreds of entries) | The list endpoint is currently unbounded; document a `TODO` to add pagination (`?page=&size=`) before going to production |
| 11 | Application restart loses all data (H2 in-memory) | Expected in this iteration; spec notes that switching to a file-based H2 URL (`jdbc:h2:file:./data/anitracker`) or PostgreSQL resolves this when persistence is needed |
