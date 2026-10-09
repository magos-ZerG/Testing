#!/usr/bin/env python3
"""Fast source-level invariants, no Android SDK or network required."""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
app = root / "mobile-app/app"
client = (app / "src/androidTest/java/com/z23u184/studymate/app/StudyMateApplicationE2ETest.kt").read_text()
bridge = (app / "src/debug/java/com/z23u184/studymate/app/e2e/Lab2E2eBridgeProvider.kt").read_text()
manifest = (app / "src/debug/AndroidManifest.xml").read_text()
build = (app / "build.gradle.kts").read_text()
runner = (root / "lab2/scripts/run-lab2.sh").read_text()
workflow = (root / ".github/workflows/lab2-ci.yml").read_text()

assert "ContentResolver" in client or "contentResolver.call(" in client
assert "Retrofit.Builder()" in client and "backend.pull(" in client
assert not any(s in client for s in ("org.koin", "StudyMateDatabase", "CreateTopicUseCase", "CreateTaskUseCase", "StudyDataSyncCoordinator", "MainActivity", "Espresso", "koin.get"))
assert "@POST(\"api/v1/auth/login\")" in client and "@GET(\"api/v1/sync/pull\")" in client
assert "Lab2E2eBridgeProvider" in bridge and "GetTaskByIdUseCase" in bridge
assert 'android:enabled="${lab2E2eBridgeEnabled}"' in manifest
assert 'manifestPlaceholders["lab2E2eBridgeEnabled"] = lab2E2e.toString()' in build
assert "applicationIdSuffix = \".lab2e2e\"" in build
assert 'serial="$(adb devices' in runner and "emulator-" in runner
assert "reactivecircus/android-emulator-runner@v2" in workflow
assert "-Plab2E2e=true" in runner
print("PASS: black-box contract, Retrofit oracle, disabled bridge and GitHub emulator verified")
