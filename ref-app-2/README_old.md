# ref-app-2: Supply Chain Posture Demo Application

A simple Node.js/Express application designed to demonstrate diverse npm ecosystem dependencies with varying supply chain maturity levels.

## Purpose

This reference application serves as input for Scrutinizer experiments that evaluate:
- OpenSSF Scorecard accuracy
- Provenance/SLSA attestation detection
- Dependency drift and supply chain risk
- Policy compliance against baseline standards

## Dependencies

#### test

### Direct Production Dependencies (15)
- **express** 4.18.2 — Well-maintained web framework
- **lodash** 4.17.21 — Utility library with high Scorecard
- **axios** 1.6.0 — HTTP client with provenance
- **jsonwebtoken** 9.0.2 — JWT auth (known CVE history)
- **bcryptjs** 2.4.3 — Password hashing
- **helmet** 7.1.0 — Security middleware with high Scorecard
- **cors** 2.8.5 — CORS middleware
- **dotenv** 16.3.1 — Environment configuration
- **winston** 3.11.0 — Logging
- **mongoose** 8.0.0 — MongoDB ODM
- **pg** 8.11.3 — PostgreSQL driver
- **redis** 4.6.10 — Redis client
- **uuid** 9.0.0 — UUID generation
- **moment** 2.29.4 — Date library (deprecated/unmaintained)
- **chalk** 4.1.2 — Terminal colors (CJS format)

### Dev Dependencies (3)
- jest 29.7.0
- eslint 8.56.0
- nodemon 3.0.2

## Usage

### Installation
```bash
npm install
```

### Running the Application
```bash
npm start
```

The server will listen on port 3000 (or `$PORT` environment variable).

### Health Check
```bash
curl http://localhost:3000/health
```

### Endpoints
- `GET /health` — Health status and version
- `POST /auth/register` — Demo user registration
- `POST /auth/token` — JWT token generation
- `GET /api/external` — Proxy external API call

## Generating SBOM

Generate a CycloneDX SBOM (requires `@cyclonedx/cyclonedx-npm` installed globally or via npx):

```bash
npm run generate-sbom
```

This will create `sbom.json` in the project root.

## Experiments

This application is used in Scrutinizer experiments:

1. **Experiment 1** — Dependency drift detection (baseline vs. drift SBOM variants)
2. **Experiment 2** — Provenance/SLSA coverage accuracy (10-package curated SBOM)

See `/reference-env/experiments/` for ground truth files and expected outcomes.

## Notes

- **moment** (v2.29.4) is intentionally included despite being unmaintained to test policy handling of deprecated packages
- **chalk** (v4.1.2) is CommonJS-only; some policies may flag ESM-only requirements
- The application includes diverse dependency types (web framework, security, database, logging, auth) typical of production Node.js applications
- No actual MongoDB or PostgreSQL connections are required; the drivers are included for supply chain analysis only
