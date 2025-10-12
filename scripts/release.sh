#!/bin/bash

# inqwise-walker Release Script
# Usage: ./scripts/release.sh [version|patch|minor|major]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "pom.xml" ] || [ ! -d "src" ]; then
    log_error "This script must be run from the root of the inqwise-walker project"
    exit 1
fi

# Check if git is clean
if [ -n "$(git status --porcelain)" ]; then
    log_error "Working directory is not clean. Please commit or stash your changes first."
    exit 1
fi

# Check if we're on main branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "main" ]; then
    log_warning "You are not on the main branch (current: $CURRENT_BRANCH)"
    read -p "Do you want to continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Release cancelled"
        exit 1
    fi
fi

# Get current version
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | sed 's/-SNAPSHOT//')
log_info "Current version: $CURRENT_VERSION"

# Determine new version
VERSION_TYPE=${1:-patch}
if [[ "$VERSION_TYPE" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    NEW_VERSION="$VERSION_TYPE"
    log_info "Using explicit version: $NEW_VERSION"
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
    log_info "Calculated new version: $NEW_VERSION (type: $VERSION_TYPE)"
fi

# Confirm release
echo
log_warning "This will create a new release:"
echo "  Current version: $CURRENT_VERSION"
echo "  New version: $NEW_VERSION"
echo
read -p "Do you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    log_info "Release cancelled"
    exit 1
fi

log_info "Starting release process..."

# Pull latest changes
log_info "Pulling latest changes..."
git pull origin main

# Run tests
log_info "Running tests..."
mvn clean test -B
if [ $? -ne 0 ]; then
    log_error "Tests failed. Aborting release."
    exit 1
fi
log_success "Tests passed"

# Update version in pom.xml
log_info "Updating version in pom.xml..."
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false

# Update README version
log_info "Updating README version..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s/<version>[0-9]\+\.[0-9]\+\.[0-9]\+<\/version>/<version>$NEW_VERSION<\/version>/g" README.adoc
else
    # Linux
    sed -i "s/<version>[0-9]\+\.[0-9]\+\.[0-9]\+<\/version>/<version>$NEW_VERSION<\/version>/g" README.adoc
fi

# Build project
log_info "Building project..."
mvn clean compile package -B
if [ $? -ne 0 ]; then
    log_error "Build failed. Aborting release."
    exit 1
fi
log_success "Build successful"

# Generate artifacts
log_info "Generating Javadocs and sources..."
mvn javadoc:jar source:jar -B

# Generate changelog
log_info "Generating changelog..."
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

echo "# Release Notes for v$NEW_VERSION" > RELEASE_NOTES.md
echo "" >> RELEASE_NOTES.md
echo "## Changes" >> RELEASE_NOTES.md

if [ -z "$LAST_TAG" ]; then
    echo "- Initial release of inqwise-walker" >> RELEASE_NOTES.md
    echo "- Event-driven object traversal framework" >> RELEASE_NOTES.md
    echo "- Support for JSON Objects and Arrays" >> RELEASE_NOTES.md
    echo "- Extensible walker architecture" >> RELEASE_NOTES.md
    echo "- Flow control (pause/resume/terminate)" >> RELEASE_NOTES.md
    echo "- Path tracking and context data sharing" >> RELEASE_NOTES.md
else
    # Generate changelog from commits
    git log ${LAST_TAG}..HEAD --pretty=format:"- %s" --grep="^feat:" --grep="^fix:" --grep="^perf:" --grep="^refactor:" -E >> RELEASE_NOTES.md
fi

echo "" >> RELEASE_NOTES.md
echo "## Installation" >> RELEASE_NOTES.md
echo "" >> RELEASE_NOTES.md
echo '```xml' >> RELEASE_NOTES.md
echo "<dependency>" >> RELEASE_NOTES.md
echo "    <groupId>com.inqwise</groupId>" >> RELEASE_NOTES.md
echo "    <artifactId>inqwise-walker</artifactId>" >> RELEASE_NOTES.md
echo "    <version>$NEW_VERSION</version>" >> RELEASE_NOTES.md
echo "</dependency>" >> RELEASE_NOTES.md
echo '```' >> RELEASE_NOTES.md

log_success "Generated release notes"

# Commit and tag
log_info "Committing version changes..."
git add pom.xml README.adoc RELEASE_NOTES.md
git commit -m "chore: bump version to $NEW_VERSION"

log_info "Creating git tag..."
git tag -a "v$NEW_VERSION" -m "Release v$NEW_VERSION"

# Ask about pushing
echo
log_warning "Release prepared locally. The following will be pushed:"
echo "  - Version bump commit"
echo "  - Git tag: v$NEW_VERSION"
echo
read -p "Push to GitHub now? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    log_info "Pushing to GitHub..."
    git push origin main
    git push origin --tags
    log_success "Pushed to GitHub"
    
    echo
    log_success "Release v$NEW_VERSION completed!"
    log_info "GitHub Actions will now create the GitHub release automatically"
    log_info "Check: https://github.com/inqwise/inqwise-walker/releases"
else
    log_info "Not pushed to GitHub. You can push manually later with:"
    echo "  git push origin main"
    echo "  git push origin --tags"
fi

echo
log_success "Release process completed locally!"
echo "Release notes saved to: RELEASE_NOTES.md"