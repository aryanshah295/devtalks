# SETUP.md

Fresh-machine setup for DevTalks. Targets macOS with Homebrew (the project's actual dev environment); Linux notes inline where the steps differ. Everything below is **per-developer** — the GCP project, Firestore database, Artifact Registry repo, GCS tfstate bucket, and Cloud Build triggers are already provisioned for the shared project `devtalks-aryan-4787` and should NOT be recreated.

If a command below is already installed, the install step is a no-op. None of these steps need root.

---

## 1. System tools

Confirm or install:

| Tool | Why | macOS install | Linux install |
|---|---|---|---|
| Git | source control | preinstalled | distro package manager |
| Homebrew | the install vector for everything else | https://brew.sh | (Linuxbrew or skip) |
| curl | smoke tests in this doc | preinstalled | distro package manager |
| jq | JSON pretty-print in verification | `brew install jq` | distro package manager |

---

## 2. Language toolchains

### JDK 21 (Temurin) via SDKMAN

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.5-tem
sdk default java 21.0.5-tem
java -version    # → openjdk version "21.0.5"
```

Alternatives: `asdf install java temurin-21.0.5+11`, `mise install java@21`, or `brew install --cask temurin@21`. Whatever you pick, **the JVM must be 21** because Gradle's `jvmToolchain(21)` enforces it.

### Node 20 + pnpm via Corepack

```bash
curl -fsSL https://github.com/Schniz/fnm/raw/master/.ci/install.sh | bash    # one-time install of fnm
fnm install 20
fnm default 20
corepack enable                                                              # ships with Node
node -v          # → v20.x
pnpm --version   # → 10.x (pinned in frontend/package.json `packageManager`)
```

Alternatives: `nvm install 20`, `brew install node@20`, `asdf install nodejs 20.20.2`.

### Gradle (one-time, only to bootstrap the wrapper)

The repo ships a pinned Gradle wrapper at version 8.7. You only need a global `gradle` if you want to regenerate that wrapper. Otherwise everything goes through `./gradlew`.

```bash
brew install gradle    # macOS
# or: sdk install gradle 8.7
gradle --version       # 8.7+ — exact version doesn't matter
```

---

## 3. CLIs

### gcloud (Google Cloud SDK)

```bash
brew install --cask google-cloud-sdk
gcloud --version
gcloud components install beta firestore --quiet
```

Linux: follow https://cloud.google.com/sdk/docs/install#deb.

### Firebase CLI

```bash
npm install -g firebase-tools           # corepack/Node 20 makes this work without sudo
firebase --version                      # ≥ 13.x
```

### GitHub CLI (`gh`)

```bash
brew install gh
gh --version
```

### Buf (`proto/` codegen)

```bash
brew install bufbuild/buf/buf
buf --version    # 1.41+ — older may break on `clean: true` and other v2 fields
```

### Terraform

```bash
brew install terraform
terraform -version    # ≥ 1.6
```

### Docker (optional — only for local container experiments)

```bash
brew install --cask docker        # macOS — open Docker.app once to start the daemon
```

Jib is daemon-less for the canonical build (`./gradlew :services:api-gateway:jib`), so Docker isn't required for normal development or CI.

---

## 4. Authentication

All of these are interactive — they pop a browser window. Run each once per machine.

### gcloud (user credentials + Application Default Credentials)

```bash
gcloud auth login                                                # interactive web flow
gcloud auth application-default login                            # ADC for Java SDKs (Firestore, Firebase Admin)
gcloud auth configure-docker us-central1-docker.pkg.dev          # lets Docker/Jib push to Artifact Registry
gcloud config set project devtalks-aryan-4787
gcloud config set run/region us-central1
gcloud config set compute/region us-central1
gcloud config set artifacts/location us-central1
```

Verify: `gcloud config list` should show `project=devtalks-aryan-4787` and `account=<your email>`.

### Firebase

```bash
firebase login                                # browser flow
firebase projects:list                        # must list "DevTalks (devtalks-aryan-4787)"
firebase use devtalks-aryan-4787              # works once you're in the repo root (uses .firebaserc)
```

### GitHub

```bash
gh auth login        # pick HTTPS, prompt for browser auth, accept default scopes
gh auth status       # must show repo+workflow scopes
```

If you'll register an SSH signing key later (next section), also grant: `gh auth refresh -s admin:ssh_signing_key`.

---

## 5. Git signing (required — `main` is protection-gated by signature verification)

All commits in this repo must be **SSH-signed** and show as **Verified** on GitHub. One-time setup:

```bash
# 1. Generate an SSH key if you don't have one:
[ -f ~/.ssh/id_ed25519 ] || ssh-keygen -t ed25519 -C "$(git config --global user.email)"

# 2. Tell git to sign every commit with that key:
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
git config --global tag.gpgsign true

# 3. Set up local signature verification (optional but useful):
mkdir -p ~/.config/git
EMAIL=$(git config --global user.email)
KEY=$(cat ~/.ssh/id_ed25519.pub | awk '{print $1" "$2}')
echo "$EMAIL $KEY" > ~/.config/git/allowed_signers
git config --global gpg.ssh.allowedSignersFile ~/.config/git/allowed_signers
```

Then upload the public key to GitHub **as a Signing Key** (separate from authentication keys):

- https://github.com/settings/ssh/new → **Key type: Signing Key** → paste `~/.ssh/id_ed25519.pub` contents → Add.

Verify: `gh api /user/emails` shows your commit email as verified, then `git log --show-signature -1` on any signed commit should report "Good \"git\" signature".

---

## 6. Repository setup

```bash
git clone https://github.com/aryanshah295/devtalks.git
cd devtalks
./gradlew --version                # bootstraps the wrapper distribution (~30s first time)
```

The frontend config is committed:
- `frontend/.env.production` already holds the Firebase Web SDK config (apiKey, authDomain, etc.) — non-secret per Firebase docs, intentionally checked in.
- Local development reads the same file (Vite picks `.env.production` for `pnpm build` and merges other `.env*` files for `pnpm dev`).

---

## 7. Verify the install

### Backend builds and tests pass

```bash
# `-x :proto:generateProto` skips an in-Gradle buf invocation that fails outside of CI
# (the gradle process doesn't inherit your shell PATH, so it can't find `buf`).
# Generated stubs are committed-out-of-band — run `cd proto && buf generate` if you
# touched a .proto file.
./gradlew :services:api-gateway:build -x :proto:generateProto

# Expected output ending:
#   HealthControllerTest > api healthz returns ok() PASSED
#   TalkCatalogServiceTest > getTalk … PASSED   (×3)
#   TalksControllerTest > GET talks … PASSED    (×3)
#   BUILD SUCCESSFUL
```

### Backend boots and serves locally

```bash
./gradlew :services:api-gateway:bootRun --args='--spring.profiles.active=local' \
    -x :proto:generateProto &
sleep 10
curl -s http://localhost:8080/api/healthz       # → {"status":"ok"}
curl -s http://localhost:8080/api/v1/talks      # → {"talks":[…5 items…],"nextPageToken":""}
kill %1
```

### Frontend builds and serves locally

```bash
cd frontend
pnpm install
pnpm dev    # → http://localhost:5173
```

Open http://localhost:5173 — the page should show the placeholder healthz status. (Until the frontend pages ship, there's no list/detail UI yet.)

### Seed Firestore (optional — already seeded for the shared project)

```bash
./gradlew :services:api-gateway:runSeedScript -x :proto:generateProto
# Spring boots under the `seed` profile, writes 5 KubeCon talks via batch.set, exits.
# Idempotent; safe to re-run.
```

Verify via the Firestore console: https://console.cloud.google.com/firestore/databases/-default-/data/panel/talks?project=devtalks-aryan-4787

### Proto codegen works

```bash
cd proto
buf lint && buf build && buf generate
ls gen/kotlin/com/devtalks/v1/        # → TalkCatalogServiceGrpcKt.kt, Talk.kt, etc.
```

---

## 8. Troubleshooting

| Symptom | Likely cause |
|---|---|
| `./gradlew jib` fails with "Set -PgcpProject=..." | Run with `GCP_PROJECT_ID=devtalks-aryan-4787 ./gradlew :services:api-gateway:jib` or pass `-PgcpProject=devtalks-aryan-4787`. |
| Gradle's `generateProto` task fails with `command 'buf'` | Gradle's process PATH doesn't include `buf`. Run with `-x :proto:generateProto` or invoke `cd proto && buf generate` manually before building. |
| `terraform apply` says state is locked | Another `terraform` invocation didn't release the lock. Run `terraform force-unlock <lock-id>` only after confirming no one else is mid-apply. |
| Firestore SDK throws `PERMISSION_DENIED` | `gcloud auth application-default login` not run, or ADC user lacks `roles/datastore.user`. |
| Browser shows Firebase Auth `auth/unauthorized-domain` | Add the dev URL to Firebase Console → Authentication → Settings → Authorized domains. `localhost` and `*.web.app` are pre-authorized. |
| `git push` rejected with "signature required" | Signing not configured (see §5). |
| `git push origin main` rejected | Direct pushes to `main` are blocked by the GitHub Ruleset. Open a PR instead. |
| Cloud Run returns Google's branded 404 at `/healthz` | The `*.run.app` edge reserves `/healthz`. Use `/api/healthz` instead. |

---

## 9. What you do NOT need to set up

These are already provisioned in the shared GCP project — recreating them would either fail or break the deployed system:

- The GCP project `devtalks-aryan-4787` itself.
- Firestore database (default, Native mode, `us-central1`).
- Artifact Registry repo `devtalks` in `us-central1`.
- GCS bucket `gs://devtalks-aryan-4787-tfstate` (Terraform remote state).
- Cloud Run service `api-gateway` (Terraform-managed; see `infra/cloudrun.tf`).
- Firebase Hosting site `devtalks-aryan-4787`.
- Firebase Auth providers (Anonymous + Google).
- Cloud Build connections + triggers (`pr-check`, `main-deploy`) in `us-central1`.
- GitHub repo ruleset `main-protection` (requires PR, requires `pr-check` to pass).

If you're forking the project to your own GCP, see `infra/` and `PLAN.md` "Phase 0 — Foundations" for the provisioning steps.
