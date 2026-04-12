# Scrutinizer CLI - File Structure and Documentation

This document describes all files in the scrutinizer-cli directory and their purposes.

## Directory Structure

```
scrutinizer-cli/
├── scrutinizer-hook              (259 lines) - Main pre-commit hook script
├── install.sh                    (156 lines) - Installation/setup script
├── .scrutinizer.env.template     (62 lines)  - Configuration template
├── README.md                     (329 lines) - Full user documentation
├── QUICKSTART.md                 (119 lines) - 5-minute getting started guide
├── INTEGRATION.md                (380 lines) - CI/CD integration examples
├── FILES.md                      (this file) - File descriptions
└── ci-templates/
    └── .scrutinizer-ci.yml       (227 lines) - GitLab CI template
```

**Total: 7 files, 1532 lines of code/documentation**

## Executable Scripts

### `scrutinizer-hook` (259 lines)

The main pre-commit hook that integrates with git and the Scrutinizer API.

**Features:**
- Detects dependency file changes (pom.xml, package.json, requirements.txt, go.mod, Cargo.toml, build.gradle, Gemfile.lock, yarn.lock, package-lock.json)
- Early exit for clean commits (no dependency changes)
- Loads configuration from `.scrutinizer.env` or environment variables
- Locates or generates SBOM using cdxgen
- Sends SBOM to Scrutinizer API endpoint: `POST /api/v1/runs`
- Parses JSON response with fallback to grep (no jq required)
- Outputs colored terminal messages (red/green/yellow/blue)
- Measures and reports elapsed time in milliseconds
- Exit codes:
  - 0: success (PASS or WARN decision)
  - 1: policy violation (FAIL decision)
  - 2: configuration error (missing POLICY_ID)
  - 3: API/connection error (non-blocking, allows commit)
- Gracefully handles:
  - API unreachable (doesn't block commit)
  - Missing SBOM (attempts generation or warns)
  - Missing jq (falls back to grep/sed)

**Configuration (environment variables):**
- `SCRUTINIZER_API_URL` - API base URL (default: http://localhost:8080)
- `SCRUTINIZER_POLICY_ID` - Policy UUID (required)
- `SCRUTINIZER_PROJECT_ID` - Project UUID (optional)
- `SCRUTINIZER_APP_NAME` - App name (default: git repo name)
- `SCRUTINIZER_SBOM_PATH` - SBOM file path (default: ./sbom.json)
- `SCRUTINIZER_FAIL_ON_WARN` - Block on WARN too (default: false)
- `SCRUTINIZER_SKIP_HOOK` - Disable hook (escape hatch)

**Performance:**
- Clean commits: <100ms (early exit)
- With dependencies: ~2-3 seconds (API evaluation)
- First run with SBOM generation: ~4-5 seconds

### `install.sh` (156 lines)

Installation script that sets up the pre-commit hook in a git repository.

**Functions:**
1. Validates it's running in a git repository
2. Creates `.git/hooks/` directory if needed
3. Installs `scrutinizer-hook` as the pre-commit hook
4. Appends to existing pre-commit hooks (if present)
5. Creates `.scrutinizer.env` from template
6. Prints setup instructions

**Usage:**
```bash
cd /your/repo
/path/to/scrutinizer-cli/install.sh
```

**Output:**
- Copies hook to `.git/hooks/pre-commit`
- Creates `.scrutinizer.env` config file
- Prints next steps for configuration

## Configuration Files

### `.scrutinizer.env.template` (62 lines)

Template configuration file for projects.

**Contents:**
- Required: `SCRUTINIZER_API_URL`, `SCRUTINIZER_POLICY_ID`
- Optional: `PROJECT_ID`, `APP_NAME`, `SBOM_PATH`, `FAIL_ON_WARN`, `SKIP_HOOK`
- Detailed comments explaining each option
- Environment variable precedence rules
- Emergency bypass instructions

**Usage:**
1. Copy to `.scrutinizer.env` in your project
2. Customize the values
3. Commit to git (or add to .gitignore for secrets)

## Documentation Files

### `README.md` (329 lines)

Comprehensive user documentation.

**Sections:**
- Overview - what the hook does
- Installation (quick start and manual)
- Configuration (required and optional)
- Usage examples
- SBOM management options
- Output examples (PASS/FAIL/WARN/API error)
- CI/CD integration pointers
- Exit codes reference
- Troubleshooting guide
- Development section

**Audience:** End users, DevOps engineers

### `QUICKSTART.md` (119 lines)

5-minute getting started guide.

**Contents:**
- Installation in 3 steps
- Minimal configuration
- How to test it
- Quick troubleshooting
- Emergency bypass command
- Next steps (point to full docs)

**Audience:** New users in a hurry

### `INTEGRATION.md` (380 lines)

Integration guide for CI/CD systems and enterprises.

**Sections:**
- Developer setup (single/multiple repos, enterprise)
- CI/CD integration for:
  - GitLab CI (template and direct job)
  - GitHub Actions (workflow example)
  - Jenkins (Groovy pipeline example)
- Performance measurement
- Comprehensive troubleshooting
- Experiment 4 metrics collection
- Additional resources

**Audience:** DevOps/platform teams, CI administrators

### `FILES.md` (this file)

File structure documentation.

**Purpose:** Explain what each file does and how they relate

## CI/CD Templates

### `ci-templates/.scrutinizer-ci.yml` (227 lines)

Reusable GitLab CI template for policy evaluation.

**Features:**
- Base job (`.scrutinizer-scan`) that can be extended
- Configurable via CI variables
- Before script: validates required configuration, checks SBOM exists
- Main script:
  - Builds API request with SBOM
  - Calls `POST /api/v1/runs` endpoint
  - Parses JSON response
  - Formats findings output
  - Sets exit code based on decision
- After script: saves report as artifact
- Artifacts: posture-report.json
- Retry logic: 2 retries on script failure
- Timeout: 5 minutes
- Optional variants:
  - `.scrutinizer-scan-strict` (allow_failure: false)
  - `.scrutinizer-scan-advisory` (allow_failure: true)

**Usage:**
```yaml
# In .gitlab-ci.yml
include:
  - project: 'your-group/scrutinizer'
    file: '/scrutinizer-cli/ci-templates/.scrutinizer-ci.yml'

scrutinizer-evaluation:
  extends: .scrutinizer-scan
  variables:
    SCRUTINIZER_POLICY_ID: "your-uuid"
```

## Key Features

### 1. Fast Path for Clean Commits
- Checks if dependency files are staged
- If none found, exits immediately with status 0
- Overhead: <100ms

### 2. Dependency File Detection
Watches for changes in:
- Java: pom.xml, build.gradle
- Node.js: package.json, yarn.lock, package-lock.json
- Python: requirements.txt
- Go: go.mod
- Rust: Cargo.toml
- Ruby: Gemfile.lock

### 3. SBOM Management
- Looks for existing SBOM file
- Attempts to generate using cdxgen if missing
- Graceful fallback if cdxgen not available
- Configurable SBOM path

### 4. Colored Output
- Green: PASS decisions
- Yellow: WARN decisions
- Red: FAIL decisions
- Blue: Info messages

### 5. Resilient API Integration
- Uses curl (standard, always available)
- Handles connection failures gracefully
- Falls back from jq to grep for JSON parsing
- Doesn't block commits on API errors
- Reports HTTP status codes

### 6. Performance Measurement
- Tracks elapsed time in milliseconds
- Prints timing for each evaluation
- Data useful for measuring CLI overhead

### 7. Graceful Degradation
- API unavailable: warn but allow commit
- SBOM missing: warn but allow commit
- jq not installed: use grep fallback
- Policy ID not set: block with helpful error

## Integration Points

### With Git
- Installs as pre-commit hook
- Can coexist with other hooks
- Integrated with git staging area

### With Scrutinizer API
- Endpoint: `POST /api/v1/runs`
- Payload: JSON with SBOM, policy ID, app name
- Response: JSON with decision (PASS/WARN/FAIL), findings array

### With SBOM Generation
- Supports cdxgen for auto-generation
- Falls back gracefully if unavailable

### With CI Systems
- GitLab CI template included
- GitHub Actions example in INTEGRATION.md
- Jenkins Groovy example in INTEGRATION.md
- Generic curl-based approach works everywhere

## Configuration Hierarchy

1. Environment variables (highest priority)
2. `.scrutinizer.env` file
3. Hard defaults (lowest priority)

Example:
```bash
# Use environment variable
SCRUTINIZER_POLICY_ID=override-id git commit

# Falls back to .scrutinizer.env
git commit

# Falls back to default
SCRUTINIZER_API_URL=http://localhost:8080
```

## Testing the Installation

```bash
# 1. Install
cd /your/repo
/path/to/scrutinizer-cli/install.sh

# 2. Configure
nano .scrutinizer.env

# 3. Generate SBOM
npm install -g @cyclonedx/cdxgen
cdxgen -o sbom.json

# 4. Test
echo "test" >> package.json
git commit -am "test: verify hook"

# Should output:
# [Scrutinizer] Checking for dependency changes...
# [Scrutinizer] Detected change: package.json
# [Scrutinizer] Sending SBOM to policy engine...
# [Scrutinizer] Policy check PASSED (2150ms)
```

## Troubleshooting Quick Reference

| Issue | File | Solution |
|-------|------|----------|
| Hook not running | README.md | Check .git/hooks/pre-commit is executable |
| Policy ID missing | QUICKSTART.md | Set in .scrutinizer.env |
| SBOM not found | README.md | Install cdxgen or generate manually |
| API unreachable | INTEGRATION.md | Check API URL and network connectivity |
| Performance slow | README.md | Pre-generate SBOM to avoid generation overhead |

## File Sizes and Metrics

| File | Lines | Size | Type |
|------|-------|------|------|
| scrutinizer-hook | 259 | 7.0K | Bash script |
| install.sh | 156 | 4.5K | Bash script |
| .scrutinizer.env.template | 62 | 2.4K | Config template |
| README.md | 329 | 8.5K | Markdown |
| QUICKSTART.md | 119 | 2.6K | Markdown |
| INTEGRATION.md | 380 | 9.0K | Markdown |
| FILES.md | ~ | ~ | Markdown |
| .scrutinizer-ci.yml | 227 | 6.7K | YAML |
| **Total** | **1532+** | **~41K** | **Runnable & Documented** |

## For Experiment 4 (Developer Workflow Impact)

This CLI tool provides:

1. **Timing Data**: Every evaluation outputs elapsed time in milliseconds
2. **Zero-Overhead Baseline**: Clean commits have <100ms overhead (early exit)
3. **Realistic Latency**: Dependency checks show actual API roundtrip times
4. **Flexible Configuration**: Can measure different policies and configurations
5. **CI Integration**: GitLab CI template for comparing local vs CI performance
6. **Graceful Failure**: Non-blocking on API errors (allows measuring impact on workflow)

Example data collection:

```bash
# Run multiple commits and parse timing
for i in {1..10}; do
    # Clean commit
    git commit --allow-empty -m "test $i"
done

# Extract timing from output
# Parse logs to measure average overhead
```

## License and Attribution

Part of the Scrutinizer project. See main repository for license information.

## Support and Questions

- See QUICKSTART.md for common issues
- See README.md for comprehensive documentation
- See INTEGRATION.md for CI/CD setup
- Check git repository for latest version
