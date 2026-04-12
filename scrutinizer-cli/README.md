# Scrutinizer Pre-Commit Hook

A git pre-commit hook that integrates with the Scrutinizer policy engine to block commits that violate dependency security policies.

## Overview

The Scrutinizer pre-commit hook automatically checks dependency files (pom.xml, package.json, requirements.txt, etc.) before allowing commits. If dependency changes are detected, it:

1. Looks for an SBOM file (or generates one using cdxgen)
2. Sends the SBOM to the Scrutinizer API for policy evaluation
3. Blocks the commit if the policy decision is FAIL
4. Allows the commit if the decision is PASS or WARN (configurable)

**Performance**: The hook is optimized for zero overhead on clean commits (no dependency changes). When dependency files change, typical evaluation takes <3 seconds.

## Installation

### Quick Start

```bash
cd /path/to/your/repo
/path/to/scrutinizer-cli/install.sh
```

This will:
1. Copy the hook to `.git/hooks/pre-commit`
2. Create a `.scrutinizer.env` configuration file (from template)
3. Print setup instructions

### Manual Installation

If you prefer to install manually:

```bash
# 1. Copy the hook to your git hooks directory
cp scrutinizer-cli/scrutinizer-hook .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# 2. Copy the config template
cp scrutinizer-cli/.scrutinizer.env.template .scrutinizer.env

# 3. Edit the config file with your settings
nano .scrutinizer.env
```

## Configuration

The hook is configured via environment variables, which can be set in:

1. **Environment**: `SCRUTINIZER_POLICY_ID=xxx git commit` (highest priority)
2. **`.scrutinizer.env`**: Project-level configuration file
3. **Defaults**: Built into the script

### Required Configuration

- `SCRUTINIZER_API_URL`: Base URL of the Scrutinizer API (default: http://localhost:8080)
- `SCRUTINIZER_POLICY_ID`: UUID of the policy to evaluate against (required, no default)

### Optional Configuration

- `SCRUTINIZER_PROJECT_ID`: UUID of the project for tracking (optional)
- `SCRUTINIZER_APP_NAME`: Application name (default: git repository name)
- `SCRUTINIZER_SBOM_PATH`: Path to SBOM file (default: ./sbom.json)
- `SCRUTINIZER_FAIL_ON_WARN`: If "true", blocks commits on WARN decisions (default: false)
- `SCRUTINIZER_SKIP_HOOK`: If "true", completely disables the hook (escape hatch)

### Example `.scrutinizer.env`

```bash
SCRUTINIZER_API_URL=http://scrutinizer-api.example.com:8080
SCRUTINIZER_POLICY_ID=550e8400-e29b-41d4-a716-446655440000
SCRUTINIZER_PROJECT_ID=123e4567-e89b-12d3-a456-426614174000
SCRUTINIZER_FAIL_ON_WARN=false
```

## Usage

### Normal Commit

```bash
git add -A
git commit -m "Update dependencies"
```

If dependency files changed, the hook will automatically evaluate them against the policy. The commit will succeed if the policy check passes.

### Skip the Hook

```bash
SCRUTINIZER_SKIP_HOOK=true git commit -m "Emergency commit"
```

Use this escape hatch only when necessary (e.g., emergency patches, testing).

### Override Configuration

```bash
SCRUTINIZER_POLICY_ID=xxx git commit -m "Use alternate policy"
```

Environment variables override the `.scrutinizer.env` file.

## SBOM Management

The hook expects an SBOM (Software Bill of Materials) file. You can:

### Option 1: Use Pre-Generated SBOM

Generate the SBOM once and commit it:

```bash
npm install -g @cyclonedx/cdxgen
cdxgen -o sbom.json
git add sbom.json
git commit -m "Add SBOM"
```

The hook will always use this committed SBOM.

### Option 2: Auto-Generate on Commit

If `cdxgen` is installed and no SBOM is found, the hook will attempt to generate one automatically. This adds latency to the first commit.

### Option 3: Skip When No SBOM

If no SBOM is found and `cdxgen` is not installed, the hook warns and exits gracefully (does not block the commit). This is useful during project setup.

## Output Examples

### Clean Commit (No Dependency Changes)

```
[Scrutinizer] Checking for dependency changes...
[Scrutinizer] No dependency changes detected (3ms)
```

### Policy Passes

```
[Scrutinizer] Checking for dependency changes...
[Scrutinizer] Detected change: package.json
[Scrutinizer] Sending SBOM to policy engine...
[Scrutinizer] Policy check PASSED (2150ms)
```

### Policy Fails

```
[Scrutinizer] Checking for dependency changes...
[Scrutinizer] Detected change: package.json
[Scrutinizer] Sending SBOM to policy engine...
[Scrutinizer] Policy check FAILED (2145ms)
[Scrutinizer] Found 3 issue(s) that violate the policy:
  - CRITICAL: Known vulnerability in log4j v2.14.0
  - HIGH: Unlicensed dependency: custom-lib v1.0.0
  - MEDIUM: Deprecated package: deprecated-util v0.1.0
[Scrutinizer] Commit blocked by policy engine
[Scrutinizer] Review findings above or contact your security team
```

### API Unreachable

```
[Scrutinizer] Checking for dependency changes...
[Scrutinizer] Detected change: package.json
[Scrutinizer] Sending SBOM to policy engine...
[Scrutinizer] Failed to connect to Scrutinizer API at http://localhost:8080
[Scrutinizer] Proceeding with commit (API unreachable)
```

The hook does not block commits when the API is unreachable, allowing developers to continue working while the Scrutinizer service is down.

## Integration with CI/CD

### GitHub Actions

Add to your workflow:

```yaml
- name: Scrutinizer Evaluation
  run: |
    curl -X POST \
      -H "Content-Type: application/json" \
      -d @sbom.json \
      "${{ secrets.SCRUTINIZER_API_URL }}/api/v1/runs" \
      -o report.json
```

### GitLab CI

Use the included CI template:

```yaml
include:
  - project: 'your-group/scrutinizer'
    ref: main
    file: '/scrutinizer-cli/ci-templates/.scrutinizer-ci.yml'

scrutinizer-scan:
  extends: .scrutinizer-scan
  variables:
    SCRUTINIZER_POLICY_ID: "550e8400-e29b-41d4-a716-446655440000"
```

## Exit Codes

The hook uses the following exit codes:

| Code | Meaning |
|------|---------|
| 0 | Success (commit allowed) |
| 1 | Policy violation (commit blocked) |
| 2 | Configuration error (required env var missing) |
| 3 | API/connection error (non-blocking, commit allowed) |

## Troubleshooting

### Hook Not Running

1. Verify the hook is executable:
   ```bash
   ls -la .git/hooks/pre-commit
   # Should show: -rwxr-xr-x
   ```

2. Check if hook is installed correctly:
   ```bash
   grep -l scrutinizer .git/hooks/pre-commit
   ```

3. Manually test the hook:
   ```bash
   bash .git/hooks/pre-commit
   ```

### "SCRUTINIZER_POLICY_ID is required"

The policy ID was not set in:
1. `.scrutinizer.env` file
2. Environment variables
3. Or another config source

Set it in `.scrutinizer.env`:

```bash
SCRUTINIZER_POLICY_ID=550e8400-e29b-41d4-a716-446655440000
```

### "Failed to connect to Scrutinizer API"

The hook could not reach the API URL. Check:

1. `SCRUTINIZER_API_URL` is correct and reachable
2. Network connectivity: `curl http://your-api-url/health`
3. API service is running and healthy

The hook will allow the commit to proceed if the API is unreachable.

### "SBOM file not found"

The hook expects an SBOM at the configured path. Either:

1. Generate and commit one: `cdxgen -o sbom.json`
2. Install `cdxgen` so the hook can auto-generate: `npm install -g @cyclonedx/cdxgen`
3. Change the path in `.scrutinizer.env`

### Performance Issues

The hook is optimized for commits without dependency changes (fast path takes <10ms). If dependency changes are detected:

- First run may be slower (SBOM generation + API call): ~3-5 seconds
- Subsequent runs with cached SBOM: ~2 seconds

To measure:

```bash
time git commit -m "test"
```

## Development

### Testing the Hook

```bash
# Generate a test SBOM
npm install -g @cyclonedx/cdxgen
cdxgen -o test-sbom.json

# Test with custom config
SCRUTINIZER_API_URL=http://localhost:8080 \
SCRUTINIZER_POLICY_ID=test-id \
SCRUTINIZER_SBOM_PATH=test-sbom.json \
bash scrutinizer-hook

# Test with curl directly
curl -X POST \
  -H "Content-Type: application/json" \
  -d @test-sbom.json \
  http://localhost:8080/api/v1/runs
```

### Hook Development

The hook is a bash script that:

1. Parses configuration from environment and `.scrutinizer.env`
2. Checks for staged dependency files
3. Locates or generates SBOM
4. Calls the Scrutinizer API
5. Parses response and sets exit code

Key files:

- `scrutinizer-hook`: Main hook script
- `install.sh`: Installation helper
- `.scrutinizer.env.template`: Configuration template

## License

See the main Scrutinizer project for license information.

## Support

For issues or questions:

1. Check the Troubleshooting section above
2. Review the hook output for error messages
3. Enable debugging: `bash -x .git/hooks/pre-commit`
4. Check logs in `.git/hooks/scrutinizer.log` (if configured)
