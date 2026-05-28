# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Anitracker** is a Spring Boot REST API for tracking anime. It uses an H2 in-memory database (swappable for a persistent DB later) and Spring Data JPA for persistence.

- **Java 17**, **Spring Boot 3.5.14**
- **Spring Web** — REST controllers
- **Spring Data JPA** — repositories and entities
- **H2** — in-memory database (runtime scope; auto-configured)
- Base package: com.anitracker (not com.anitracker.anitracker)
- No Lombok — use plain Java getters/setters or Java records
- Angular frontend lives in: C:\funProjects\anitracker-ui (standalone components, no NgModules)
- Jikan API base URL: https://api.jikan.moe/v4
- No SSR on Angular side

## Commands

### Build & Run
```powershell
# Compile and package
./mvnw package

# Run the application (starts on http://localhost:8080)
./mvnw spring-boot:run

# Skip tests during build
./mvnw package -DskipTests
```

### Testing
```powershell
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=AnitrackerApplicationTests

# Run a single test method
./mvnw test -Dtest=AnitrackerApplicationTests#contextLoads
```

### H2 Console
When the app is running, the H2 console is available at `http://localhost:8080/h2-console` (requires enabling it in `application.properties`):
```properties
spring.h2.console.enabled=true
spring.datasource.url=jdbc:h2:mem:testdb
```

## Architecture

The codebase follows standard Spring Boot layering under `src/main/java/com/anitracker/`:

```
com.anitracker/
├── AnitrackerApplication.java   # Entry point (@SpringBootApplication)
├── model/                       # JPA @Entity classes
├── repository/                  # Spring Data JPA interfaces (extends JpaRepository)
├── service/                     # Business logic
└── controller/                  # @RestController classes (REST endpoints)
```

All application config lives in `src/main/resources/application.properties`. Tests live under `src/test/java/com/anitracker/` mirroring the main package structure.

## "What's Built So Far" listing:
- GET /api/anime/trending — calls Jikan /top/anime, returns List<AnimeDto>
- WebConfig.java in config/ — CORS config allowing localhost Angular ports
- Angular TrendingComponent at route /
