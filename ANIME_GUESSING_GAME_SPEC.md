# Anime Guessing Game — Feature Specification

## 1. Feature Overview

Users play a 10-round guessing game where each round displays a blurred anime poster alongside four multiple-choice answers (one correct, three random decoys drawn from top anime). Each wrong guess reduces the blur level, progressively revealing the poster as a hint. Scoring rewards faster correct guesses: 100 pts on the first attempt, 60 on the second, 30 on the third, 0 if all three attempts are exhausted. Each round has a 30-second countdown timer; time expiring counts as a failed round (0 pts). A final score screen is shown after all 10 rounds.

**Out of scope for this iteration:**
- Persistent high-score / leaderboard storage
- User authentication or per-user score history
- Difficulty settings or configurable round counts
- Multiplayer or real-time features

**Core behaviors:**
- Backend selects 10 unique anime randomly from Jikan `/top/anime` and returns them with decoy choices, without exposing the correct answer index
- Angular owns all game state: current round, attempts, score, timer, blur level
- Blur starts at `20px` and decreases by `6px` per wrong guess (floor `2px`)
- Timer counts down from 30 s; expiry ends the round and moves to the next
- After round 10, the game transitions to a results screen showing total score and a per-round breakdown
- "Play Again" resets state and fetches a fresh game from the backend

---

## 2. H2 Database Schema

No persistence required for this feature. All game state is ephemeral and managed client-side. Backend is stateless — it generates a fresh game on each request.

---

## 3. REST Endpoints

Base path: `/api/game`

---

### 3.1 — Start a new game

| Field        | Value                        |
|--------------|------------------------------|
| **Method**   | `GET`                        |
| **Path**     | `/api/game/start`            |
| **Auth**     | None                         |
| **Request**  | No body, no query parameters |

**Response `200 OK`:**
```json
{
  "rounds": [
    {
      "roundNumber": 1,
      "imageUrl": "https://cdn.myanimelist.net/images/anime/1223/96541.jpg",
      "choices": [
        "Fullmetal Alchemist: Brotherhood",
        "Attack on Titan",
        "Demon Slayer",
        "One Piece"
      ],
      "answerIndex": 0
    }
    // ... 9 more rounds
  ]
}
```

`answerIndex` is the 0-based index of the correct answer within the `choices` array. The frontend uses this to evaluate guesses without making additional network calls.

**Response `502 Bad Gateway`** — if the Jikan API is unreachable. Body:
```json
{ "error": "Unable to fetch anime data. Please try again later." }
```

**Response `500 Internal Server Error`** — for unexpected failures.

---

## 4. Java Classes

All classes under `src/main/java/com/anitracker/`.

---

### 4.1 DTO — `model/GameRoundDto.java`

Java record representing a single game round sent to the client:

```
record GameRoundDto(int roundNumber, String imageUrl, List<String> choices, int answerIndex)
```

---

### 4.2 DTO — `model/GameDto.java`

Java record wrapping all rounds for a full game:

```
record GameDto(List<GameRoundDto> rounds)
```

---

### 4.3 Service — `service/GameService.java`

Owns game-generation logic. Depends on `JikanService`.

| Method | Signature | Responsibility |
|--------|-----------|---------------|
| `generateGame` | `GameDto generateGame()` | Fetches top anime from Jikan, selects 10 unique rounds, builds 4 choices (1 correct + 3 random decoys), shuffles choices, sets `answerIndex` |

**Algorithm:**
1. Call `JikanService.getTrendingAnime()` to get a pool (≥ 25 entries expected).
2. Shuffle the pool and take up to 25.
3. For each of the first 10 entries (the "answer" anime): pick 3 additional random entries from the remaining pool as decoys, combine into a 4-element list, shuffle, record the correct answer's post-shuffle index.
4. Return a `GameDto` with `roundNumber` starting at 1.

If the pool has fewer than 4 anime total, throw `ResponseStatusException(502, "Insufficient anime data")`.

---

### 4.4 Controller — `controller/GameController.java`

`@RestController` at `@RequestMapping("/api/game")`. Thin layer calling `GameService`.

| Handler method | HTTP verb + path  | Returns        |
|----------------|-------------------|----------------|
| `startGame`    | `GET /api/game/start` | `200 GameDto` |

---

## 5. Angular Pieces

All files under `C:\funProjects\anitracker-ui\src\app\`.

---

### 5.1 Model — `game/game.model.ts`

TypeScript interfaces:

```typescript
export interface GameRound {
  roundNumber: number;
  imageUrl: string;
  choices: string[];
  answerIndex: number;
}

export interface GameData {
  rounds: GameRound[];
}

export interface RoundResult {
  roundNumber: number;
  correct: boolean;
  pointsEarned: number;
  attemptsTaken: number;
}
```

---

### 5.2 Service — `game/game.service.ts`

`@Injectable({ providedIn: 'root' })` wrapping `HttpClient`.

| Method | Signature | HTTP call |
|--------|-----------|-----------|
| `startGame` | `startGame(): Observable<GameData>` | `GET /api/game/start` |

---

### 5.3 Game Component — `game/game.component.ts`

Standalone component. Responsibilities:

- On init, call `GameService.startGame()` and enter the first round
- Manage signals: `currentRound`, `attemptsLeft` (starts 3), `score`, `blurPx` (starts 20), `gamePhase` (`'loading' | 'playing' | 'result' | 'finished'`), `timerSeconds` (starts 30), `roundResults`
- On each wrong guess: decrement `attemptsLeft`, reduce `blurPx` by 6 (min 2), check if attempts exhausted → advance round with 0 pts
- On correct guess: award pts (100/60/30), advance round
- Timer: use `setInterval` to decrement `timerSeconds`; on 0, treat as 0-pts round and advance
- On round advance: clear timer, reset `attemptsLeft = 3`, `blurPx = 20`, `timerSeconds = 30`, push `RoundResult` to history
- After round 10: set `gamePhase = 'finished'`
- "Play Again" resets all state and re-calls `startGame()`
- Clean up interval in `ngOnDestroy`

**Template features:**
- Blurred poster via inline `filter: blur(Xpx)` style binding
- 4 choice buttons; buttons disabled after correct guess or round expiry
- Correct answer highlighted green, wrong guess highlighted red
- Timer progress bar
- Running score display in header
- Results screen: total score, per-round table (round #, anime title, pts earned, attempts)

---

### 5.4 Route

Add to `app.routes.ts`:

```
{ path: 'game', loadComponent: () => import('./game/game.component').then(m => m.GameComponent) }
```

Add a **Game** nav link to `app.html` alongside existing nav links.

---

## 6. Edge Cases

| # | Scenario | Expected behavior |
|---|----------|-------------------|
| 1 | Jikan returns fewer than 4 anime | Backend returns `502` with `"Insufficient anime data"`; Angular shows an error screen with a retry button |
| 2 | Jikan API is unreachable (network timeout) | Backend returns `502`; Angular shows error state with retry; game does not start |
| 3 | Timer reaches 0 before any guess | Round ends immediately with 0 pts; next round begins automatically |
| 4 | User clicks a choice after timer expired | Buttons are disabled the moment the timer hits 0; click is a no-op |
| 5 | User clicks the correct answer on the first try | Award 100 pts, highlight button green, disable all buttons, auto-advance after 1 s |
| 6 | User exhausts all 3 attempts without guessing correctly | Award 0 pts, reveal correct answer (highlight it green), auto-advance after 1.5 s |
| 7 | Jikan returns duplicate titles in the pool | Deduplication by `malId` before building rounds; same title cannot appear as both answer and decoy |
| 8 | `imageUrl` is null for an anime entry | Skip that entry when building rounds; do not include anime with null images |
| 9 | Play Again clicked during an active round | Timer is cleared immediately, state is reset before the new game request fires |
| 10 | Network error mid-game (after game started) | Already downloaded; game continues unaffected — all data is local after `startGame()` resolves |
