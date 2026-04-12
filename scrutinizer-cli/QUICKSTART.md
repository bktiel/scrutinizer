# Quick Start: Scrutinizer Pre-Commit Hook

Get the hook running in 5 minutes.

## Installation

```bash
# 1. Run the installer
cd /your/project
/path/to/scrutinizer-cli/install.sh

# 2. Edit configuration
nano .scrutinizer.env

# 3. Generate SBOM (one-time)
npm install -g @cyclonedx/cdxgen
cdxgen -o sbom.json
git add sbom.json
```

## Configuration

Edit `.scrutinizer.env`:

```bash
SCRUTINIZER_API_URL=http://localhost:8080
SCRUTINIZER_POLICY_ID=your-policy-uuid-here
```

That's it! Required fields only.

## Test It

```bash
# Modify a dependency file
echo "test" >> package.json

# Commit
git commit -am "test: verify hook is working"

# You should see:
# [Scrutinizer] Checking for dependency changes...
# [Scrutinizer] Detected change: package.json
# [Scrutinizer] Sending SBOM to policy engine...
# [Scrutinizer] Policy check PASSED (2150ms)
```

## Troubleshooting

### "SCRUTINIZER_POLICY_ID is required"

```bash
# Get your policy ID from the dashboard, then:
echo 'SCRUTINIZER_POLICY_ID=550e8400-e29b-41d4-a716-446655440000' >> .scrutinizer.env
```

### "SBOM file not found"

```bash
npm install -g @cyclonedx/cdxgen
cdxgen -o sbom.json
git add sbom.json
```

### "Failed to connect to Scrutinizer API"

```bash
# Check the API URL:
curl http://your-api-url:8080/health

# Update in .scrutinizer.env if needed
SCRUTINIZER_API_URL=http://correct-url:8080
```

### Hook not running at all

```bash
# Check if it's installed:
cat .git/hooks/pre-commit | grep scrutinizer

# If not found, re-run installer:
/path/to/scrutinizer-cli/install.sh
```

## Skip the Hook

Emergency commit? Skip the check:

```bash
SCRUTINIZER_SKIP_HOOK=true git commit -m "emergency fix"
```

## Next Steps

- Review [README.md](./README.md) for full documentation
- Check [INTEGRATION.md](./INTEGRATION.md) for CI/CD setup
- Read the API docs in `../scrutinizer-api/`

## Key Files

```
scrutinizer-cli/
├── scrutinizer-hook           # Main hook script
├── install.sh                 # Installation script
├── .scrutinizer.env.template  # Config template
├── README.md                  # Full documentation
├── QUICKSTART.md             # This file
├── INTEGRATION.md            # CI/CD integration guide
└── ci-templates/
    └── .scrutinizer-ci.yml   # GitLab CI template
```

## Performance

- **Clean commits** (no dependency changes): <100ms overhead
- **Dependency changes**: ~2-3 seconds (API evaluation)
- **First run with SBOM generation**: ~4-5 seconds

Typical developer experience: commits are fast and dependency checks are transparent.
