#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENGINE_DIR="$PROJECT_ROOT/scrutinizer-engine"
OUTPUT_BASE="$SCRIPT_DIR/results"

echo "=== Scrutinizer Validation Suite ==="
echo ""

if ! command -v java &>/dev/null; then
    echo "ERROR: Java 17+ is required but not found on PATH."
    exit 1
fi

JAR="$ENGINE_DIR/build/libs/scrutinizer.jar"
if [ ! -f "$JAR" ]; then
    echo "Building scrutinizer..."
    cd "$ENGINE_DIR"
    ./gradlew bootJar --no-daemon -x test
    cd "$PROJECT_ROOT"
fi

SBOMS_DIR="$PROJECT_ROOT/reference-env/sboms"
POLICIES_DIR="$PROJECT_ROOT/reference-env/policies"

declare -A SBOM_FILES=(
    ["frontend"]="$SBOMS_DIR/ref-app-1-frontend.cdx.json"
    ["backend"]="$SBOMS_DIR/ref-app-1-backend.cdx.json"
)

declare -A POLICY_FILES=(
    ["permissive"]="$POLICIES_DIR/permissive.yaml"
    ["strict"]="$POLICIES_DIR/strict.yaml"
    ["weighted"]="$POLICIES_DIR/weighted.yaml"
)

PASS_COUNT=0
FAIL_COUNT=0

run_validation() {
    local sbom_name=$1
    local policy_name=$2
    local sbom_path=$3
    local policy_path=$4

    local run_dir="$OUTPUT_BASE/${sbom_name}_${policy_name}"
    mkdir -p "$run_dir"

    echo "--- Validating: $sbom_name with $policy_name policy ---"

    local exit_code=0
    java -jar "$JAR" \
        --sbom "$sbom_path" \
        --policy "$policy_path" \
        --output-dir "$run_dir" \
        --format json \
        --no-file \
        2>&1 | head -50 || exit_code=$?

    if [ -f "$run_dir/posture-report.json" ] && [ -f "$run_dir/inventory.csv" ] && [ -f "$run_dir/evidence-manifest.json" ]; then
        echo "  PASS: Evidence bundle complete (exit code: $exit_code)"
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        echo "  FAIL: Missing evidence bundle artifacts"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
    echo ""
}

rm -rf "$OUTPUT_BASE"
mkdir -p "$OUTPUT_BASE"

for sbom_name in "${!SBOM_FILES[@]}"; do
    for policy_name in "${!POLICY_FILES[@]}"; do
        run_validation "$sbom_name" "$policy_name" "${SBOM_FILES[$sbom_name]}" "${POLICY_FILES[$policy_name]}"
    done
done

echo "=== Validation Summary ==="
echo "Passed: $PASS_COUNT"
echo "Failed: $FAIL_COUNT"

if [ "$FAIL_COUNT" -gt 0 ]; then
    echo "VALIDATION FAILED"
    exit 1
fi
echo "ALL VALIDATIONS PASSED"
