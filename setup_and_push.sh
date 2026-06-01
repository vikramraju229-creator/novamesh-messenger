#!/bin/bash
#
# setup_and_push.sh — NovaMesh Messenger
# Initialises a Git repository and pushes to GitHub.
# Optimised for low-RAM systems (under 2GB).
#
# Usage:
#   chmod +x setup_and_push.sh
#   ./setup_and_push.sh
#
# Prerequisites:
#   - GitHub account with PAT or SSH key configured
#   - Git installed
#   - (Optional) GitHub CLI (gh) installed and authenticated
#
# ─────────────────────────────────────────────────────
set -euo pipefail

# ╔═════════════════════════════════════════════════════╗
# ║  CONFIG — Edit these before running                ║
# ╚═════════════════════════════════════════════════════╝

# GitHub username (from existing git config or set manually)
GITHUB_USERNAME="${GITHUB_USERNAME:-$(git config --global user.name || echo 'YOUR_GITHUB_USERNAME')}"

# Repository name (must match the project folder name)
REPO_NAME="novamesh-messenger"

# Repository description
REPO_DESC="NovaMesh: WhatsApp + Snapchat hybrid Android messenger with E2EE, Signal Protocol, Material You"

# GitHub visibility: "public" or "private"
REPO_VISIBILITY="public"

# Default branch
BRANCH="main"

# Commit message
COMMIT_MSG="Initial commit: NovaMesh Messenger — E2EE Android messenger with Matrix protocol"

# Project root (auto-detected as script location)
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ╔═════════════════════════════════════════════════════╗
# ║  COLOURS & HELPERS                                 ║
# ╚═════════════════════════════════════════════════════╝

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Colour

log_info()  { echo -e "${BLUE}[INFO]${NC}  $1"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_command() {
  if ! command -v "$1" &> /dev/null; then
    log_error "$1 is not installed. Please install it first."
    exit 1
  fi
}

# ╔═════════════════════════════════════════════════════╗
# ║  STEP 0: System Check (Low-RAM Safe)              ║
# ╚═════════════════════════════════════════════════════╝

low_ram_setup() {
  log_info "Configuring Git for low-RAM system..."

  # Disable automatic garbage collection (avoids OOM)
  git config --global gc.auto 0

  # Limit pack memory usage (critical for low RAM)
  git config --global pack.windowMemory "100m"
  git config --global pack.packSizeLimit "100m"
  git config --global pack.threads "1"
  git config --global core.packedGitLimit "100m"
  git config --global core.packedGitWindowSize "100m"

  # Disable compression to reduce RAM spikes
  git config --global core.compression 0

  # Increase HTTP buffer for large repos
  git config --global http.postBuffer 524288000

  # Use --no-pager for all git commands (no interactive pager)
  export GIT_PAGER=cat

  log_ok "Low-RAM Git configuration applied"
}

check_ram() {
  local total_ram_mb
  total_ram_mb=$(free -m | awk '/^Mem:/{print $2}')
  log_info "Total system RAM: ${total_ram_mb}MB"

  if [ "$total_ram_mb" -lt 2048 ]; then
    log_warn "Less than 2GB RAM detected — operations may be slow"
    low_ram_setup
  else
    log_ok "Sufficient RAM detected"
    low_ram_setup  # Apply settings anyway for safety
  fi
}

# ╔═════════════════════════════════════════════════════╗
# ║  STEP 1: Prerequisites                            ║
# ╚═════════════════════════════════════════════════════╝

check_prerequisites() {
  log_info "Checking prerequisites..."

  check_command git
  log_ok "Git $(git --version | awk '{print $3}')"

  # Check for GitHub CLI (optional)
  if command -v gh &> /dev/null; then
    log_info "GitHub CLI (gh) detected: $(gh --version | head -1)"
  else
    log_warn "GitHub CLI (gh) not installed — will use git commands directly"
  fi
}

# ╔═════════════════════════════════════════════════════╗
# ║  STEP 2: Initialise Git Repository                ║
# ╚═════════════════════════════════════════════════════╝

init_repo() {
  log_info "Initialising Git repository..."

  cd "$PROJECT_DIR"

  # Check if already a git repo
  if [ -d ".git" ]; then
    log_warn "Git repository already exists at $PROJECT_DIR"
    read -rp "Re-initialise? This will DELETE the existing .git folder [y/N]: " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
      rm -rf ".git"
      log_info "Removed existing .git"
    else
      log_info "Using existing repository"
      return
    fi
  fi

  git init
  git checkout -b "$BRANCH" 2>/dev/null || true
  log_ok "Repository initialised on branch '$BRANCH'"
}

# ╔═════════════════════════════════════════════════════╗
# ║  STEP 3: Stage & Commit in Chunks (Low-RAM Safe)  ║
# ╚═════════════════════════════════════════════════════╝

stage_and_commit() {
  log_info "Staging files in low-RAM mode (chunked)..."

  # Ensure .gitignore exists
  if [ ! -f ".gitignore" ]; then
    log_warn "No .gitignore found — creating default Android .gitignore"
    cat > .gitignore << 'GITIGNORE'
# Build artifacts
build/
.gradle/
*.jks
*.keystore
*.apk
*.aab
*.ap_
*.dex
*.class
*.log

# IDE
.idea/
*.iml
.DS_Store
local.properties

# Environment & Secrets
.env
secrets.properties
google-services.json

# Temp
*.tmp
*~
GITIGNORE
    log_ok "Created .gitignore"
  fi

  # Stage in logical chunks to keep working set small
  log_info "Staging build configuration..."
  git add build.gradle.kts settings.gradle.kts gradle.properties gradlew 2>/dev/null || true
  git add gradle/ 2>/dev/null || true

  log_info "Staging app source code..."
  git add app/src/main/kotlin/ 2>/dev/null || true

  log_info "Staging app resources..."
  git add app/src/main/res/ 2>/dev/null || true

  log_info "Staging AndroidManifest & config..."
  git add app/src/main/AndroidManifest.xml 2>/dev/null || true
  git add app/build.gradle.kts app/proguard-rules.pro 2>/dev/null || true

  log_info "Staging documentation & root files..."
  git add README.md .gitignore 2>/dev/null || true
  git add LICENSE 2>/dev/null || true
  git add .github/ 2>/dev/null || true

  # Safety: don't stage secrets
  if git ls-files --error-unmatch google-services.json &>/dev/null; then
    git rm --cached google-services.json 2>/dev/null || true
    log_warn "Removed google-services.json from staging (should be .gitignored)"
  fi

  # Show what's staged
  local staged_count
  staged_count=$(git diff --cached --name-only | wc -l)
  log_info "Staged $staged_count files"

  # Commit
  log_info "Committing..."
  git commit -m "$COMMIT_MSG" --no-verify --quiet 2>/dev/null || {
    # If nothing staged, try adding everything except what's gitignored
    log_warn "Nothing staged — falling back to bulk add"
    git add --all
    staged_count=$(git diff --cached --name-only | wc -l)
    if [ "$staged_count" -eq 0 ]; then
      log_error "Nothing to commit. Are there files in $PROJECT_DIR?"
      exit 1
    fi
    git commit -m "$COMMIT_MSG" --no-verify --quiet
  }

  log_ok "Committed: $COMMIT_MSG"
}

# ╔═════════════════════════════════════════════════════╗
# ║  STEP 4: Create GitHub Repo & Push                ║
# ╚═════════════════════════════════════════════════════╝

push_to_github() {
  log_info "Creating GitHub repository and pushing..."

  local remote_url=""

  # Strategy A: Use GitHub CLI (gh)
  if command -v gh &> /dev/null && gh auth status 2>&1 &> /dev/null; then
    log_info "Using GitHub CLI..."

    # Create repo and push in one command
    gh repo create "$REPO_NAME" \
      --"$REPO_VISIBILITY" \
      --description "$REPO_DESC" \
      --push \
      --source=. \
      --remote=origin 2>&1 && {
      log_ok "Repository created and pushed via gh CLI"
      return
    }

    log_warn "gh CLI failed — falling back to manual git push"
  fi

  # Strategy B: Manual git push (HTTPS with PAT or SSH)
  log_info "Using manual git push..."

  # Determine remote URL
  if [ -f "$HOME/.ssh/id_ed25519" ] || [ -f "$HOME/.ssh/id_rsa" ]; then
    # SSH available
    remote_url="git@github.com:${GITHUB_USERNAME}/${REPO_NAME}.git"
    log_info "Remote (SSH): $remote_url"
  else
    # HTTPS (will use credential helper or prompt for token)
    remote_url="https://github.com/${GITHUB_USERNAME}/${REPO_NAME}.git"
    log_info "Remote (HTTPS): $remote_url"
  fi

  # Create repo via GitHub API (must have credentials)
  log_info "Creating repository on GitHub via API..."

  local api_status
  api_status=$(curl -s -o /dev/null -w "%{http_code}" \
    -u "$GITHUB_USERNAME" \
    -X POST \
    -H "Accept: application/vnd.github.v3+json" \
    "https://api.github.com/user/repos" \
    -d "{\"name\":\"$REPO_NAME\",\"description\":\"$REPO_DESC\",\"private\":false,\"auto_init\":false}" 2>&1 || echo "000")

  if [ "$api_status" = "201" ]; then
    log_ok "GitHub repository created"
  elif [ "$api_status" = "422" ]; then
    log_warn "Repository already exists on GitHub"
  else
    log_error "Failed to create repository (HTTP $api_status)"
    log_error "Ensure your GitHub credentials are configured:"
    log_error "  git config --global credential.helper store"
    log_error "  echo 'https://USERNAME:TOKEN@github.com' > ~/.git-credentials"
    log_error ""
    log_error "Or set up SSH:"
    log_error "  ssh-keygen -t ed25519 -C 'your_email@example.com'"
    log_error "  cat ~/.ssh/id_ed25519.pub  # Add to GitHub Settings → SSH Keys"
    exit 1
  fi

  # Add remote
  if git remote get-url origin &>/dev/null; then
    git remote set-url origin "$remote_url"
  else
    git remote add origin "$remote_url"
  fi

  # Push (with low-RAM safety)
  log_info "Pushing to GitHub..."
  GIT_PAGER=cat git push -u origin "$BRANCH" --no-verify --verbose 2>&1 || {
    log_error "Push failed. Check your internet connection and GitHub credentials."
    exit 1
  }

  log_ok "Successfully pushed to: https://github.com/${GITHUB_USERNAME}/${REPO_NAME}"
}

# ╔═════════════════════════════════════════════════════╗
# ║  MAIN                                             ║
# ╚═════════════════════════════════════════════════════╝

main() {
  echo ""
  echo "╔═══════════════════════════════════════════════╗"
  echo "║   NovaMesh Messenger — Git Setup & Push      ║"
  echo "╚═══════════════════════════════════════════════╝"
  echo ""

  check_ram
  check_prerequisites
  init_repo
  stage_and_commit
  push_to_github

  echo ""
  log_ok "🎉 All done! Repository is live at:"
  echo "   https://github.com/${GITHUB_USERNAME}/${REPO_NAME}"
  echo ""
}

main "$@"
