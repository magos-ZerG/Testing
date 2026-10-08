# StudyMate Lab 2 — GitHub Actions

## What this patch adds

- `.github/workflows/lab2-ci.yml`: automatic run on push / pull request (and manual dispatch) on GitHub-hosted `ubuntu-24.04`. The existing `lab2/scripts/run-lab2.sh all` ensures `unit → integration → e2e`, PostgreSQL isolation and cleanup, and skipped reports on failure. Download the artifact `lab2-reports-*` from the workflow run. Allure history is restored and saved through Actions cache.
- `.github/workflows/lab2-full-device.yml`: optional *manual* full run on a self-hosted Fedora runner with Docker, Android SDK / ADB, and the authorized physical Android phone. Register the machine in **Repository Settings → Actions → Runners → New self-hosted runner** using the instructions provided by GitHub and add the custom label `android-device`. The runner user must have Docker and USB/ADB access, and `adb devices` must show `device`. Use this only in a **private trusted repository**.

## Install

Copy the `.github` directory from the patch to the root of the repository (alongside `Makefile` and `lab2/`). Commit and push the added workflows.

## Launch and inspect

1. GitHub → **Actions** → **StudyMate Lab 2 - CI**; pushes and PRs automatically run it. **Run workflow** also works from the default branch.
2. Wait for the job named **Unit -> Integration -> E2E**. The entire test stack runs in Docker on an isolated disposable PostgreSQL instance.
3. Open the workflow run → **Artifacts** → download `lab2-reports-*`; use a local HTTP server for Allure HTML (`python3 -m http.server 8765 --directory lab2/reports/allure-report`) rather than opening index.html via `file://`.
4. For phone tests, on the registered self-hosted runner, connect/authorize the device then go to **Actions** → **StudyMate Lab 2 - Full CI with physical Android** → **Run workflow**.

## Notes

- GitHub-hosted runners cannot access the user's local USB phone. The physical-device workflow requires self-hosted runner registration.
- `RUN_ANDROID_DEVICE_TESTS=0` on the GitHub-hosted workflow: Android *unit* tests still execute, but physical-device integration is marked skipped. The manual workflow executes all tests, including the device.
- `actions/upload-artifact` uploads reports even if tests failed; the runner script creates reports even on failure.
- This is **CI**, satisfying the laboratory requirement to run tests in a CI/CD environment; the laboratory does not require deploying the production app.
- Do not commit `server/.env`, credentials, `local.properties`, or generated reports and build folders.
- The GitHub Actions YAML has not been end-to-end executed against a live GitHub runner. If you see a dependency or disk-space failure in the hosted environment, inspect the run's logs and adjust the runner.
