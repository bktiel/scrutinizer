# Reference Environment

Test data for validating the Scrutinizer posture evaluation pipeline.

## SBOMs

All SBOMs are CycloneDX 1.5 JSON format, generated from the `ref-app-1/`
Nine Line MEDEVAC application (Spring Boot 3.3.0 + React 18 / TypeScript).

| SBOM | Ecosystem | Components | Edges | Source |
|------|-----------|------------|-------|--------|
| `ref-app-1-frontend.cdx.json` | npm | 50 | 73 | `ref-app-1/frontend/package.json` + realistic transitives |
| `ref-app-1-backend.cdx.json` | Maven | 52 | 57 | `ref-app-1/build.gradle` + Spring Boot transitive tree |
| `../tests/fixtures/sample_npm_sbom.json` | npm | 5 | 6 | Synthetic minimal express app (baseline) |

## ref-app-1-frontend.cdx.json

React 18 frontend with Material-UI, Axios, React Router, React Hook Form,
Zod validation, Cypress E2E testing, Vitest, and MSW for API mocking.

Key characteristics:
- 13 production dependencies, 18 dev dependencies, 19 transitive dependencies
- Scoped npm packages (`@mui/*`, `@emotion/*`, `@testing-library/*`)
- Expected good OpenSSF Scorecard coverage (React, MUI, Axios are well-maintained)
- npm ecosystem has growing Sigstore/SLSA provenance adoption

## ref-app-1-backend.cdx.json

Spring Boot 3.3.0 backend with JPA/Hibernate, Spring Security, Flyway,
PostgreSQL, Lombok, Jackson, and embedded Tomcat.

Key characteristics:
- 9 direct dependencies, 43 transitive dependencies
- Deep Spring Framework transitive tree (spring-core, spring-context, etc.)
- Maven Central ecosystem — expect Scorecard data for major projects
- SLSA provenance adoption is minimal on Maven Central (document the gap)

## Regenerating SBOMs

For production-accurate SBOMs with full transitive resolution:

```bash
# Frontend (from ref-app-1/frontend/)
cd ref-app-1/frontend && yarn install
npx @cyclonedx/cyclonedx-npm --output-file ../../reference-env/sboms/ref-app-1-frontend.cdx.json

# Backend (from ref-app-1/)
cd ref-app-1
# Add to build.gradle: id 'org.cyclonedx.bom' version '1.8.2' in plugins block
./gradlew cyclonedxBom
cp build/reports/bom.json ../reference-env/sboms/ref-app-1-backend.cdx.json
```

## Validation

Both SBOMs have been validated for:
- Correct CycloneDX 1.5 format parsing
- Deterministic output across 10 shuffled-input iterations
- Complete transitive dependency traversal from root component
- Correct purl format for both npm and Maven ecosystems
