# ref-app-2: Supply Chain Posture Demo Application

A simple Node.js/Express application designed to demonstrate diverse npm ecosystem dependencies with varying supply chain maturity levels.

## Purpose

This reference application serves as input for Scrutinizer experiments that evaluate:
- OpenSSF Scorecard accuracy
- Provenance/SLSA attestation detection
- Dependency drift and supply chain risk
- Policy compliance against baseline standards

## Dependencies

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

Made a change