# Anitracker Workflow Plugin

## What This Plugin Does

The `anitracker-workflow` plugin bundles a full spec-driven feature pipeline for this Spring Boot + Angular project. It wires together a `/new-feature` command that takes you from a GitHub issue all the way through implementation, automated tests, and a security review before opening a PR. Two specialized subagents (`@test-writer` and `@security-reviewer`) run in parallel after implementation, and two hooks guard against accidental deletions and keep edited Java/TypeScript files auto-formatted.

## Prerequisites

- **Java 17** — backend runtime and compilation
- **Node.js 18+** — required by Angular CLI
- **Angular CLI** (`npm install -g @angular/cli`) — for the frontend at `C:\funProjects\anitracker-ui`
- **Claude Code** — CLI (`npm install -g @anthropic-ai/claude-code`) or desktop app
- **GitHub CLI** (`gh`) — authenticated (`gh auth login`) for PR automation
- **GitHub MCP server** — see setup instructions at the bottom of this file

## Installation Steps (reusing in a new project)

1. Copy the entire `.claude/` folder from this project into the root of your new project.
2. Update `.claude/plugin.json` — change `name`, `author`, and `stack` to match your project.
3. Update `.claude/agents/test-writer.md` and `security-reviewer.md` to reflect your tech stack (language, test framework, etc.).
4. Update `.claude/commands/new-feature.md` with any project-specific conventions (base package, API base URL, etc.).
5. If your project is not on Windows, swap `pre-delete-guard.ps1` → `pre-delete-guard.sh` and `post-edit-format.ps1` → `post-edit-format.sh` in `.claude/settings.json`.
6. Run `claude` in the project root — the plugin is active immediately.

## How to Use `@test-writer`

The `@test-writer` agent writes JUnit 5 integration tests for Spring Boot controllers and services. It never modifies source files.

**Example prompt:**
```
@test-writer Write integration tests for AnimeController covering:
- GET /api/anime/trending returns 200 with a non-empty list
- GET /api/anime/search?q=naruto returns matching results
- GET /api/anime/compare?ids=1,2 returns two entries side by side
Use @SpringBootTest with MockMvc. No Mockito mocks for the database.
```

The agent will create test files under `src/test/java/com/anitracker/` mirroring the main package structure and report which test methods it added.

## How to Use `@security-reviewer`

The `@security-reviewer` agent audits staged or recently changed code for OWASP top-10 issues, secret exposure, CORS misconfiguration, H2 console exposure, and injection risks. It is read-only and never modifies files.

**Example prompt:**
```
@security-reviewer Review the changes on the current branch for security issues.
Focus on: SQL injection in search endpoints, CORS headers in WebConfig,
H2 console access controls, and any hardcoded credentials or API keys.
Report findings with file + line number and severity (critical / high / medium / low).
```

The agent returns a findings list you can act on before opening a PR.

## How to Use `/new-feature`

The `/new-feature` command runs the full pipeline: reads the GitHub issue → generates a spec → plans implementation → implements → runs `@test-writer` and `@security-reviewer` in parallel → opens a PR.

**Example prompt:**
```
/new-feature "Add POST /api/anime/watchlist endpoint so users can save anime to a personal watchlist"
```

What happens:
1. A GitHub issue is created (or linked if you pass `#123`) with an acceptance-criteria checklist.
2. A spec doc is drafted and shown to you for approval before any code is written.
3. Implementation runs across controller, service, repository, and entity layers.
4. Tests and security review run in parallel subagents.
5. A PR is opened against `main` with a summary, test plan, and link to the issue.

You can also pass an existing issue number:
```
/new-feature #7
```

## What the Hooks Protect Against

| Hook | File | Trigger | Protection |
|------|------|---------|------------|
| `PreToolUse` | `pre-delete-guard.ps1` | Before any file-delete or destructive bash command | Blocks accidental deletion of source files, entity classes, migration scripts, or `pom.xml`; prompts for confirmation if the target path looks like production code. |
| `PostToolUse` | `post-edit-format.ps1` | After every file edit | Runs `google-java-format` (if on PATH) on edited `.java` files and `prettier` (if on PATH) on edited `.ts`/`.html` files so code style stays consistent without a separate format step. |

Both hooks have `.sh` equivalents for macOS/Linux in the same `.claude/hooks/` directory.

## GitHub MCP Setup Instructions

The `/new-feature` command and `@security-reviewer` agent use the GitHub MCP server to read issues, push branches, and open PRs.

### 1. Install the GitHub MCP server

```bash
npm install -g @modelcontextprotocol/server-github
```

### 2. Add it to your Claude Code MCP config

Open (or create) `~/.claude/mcp_settings.json` and add:

```json
{
  "mcpServers": {
    "github": {
      "command": "mcp-server-github",
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "<your-token>"
      }
    }
  }
}
```

### 3. Create a GitHub Personal Access Token

1. Go to **GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens**.
2. Set repository access to this repo (`Abhisek542/anitracker`).
3. Grant permissions: **Contents** (read/write), **Issues** (read/write), **Pull requests** (read/write), **Metadata** (read).
4. Copy the token into the `mcp_settings.json` above.

### 4. Verify the connection

```bash
claude mcp list
```

You should see `github` listed as a connected server. If it shows as connecting, restart the Claude Code session.

> **Note:** Never commit `mcp_settings.json` or any file containing your PAT. Add `mcp_settings.json` to `.gitignore` if you place it inside the project directory.
