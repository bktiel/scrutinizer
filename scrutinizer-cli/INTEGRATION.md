# Scrutinizer CLI Integration Guide

This guide covers integrating the Scrutinizer pre-commit hook and CI template into your development workflow.

## Contents

1. [Developer Setup](#developer-setup)
2. [CI/CD Integration](#cicd-integration)
3. [Performance Measurement](#performance-measurement)
4. [Troubleshooting](#troubleshooting)

## Developer Setup

### For a Single Repository

```bash
cd /path/to/repo
/path/to/scrutinizer-cli/install.sh
```

This runs the setup wizard and creates `.scrutinizer.env`.

### For Multiple Repositories

Create a shared installation script:

```bash
#!/bin/bash
for repo in ~/projects/*; do
    if [ -d "$repo/.git" ]; then
        cd "$repo"
        /path/to/scrutinizer-cli/install.sh
        echo "✓ Installed Scrutinizer hook in $repo"
    fi
done
```

### Manual Setup for Enterprise

If you need to integrate with existing pre-commit frameworks (e.g., pre-commit.com):

```bash
# Using pre-commit framework (https://pre-commit.com/)
cat >> .pre-commit-config.yaml <<EOF
  - repo: /path/to/scrutinizer-cli
    rev: main
    hooks:
      - id: scrutinizer-check
        name: Scrutinizer Policy Check
        entry: ./scrutinizer-hook
        language: script
        stages: [commit]
        pass_filenames: false
EOF

pre-commit install
```

## CI/CD Integration

### GitLab CI

#### Option 1: Include the Template

```yaml
# .gitlab-ci.yml
include:
  - project: 'your-group/scrutinizer'
    ref: main
    file: '/scrutinizer-cli/ci-templates/.scrutinizer-ci.yml'

scrutinizer-evaluation:
  extends: .scrutinizer-scan
  variables:
    SCRUTINIZER_API_URL: "http://scrutinizer-api.internal:8080"
    SCRUTINIZER_POLICY_ID: "550e8400-e29b-41d4-a716-446655440000"
    SCRUTINIZER_PROJECT_ID: "123e4567-e89b-12d3-a456-426614174000"

stages:
  - test
  - deploy
```

#### Option 2: Direct CI Job

```yaml
scrutinizer-check:
  stage: test
  image: curlimages/curl:latest
  script:
    - |
      curl -X POST \
        -H "Content-Type: application/json" \
        -d @sbom.json \
        "${SCRUTINIZER_API_URL}/api/v1/runs" \
        -o report.json
      
      DECISION=$(jq -r '.decision' report.json)
      [ "$DECISION" = "FAIL" ] && exit 1 || exit 0
  artifacts:
    paths:
      - report.json
  allow_failure: true
```

### GitHub Actions

```yaml
# .github/workflows/scrutinizer.yml
name: Scrutinizer Policy Check

on:
  push:
    paths:
      - 'pom.xml'
      - 'package.json'
      - 'requirements.txt'
      - 'go.mod'
      - 'Cargo.toml'
      - 'build.gradle'
      - 'Gemfile.lock'

jobs:
  scrutinizer-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Generate SBOM
        run: |
          npm install -g @cyclonedx/cdxgen
          cdxgen -o sbom.json

      - name: Scrutinizer Policy Evaluation
        env:
          SCRUTINIZER_API_URL: ${{ secrets.SCRUTINIZER_API_URL }}
          SCRUTINIZER_POLICY_ID: ${{ secrets.SCRUTINIZER_POLICY_ID }}
          SCRUTINIZER_PROJECT_ID: ${{ secrets.SCRUTINIZER_PROJECT_ID }}
        run: |
          curl -X POST \
            -H "Content-Type: application/json" \
            -d @sbom.json \
            "${SCRUTINIZER_API_URL}/api/v1/runs" \
            -o report.json

          DECISION=$(jq -r '.decision' report.json)
          if [ "$DECISION" = "FAIL" ]; then
            echo "Policy check failed"
            jq '.findings' report.json
            exit 1
          fi

      - name: Upload Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: scrutinizer-report
          path: report.json
```

### Jenkins

```groovy
// Jenkinsfile
pipeline {
    agent any

    stages {
        stage('Scrutinizer Check') {
            when {
                changeset pattern: "pom.xml|package.json|requirements.txt|go.mod|Cargo.toml|build.gradle|Gemfile.lock"
            }
            steps {
                script {
                    // Generate SBOM
                    sh '''
                        npm install -g @cyclonedx/cdxgen
                        cdxgen -o sbom.json
                    '''

                    // Call Scrutinizer API
                    def response = sh(
                        script: '''
                            curl -s -X POST \
                              -H "Content-Type: application/json" \
                              -d @sbom.json \
                              "${SCRUTINIZER_API_URL}/api/v1/runs"
                        ''',
                        returnStdout: true
                    ).trim()

                    def decision = readJSON(text: response).decision

                    if (decision == 'FAIL') {
                        error('Scrutinizer policy check failed')
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'sbom.json,report.json', allowEmptyArchive: true
        }
    }
}
```

## Performance Measurement

### Benchmark the Hook

Run commits with and without dependency changes to measure overhead:

```bash
# Measure clean commit (no dependency changes)
time git commit --allow-empty -m "test: measure hook overhead"

# Typical output: ~50-200ms (mostly git internals)

# Measure commit with dependency changes
echo "test" >> package.json
time git commit -am "test: measure with dependency check"

# Typical output: ~2-3 seconds (SBOM parsing + API call)
```

### Performance Expectations

| Scenario | Time | Notes |
|----------|------|-------|
| Clean commit | <100ms | Early exit, zero API overhead |
| With dep changes, cached SBOM | ~2s | SBOM parse + API call |
| With dep changes, SBOM generation | ~4-5s | SBOM generation + API call |
| API unreachable | ~5s | Connection timeout, then proceeds |

### Optimize Performance

1. **Pre-generate and commit SBOM**:
   ```bash
   cdxgen -o sbom.json
   git add sbom.json
   ```
   This eliminates generation overhead on every commit.

2. **Use a local API mirror**:
   Configure `SCRUTINIZER_API_URL` to point to a local cache/mirror for faster responses.

3. **Increase API timeout** (if needed):
   Edit the curl timeout in `scrutinizer-hook` if your API is consistently slow.

## Troubleshooting

### Hook Not Executing

**Symptom**: Commits succeed without running the hook.

**Solutions**:

```bash
# Check if hook is executable
ls -la .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# Check if hook is properly installed
grep "scrutinizer" .git/hooks/pre-commit

# Manually test
bash -x .git/hooks/pre-commit
```

### API Connection Issues

**Symptom**: "Failed to connect to Scrutinizer API"

**Solutions**:

```bash
# Test API reachability
curl -v http://localhost:8080/health

# Check if API URL is correct
echo $SCRUTINIZER_API_URL

# Test with verbose curl
curl -v -X POST http://localhost:8080/api/v1/runs
```

### SBOM Generation Failures

**Symptom**: "Failed to generate SBOM with cdxgen"

**Solutions**:

```bash
# Install/update cdxgen
npm install -g @cyclonedx/cdxgen@latest

# Test cdxgen directly
cdxgen -o test-sbom.json /path/to/project

# Check for unsupported project structures
ls -la pom.xml package.json build.gradle requirements.txt go.mod Cargo.toml Gemfile.lock
```

### Policy ID Not Set

**Symptom**: "SCRUTINIZER_POLICY_ID is required but not set"

**Solutions**:

```bash
# Set in environment
export SCRUTINIZER_POLICY_ID="550e8400-e29b-41d4-a716-446655440000"
git commit

# Or set in .scrutinizer.env
echo 'SCRUTINIZER_POLICY_ID=550e8400-e29b-41d4-a716-446655440000' >> .scrutinizer.env

# Verify it's set
grep SCRUTINIZER_POLICY_ID .scrutinizer.env
```

### False Positives / Unexpected Failures

**Symptom**: Hook blocks commits that should pass.

**Solutions**:

1. Review the findings printed by the hook
2. Check if policy is too restrictive
3. Test with a different policy: `SCRUTINIZER_POLICY_ID=xxx git commit`
4. Use `SCRUTINIZER_SKIP_HOOK=true git commit` as a temporary workaround
5. Contact your policy administrator

## Experiment 4: Measuring CLI Overhead

This tool is designed for Experiment 4 (Developer Workflow Impact). Key metrics to measure:

### Metrics

1. **Cold Start Latency** (first commit with dependency changes):
   - SBOM generation: ~2-3s
   - API call: ~1-2s
   - Total: ~3-5s

2. **Warm Latency** (subsequent commits, cached SBOM):
   - API call only: ~2-3s

3. **Zero-Overhead Fast Path** (no dependency changes):
   - Early exit: <50ms

4. **Developer Perception**:
   - Clean commits: unnoticeable
   - Dependency updates: acceptable (similar to running tests)

### Collection

Use the timing information printed by the hook:

```bash
# Example output shows elapsed time:
[Scrutinizer] Policy check PASSED (2150ms)
```

Parse logs to collect statistics:

```bash
# Extract timing from git log
git log --oneline | while read hash msg; do
    git show "$hash" 2>/dev/null | grep "passed\|failed" | grep -oP '\(\d+ms\)'
done | sort | uniq -c
```

## Additional Resources

- [README.md](./README.md) - User guide and configuration reference
- [.scrutinizer.env.template](./.scrutinizer.env.template) - Configuration template
- [Scrutinizer API Documentation](../scrutinizer-api/README.md) - API endpoint details
