#!/bin/bash

# Test release script for dry-run testing
# Usage: ./scripts/test-release.sh [version|patch|minor|major]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[TEST SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[TEST WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[TEST ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "pom.xml" ] || [ ! -d "src" ]; then
    log_error "This script must be run from the root of the inqwise-walker project"
    exit 1
fi

log_info "Testing release process (dry run)..."

# Get current version
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | sed 's/-SNAPSHOT//')
log_info "Current version: $CURRENT_VERSION"

# Determine new version
VERSION_TYPE=${1:-patch}
if [[ "$VERSION_TYPE" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    NEW_VERSION="$VERSION_TYPE"
    log_info "Would use explicit version: $NEW_VERSION"
else
    case "$VERSION_TYPE" in
        "major")
            NEW_VERSION=$(echo $CURRENT_VERSION | awk -F. '{print ($1+1)".0.0"}')
            ;;
        "minor")
            NEW_VERSION=$(echo $CURRENT_VERSION | awk -F. '{print $1"."($2+1)".0"}')
            ;;
        "patch")
            NEW_VERSION=$(echo $CURRENT_VERSION | awk -F. '{print $1"."$2"."($3+1)}')
            ;;
        *)
            log_error "Invalid version type: $VERSION_TYPE"
            log_info "Usage: $0 [version|patch|minor|major]"
            exit 1
            ;;
    esac
    log_info "Would calculate new version: $NEW_VERSION (type: $VERSION_TYPE)"
fi

# Test Maven commands
log_info "Testing Maven compilation..."
mvn clean compile -q
log_success "Maven compile works"

log_info "Testing Maven tests..."
mvn test -q
log_success "Maven tests pass"

log_info "Testing Maven packaging..."
mvn package -q
log_success "Maven packaging works"

log_info "Testing version evaluation..."
EVAL_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
log_success "Version evaluation works: $EVAL_VERSION"

log_info "Testing Javadoc generation..."
mvn javadoc:jar -q
log_success "Javadoc generation works"

log_info "Testing source jar generation..."
mvn source:jar -q
log_success "Source jar generation works"

# Test git operations (read-only)
log_info "Testing git status..."
if [ -n "$(git status --porcelain)" ]; then
    log_warning "Working directory is not clean (this is just a test)"
else
    log_success "Working directory is clean"
fi

log_info "Testing branch detection..."
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
log_success "Current branch: $CURRENT_BRANCH"

log_info "Testing tag detection..."
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "No tags found")
log_success "Last tag: $LAST_TAG"

# Test README update pattern
log_info "Testing README version pattern matching..."
if grep -q "<version>[0-9]\+\.[0-9]\+\.[0-9]\+</version>" README.adoc; then
    log_success "README version pattern found"
else
    log_warning "README version pattern not found - manual update may be needed"
fi

echo
log_success "All release tests passed!"
log_info "The actual release script should work correctly"
echo
log_info "To run actual release:"
echo "  ./scripts/release.sh $VERSION_TYPE"