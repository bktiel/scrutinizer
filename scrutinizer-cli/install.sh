#!/usr/bin/env bash

#
# Scrutinizer Pre-Commit Hook Installation Script
# Sets up the git hook in your repository
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GIT_HOOKS_DIR=".git/hooks"
PRE_COMMIT_HOOK="${GIT_HOOKS_DIR}/pre-commit"
CONFIG_FILE=".scrutinizer.env"
TEMPLATE_FILE="${SCRIPT_DIR}/.scrutinizer.env.template"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[Scrutinizer]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[Scrutinizer]${NC} $*"
}

log_warn() {
    echo -e "${YELLOW}[Scrutinizer]${NC} $*"
}

# Check if we're in a git repository
if [[ ! -d ".git" ]]; then
    echo "Error: Not a git repository. Run this script from the root of your git repository."
    exit 1
fi

log_info "Installing Scrutinizer pre-commit hook..."

# Create hooks directory if it doesn't exist
mkdir -p "${GIT_HOOKS_DIR}"

# Check if pre-commit hook already exists
if [[ -f "${PRE_COMMIT_HOOK}" ]]; then
    log_warn "Pre-commit hook already exists at ${PRE_COMMIT_HOOK}"

    # Check if Scrutinizer hook is already installed
    if grep -q "scrutinizer-hook" "${PRE_COMMIT_HOOK}"; then
        log_info "Scrutinizer hook is already installed"
    else
        log_info "Appending Scrutinizer hook to existing pre-commit hook..."

        # Create a wrapper to call both hooks
        TEMP_HOOK=$(mktemp)
        {
            cat "${PRE_COMMIT_HOOK}"
            echo ""
            echo "# Call Scrutinizer pre-commit hook"
            echo "if [[ -f \"${SCRIPT_DIR}/scrutinizer-hook\" ]]; then"
            echo "    \"${SCRIPT_DIR}/scrutinizer-hook\""
            echo "    EXIT_CODE=\$?"
            echo "    if [[ \$EXIT_CODE -eq 1 ]]; then"
            echo "        exit 1"
            echo "    fi"
            echo "fi"
        } > "${TEMP_HOOK}"

        mv "${TEMP_HOOK}" "${PRE_COMMIT_HOOK}"
        chmod +x "${PRE_COMMIT_HOOK}"
        log_success "Scrutinizer hook appended to existing pre-commit hook"
    fi
else
    log_info "Creating new pre-commit hook..."

    cat > "${PRE_COMMIT_HOOK}" <<'HOOK_CONTENT'
#!/usr/bin/env bash

# Scrutinizer Pre-Commit Hook Wrapper
# This hook can be extended to call additional checks

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Call Scrutinizer hook
if [[ -f "${SCRIPT_DIR}/../../../scrutinizer-cli/scrutinizer-hook" ]]; then
    "${SCRIPT_DIR}/../../../scrutinizer-cli/scrutinizer-hook"
    EXIT_CODE=$?
    if [[ $EXIT_CODE -eq 1 ]]; then
        exit 1
    elif [[ $EXIT_CODE -eq 2 ]]; then
        exit 2
    fi
fi

exit 0
HOOK_CONTENT

    chmod +x "${PRE_COMMIT_HOOK}"
    log_success "Pre-commit hook created at ${PRE_COMMIT_HOOK}"
fi

# Create configuration file from template if it doesn't exist
if [[ ! -f "${CONFIG_FILE}" ]]; then
    log_info "Creating configuration file from template..."

    if [[ -f "${TEMPLATE_FILE}" ]]; then
        cp "${TEMPLATE_FILE}" "${CONFIG_FILE}"
        log_success "Configuration template created at ${CONFIG_FILE}"
        log_warn "Please edit ${CONFIG_FILE} to set SCRUTINIZER_API_URL and SCRUTINIZER_POLICY_ID"
    else
        log_warn "Template file not found at ${TEMPLATE_FILE}"
        log_info "Creating minimal configuration file..."

        cat > "${CONFIG_FILE}" <<'CONFIG_CONTENT'
# Scrutinizer Pre-Commit Hook Configuration
# Required: Set these before using the hook

SCRUTINIZER_API_URL=http://localhost:8080
SCRUTINIZER_POLICY_ID=

# Optional configuration:
# SCRUTINIZER_PROJECT_ID=
# SCRUTINIZER_APP_NAME=
# SCRUTINIZER_SBOM_PATH=./sbom.json
# SCRUTINIZER_FAIL_ON_WARN=false
# SCRUTINIZER_SKIP_HOOK=false
CONFIG_CONTENT

        log_success "Minimal configuration file created at ${CONFIG_FILE}"
        log_warn "Please edit ${CONFIG_FILE} to set required values"
    fi
else
    log_info "Configuration file already exists at ${CONFIG_FILE}"
fi

# Print setup instructions
echo ""
echo -e "${GREEN}Setup Complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. Edit ${CONFIG_FILE} and set:"
echo "     - SCRUTINIZER_API_URL (required)"
echo "     - SCRUTINIZER_POLICY_ID (required)"
echo "     - Other optional settings as needed"
echo ""
echo "  2. Generate an SBOM for your project (if not already done):"
echo "     - npm install -g @cyclonedx/cdxgen"
echo "     - cdxgen -o sbom.json"
echo ""
echo "  3. The hook will run automatically on 'git commit'"
echo "     To skip it in a commit: SCRUTINIZER_SKIP_HOOK=true git commit"
echo ""
echo "For more information, see the README.md in the scrutinizer-cli directory"
