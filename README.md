# Scrutinizer Quick Start

Get from zero to your first policy evaluation in under 10 minutes.

---

## Prerequisites

| Tool | Why | Install |
|------|-----|---------|
| Docker Desktop | Runs the stack | https://www.docker.com/products/docker-desktop |
| `jq` | Used by the experiment runner and curl examples | `winget install jqlang.jq` (Windows), `brew install jq` (macOS), `apt install jq` (Linux) |
| `curl` | API interaction | preinstalled on macOS/Linux, included with Git Bash on Windows |

Optional but recommended:
- **Hosts file entries** for friendly URLs. Add to `C:\Windows\System32\drivers\etc\hosts` (Windows, requires admin) or `/etc/hosts` (Linux/macOS):
  ```
  127.0.0.1  scrutinizer.local
  127.0.0.1  gitlab.local
  ```

---

## 1. Start the stack

From the repo root:

```bash
docker compose up -d
```

This brings up three containers on the `scrutinizer-net` network:

| Service | URL | Purpose |
|---------|-----|---------|
| Postgres | `localhost:5450` | Persistence |
| API | `localhost:8080` | REST API + policy engine |
| Dashboard | `localhost:3000` (or `scrutinizer.local:3000`) | React UI |

First-time start takes ~30 seconds. Wait until:
```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/policies
# Expect: 200
```

To bring everything down:
```bash
docker compose down
```

---

## 2. Create a policy (dashboard wizard)

1. Open `http://localhost:3000` (or `http://scrutinizer.local:3000`).
2. Go to **Policies** → **Create Policy**.
3. Pick a starter template:
   - **Balanced** — reasonable defaults, bans known-bad packages
   - **Strict Compliance** — high bar, blocks on any failure
   - **Monitor Only** — observe-only, never blocks
   - **Start from Scratch** — empty
4. Walk through the wizard: name → rules → scoring slider → review → save.

---

## 3. Register a project

1. Go to **Projects** → **Create Project**.
2. Fill in:
   - **Name** — e.g. `ref-app-2`
   - **Repository URL** — the git remote URL (e.g. `http://gitlab.local:8929/root/ref-app-2`). This is how CI jobs identify the project.
   - **Policy** — pick the one you just created.
3. Save.

---

## 4. Submit your first SBOM

### Option A — from the dashboard

There's no direct upload from the dashboard yet; use one of the other paths.

### Option B — from a CI pipeline

Drop this `.gitlab-ci.yml` into your repo:

```yaml
stages: [scan]
scrutinizer:
  stage: scan
  image: curlimages/curl:latest
  script:
    - |
      curl -X POST "http://api:8080/api/v1/runs" \
        -F "sbom=@sbom.json" \
        -F "applicationName=$CI_PROJECT_NAME" \
        -F "repositoryUrl=$CI_PROJECT_URL"
```

The API resolves your project (and its policy) by repository URL. No policy ID needed in CI.

### Option C — from your shell

```bash
POLICY_ID=$(curl -s http://localhost:8080/api/v1/policies | jq -r '.[0].id')

curl -X POST http://localhost:8080/api/v1/runs \
  -F "sbom=@reference-env/sboms/ref-app-2.cdx.json" \
  -F "applicationName=ref-app-2" \
  -F "policyId=$POLICY_ID"
```

You can also use `repositoryUrl` instead of `policyId` (must match a registered project).

### Option D — pre-commit hook

Install in any git repo:

```bash
cd path/to/your/repo
bash /path/to/scrutinizer/scrutinizer-cli/install.sh
```

The hook auto-detects dependency-file changes (`package-lock.json`, `pom.xml`, etc.), generates an SBOM via `cdxgen`, and submits it. It uses `git remote get-url origin` to resolve the project — no env vars required if your repo is registered in the dashboard.

---

## 5. View results

In the dashboard:
- **Projects** → click your project → see the latest run, score, components, and findings.
- **Runs** → all evaluations across all projects.
- **Run detail page** → click **Export Audit Bundle** to download a ZIP with `posture-report.json`, `findings.csv`, and `evidence-manifest.json`.

---

## 6. Run the validation experiments

Four experiments verify the platform end-to-end. From the repo root:

```bash
bash reference-env/run-experiments.sh --skip-expiry-test
```

Experiment 4 (developer workflow latency) runs synthetically. The runner produces a JSON report at `reference-env/experiments/results/experiment-results.json` and a console scorecard.

To pre-warm enrichment caches before a demo:
```bash
bash reference-env/run-experiments.sh --warm-cache
```

---

## 7. Optional: GitLab integration

1. Start GitLab (separate from the Scrutinizer stack):
   ```bash
   docker run -d --name gitlab \
     --network scrutinizer-net \
     --hostname gitlab.local \
     -p 8929:8929 -p 2224:22 \
     --restart unless-stopped \
     --shm-size 256m \
     -e GITLAB_OMNIBUS_CONFIG="external_url 'http://gitlab.local:8929'; gitlab_rails['gitlab_shell_ssh_port'] = 2224; nginx['listen_port'] = 8929;" \
     gitlab/gitlab-ce:nightly
   ```
   Wait ~3 minutes for first boot.

2. Get the root password:
   ```bash
   docker exec gitlab cat //etc/gitlab/initial_root_password | grep Password
   ```

3. Register a runner. From the GitLab UI: **Admin → CI/CD → Runners → Register a new runner**, then:
   ```bash
   docker run -d --name gitlab-runner \
     --network scrutinizer-net \
     -v //var/run/docker.sock:/var/run/docker.sock \
     -v /path/to/runner-config:/etc/gitlab-runner \
     gitlab/gitlab-runner:latest

   docker exec gitlab-runner gitlab-runner register \
     --non-interactive \
     --url "http://gitlab:8929" \
     --token "<token-from-gitlab-ui>" \
     --executor "docker" \
     --docker-image "node:20-alpine" \
     --docker-network-mode "scrutinizer-net"
   ```

4. Push a project to GitLab, register it in Scrutinizer with the GitLab URL as `repositoryUrl`, and run the pipeline.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `HTTP 413` from `/api/v1/runs` | SBOM > 1 MB | Already raised to 50 MB in `application.yaml`; rebuild the API: `docker compose build api && docker compose up -d api` |
| Dashboard shows blank screen, console says `t.reduce is not a function` | Browser cached old JS bundle | Hard refresh (Ctrl+Shift+R) |
| `403 Forbidden` on PUT/POST from dashboard | CORS blocking your origin | Add your origin to `WebConfig.java` `allowedOrigins`, rebuild API |
| Policy creation: dashboard shows `404` for projects/exceptions | API returns Spring `Page` object; older client expected an array | Already fixed in `scrutinizerApi.ts` via `unwrapPage()` helper |
| GitLab Runner: `invalid volume specification: "C:\Program Files\Git\..."` | Git Bash mangling Unix paths in `config.toml` | Edit `/etc/gitlab-runner/config.toml` inside the runner container; replace the mangled path with `/var/run/docker.sock:/var/run/docker.sock` |
| All packages mass-fail with the balanced policy | Old engine treated `name EQ <pkg>` as a pass-rule (every non-matching package failed) | Fixed; rebuild API: `docker compose build api && docker compose up -d api` |
| Drift score = baseline score = 0 | No outbound network for OpenSSF Scorecard / npm registry | Use cache warm-up: `curl -X POST http://localhost:8080/api/v1/admin/warm-cache -F "sbom=@yoursbom.json"` |

---

## Useful URLs

| | URL |
|---|---|
| Dashboard | http://localhost:3000 / http://scrutinizer.local:3000 |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/api/v1/swagger-ui.html |
| GitLab | http://gitlab.local:8929 |

---

## What's where

```
scrutinizer-api/        Spring Boot REST API + policy engine glue
scrutinizer-engine/     Pure Java: parser, enrichment, evaluator, scoring
scrutinizer-dashboard/  React + MUI dashboard
scrutinizer-cli/        Pre-commit hook + GitLab CI template
reference-env/          SBOMs, policies, experiment runner, ground truth
ref-app-1/              Java/Spring Boot + React reference app
ref-app-2/              Node.js/Express reference app
docker-compose.yaml     Stack definition
```

---