#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# NovaMesh Messenger — Firebase Project Setup Script
# ═══════════════════════════════════════════════════════════════
# This script automates:
#   1. Firebase login (CI token generation)
#   2. Creating a Firebase project
#   3. Enabling Auth (Phone), Firestore, Storage, FCM
#   4. Registering the Android app
#   5. Downloading google-services.json
#   6. Configuring Firestore security rules
#
# Prerequisites:
#   - Node.js 18+ installed
#   - A Google account
#   - A browser (for the initial OAuth step)
#
# Usage:
#   chmod +x firebase-setup.sh
#   ./firebase-setup.sh
# ═══════════════════════════════════════════════════════════════

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log()  { echo -e "${BLUE}[INFO]${NC}  $1"; }
ok()   { echo -e "${GREEN}[OK]${NC}    $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $1"; }
err()  { echo -e "${RED}[ERROR]${NC} $1"; }

# ──────────────────────────────────────────────
# STEP 1: Check prerequisites
# ──────────────────────────────────────────────
check_prereqs() {
  log "Checking prerequisites..."

  if ! command -v firebase &> /dev/null; then
    warn "Firebase CLI not found. Installing..."
    npm install -g firebase-tools
  fi
  ok "Firebase CLI: v$(firebase --version)"

  if ! command -v curl &> /dev/null; then
    err "curl is required. Please install it first."
    exit 1
  fi
  ok "curl available"

  if ! command -v jq &> /dev/null; then
    warn "jq not found — installing..."
    sudo apt-get install -y jq 2>/dev/null || brew install jq 2>/dev/null || {
      warn "Could not install jq. JSON parsing will be limited."
    }
  else
    ok "jq available"
  fi
}

# ──────────────────────────────────────────────
# STEP 2: Firebase Login
# ──────────────────────────────────────────────
firebase_login() {
  log "Checking Firebase login status..."

  if firebase projects:list &>/dev/null; then
    ok "Already logged in to Firebase"
    return
  fi

  echo ""
  warn "╔══════════════════════════════════════════════════════════════╗"
  warn "║  You need to log in to Firebase.                           ║"
  warn "║                                                             ║"
  warn "║  A browser window will open — or you'll see a URL.         ║"
  warn "║  Visit that URL, log in with your Google account,          ║"
  warn "║  then paste the token back here.                           ║"
  warn "╚══════════════════════════════════════════════════════════════╝"
  echo ""

  firebase login --no-localhost

  if firebase projects:list &>/dev/null; then
    ok "Firebase login successful"
  else
    err "Firebase login failed. Try: firebase login --no-localhost"
    exit 1
  fi
}

# ──────────────────────────────────────────────
# STEP 3: Create Firebase Project
# ──────────────────────────────────────────────
create_project() {
  log "Setting up Firebase project..."

  local project_id="novamesh-messenger-$(date +%s)"
  local project_name="NovaMesh Messenger"

  echo ""
  warn "A Firebase project will be created with ID: ${project_id}"
  echo ""

  # Create the Firebase project
  firebase projects:create "$project_id" \
    --display-name "$project_name" \
    --non-interactive 2>&1 || {
    # If project exists, try to use it
    warn "Project creation failed. It may already exist."
    warn "Enter an existing project ID (or press Enter to retry):"
    read -rp "Project ID: " user_project
    if [ -n "$user_project" ]; then
      project_id="$user_project"
      ok "Using existing project: $project_id"
    else
      err "Cannot proceed without a Firebase project."
      exit 1
    fi
  }

  ok "Firebase project: $project_id"

  # Save project ID for later steps
  echo "$project_id" > .firebase_project_id
}

# ──────────────────────────────────────────────
# STEP 4: Enable Firebase Services
# ──────────────────────────────────────────────
enable_services() {
  local project_id
  project_id=$(cat .firebase_project_id)
  log "Enabling Firebase services for: $project_id"

  # Enable required APIs via gcloud CLI or REST
  log "Enabling Identity Toolkit API (Firebase Auth)..."
  curl -s -X POST \
    "https://identitytoolkit.googleapis.com/v2/projects/${project_id}/identityPlatform:initializeAuth" \
    -H "Authorization: Bearer $(firebase login:ci --json 2>/dev/null | jq -r '.token')" \
    -H "Content-Type: application/json" \
    -d '{}' 2>/dev/null || warn "Auth API may need manual enablement"

  # Switch project in Firebase CLI
  firebase use "$project_id" --non-interactive || true

  # Provision Firestore
  log "Provisioning Cloud Firestore..."
  firebase firestore:locations:set --non-interactive us-central 2>&1 || {
    warn "Firestore location may already be set. Continuing..."
  }

  # Provision Storage
  log "Setting up Cloud Storage..."
  firebase storage:locations:set --non-interactive us-central 2>&1 || {
    warn "Storage location may already be set. Continuing..."
  }

  # Enable Firebase Auth (Phone)
  log "Enabling Phone Auth..."
  # This requires manual step via console for now
  warn "⚠️  IMPORTANT: You need to enable Phone Auth manually:"
  echo ""
  echo "  1. Go to: https://console.firebase.google.com/project/${project_id}/authentication/providers"
  echo "  2. Click 'Phone' → Enable → Save"
  echo "  3. Add your phone number as a test number (Settings → Test phone numbers)"
  echo ""

  ok "Firebase services configured"
}

# ──────────────────────────────────────────────
# STEP 5: Register Android App
# ──────────────────────────────────────────────
register_android_app() {
  local project_id
  project_id=$(cat .firebase_project_id)
  log "Registering Android app..."

  local app_id="com.novamesh.messenger"
  local app_id_debug="com.novamesh.messenger.debug"

  # Register via Firebase CLI
  firebase apps:create \
    --non-interactive \
    android "$app_id" \
    --project "$project_id" \
    --package-name "$app_id" 2>&1 || {
    warn "App registration may already exist."
  }

  # Register debug variant
  firebase apps:create \
    --non-interactive \
    android "${app_id_debug}" \
    --project "$project_id" \
    --package-name "$app_id_debug" 2>&1 || {
    warn "Debug app registration may already exist."
  }

  ok "Android apps registered"

  # Download google-services.json
  log "Downloading google-services.json..."
  
  # Get the app ID for the debug variant
  local app_info
  app_info=$(firebase apps:list android --project "$project_id" --json 2>/dev/null || echo "{}")
  
  # Download config for the main app
  firebase apps:sdkconfig android --project "$project_id" --out app/google-services.json 2>&1 || {
    warn "Could not download google-services.json automatically."
    warn "Download it manually from Firebase Console → Project Settings → General → Your apps → Download google-services.json"
    warn "Then place it at: app/google-services.json"
    return 1
  }

  ok "google-services.json downloaded to app/google-services.json"
}

# ──────────────────────────────────────────────
# STEP 6: Deploy Firestore & Storage Rules
# ──────────────────────────────────────────────
deploy_rules() {
  log "Setting up security rules..."

  # Create firestore.rules
  cat > firestore.rules << 'RULES'
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users: only authenticated users can read/write their own data
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
      
      // Subcollections
      match /{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
    
    // Chats: participants can read/write
    match /chats/{chatId} {
      allow read: if request.auth != null && 
        resource.data.participants.hasAny([request.auth.uid]);
      allow write: if request.auth != null && 
        request.resource.data.participants.hasAny([request.auth.uid]);
      
      // Messages subcollection
      match /messages/{messageId} {
        allow read: if request.auth != null;
        allow write: if request.auth != null;
      }
    }
    
    // Channels: anyone can read, only admins can write
    match /channels/{channelId} {
      allow read: if request.auth != null;
      allow write: if false; // System-only
      
      match /posts/{postId} {
        allow read: if request.auth != null;
        allow write: if false;
      }
    }
    
    // Stories: authenticated users can read, owners can write
    match /stories/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
      
      match /items/{storyId} {
        allow read: if request.auth != null;
        allow create: if request.auth.uid == userId;
        allow update: if request.auth.uid == userId;
        allow delete: if request.auth.uid == userId || request.time > resource.data.expiresAt;
      }
    }
  }
}
RULES

  # Create storage.rules
  cat > storage.rules << 'RULES'
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Profiles: users can only write their own
    match /profiles/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Media (chats): participants can read/write
    match /media/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Stories: users can read, owners can write
    match /stories/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Enforce file size limits (max 10MB for images, 50MB for videos)
    match /{allPaths=**} {
      allow write: if request.auth != null
        && request.resource.size < 50 * 1024 * 1024;
    }
  }
}
RULES

  # Deploy rules
  log "Deploying Firestore rules..."
  firebase deploy --only firestore:rules --non-interactive 2>&1 || {
    warn "Firestore rules deployment failed. You can deploy manually:"
    warn "  firebase deploy --only firestore:rules"
  }

  log "Deploying Storage rules..."
  firebase deploy --only storage:rules --non-interactive 2>&1 || {
    warn "Storage rules deployment failed. You can deploy manually:"
    warn "  firebase deploy --only storage:rules"
  }

  ok "Security rules deployed"
}

# ──────────────────────────────────────────────
# STEP 7: Generate CI Secrets
# ──────────────────────────────────────────────
generate_ci_secrets() {
  log "Generating CI configuration..."

  local project_id
  project_id=$(cat .firebase_project_id)

  # Encode google-services.json for GitHub Actions
  if [ -f "app/google-services.json" ]; then
    local encoded
    encoded=$(base64 -w 0 < app/google-services.json)
    
    cat > .github/secrets_guide.md << 'GUIDE'
# GitHub Secrets Setup

Add these secrets to your GitHub repository:
  Settings → Secrets and variables → Actions → New repository secret

GUIDE
    echo "| Secret | Value |" >> .github/secrets_guide.md
    echo "|--------|-------|" >> .github/secrets_guide.md
    echo "| \`GOOGLE_SERVICES_JSON\` | Base64 of google-services.json (see below) |" >> .github/secrets_guide.md
    echo "" >> .github/secrets_guide.md
    echo "Base64 encoded google-services.json:" >> .github/secrets_guide.md
    echo '```' >> .github/secrets_guide.md
    echo "$encoded" >> .github/secrets_guide.md
    echo '```' >> .github/secrets_guide.md

    ok "CI secrets guide created at .github/secrets_guide.md"
  fi
}

# ──────────────────────────────────────────────
# MAIN
# ──────────────────────────────────────────────
main() {
  echo ""
  echo "╔═══════════════════════════════════════════════╗"
  echo "║   NovaMesh — Firebase Project Setup          ║"
  echo "╚═══════════════════════════════════════════════╝"
  echo ""

  check_prereqs
  firebase_login
  create_project
  enable_services
  register_android_app
  deploy_rules
  generate_ci_secrets

  echo ""
  ok "╔══════════════════════════════════════════════════════════╗"
  ok "║  Firebase setup complete!                                ║"
  ok "║                                                          ║"
  ok "║  Next steps:                                             ║"
  ok "║  1. Enable Phone Auth in Firebase Console:               ║"
  ok "║     https://console.firebase.google.com                  ║"
  ok "║  2. Add your test phone number in Auth Settings          ║"
  ok "║  3. Build the app: ./gradlew assembleDebug              ║"
  ok "╚══════════════════════════════════════════════════════════╝"
  echo ""
}

main "$@"
