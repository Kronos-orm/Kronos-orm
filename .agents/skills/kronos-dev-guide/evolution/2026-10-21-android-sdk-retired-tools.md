# Android SDK setup must request platform-tools explicitly

The Android API 26 and 35 jobs in run `39617461835` stopped at `Set up Android SDK`, before Gradle or emulator tests. The pinned `android-actions/setup-android@9fc6c4e9069bf8d3d10b2204b1fb8f6ef7065407` requests `tools platform-tools` by default. Google removed the legacy `tools` package; the upstream report reproduces the failure with this exact action revision: [setup-android issue 537](https://github.com/android-actions/setup-android/issues/537).

Set `with.packages: platform-tools` on that action. The action still installs its command-line tools and accepts licenses. The following workflow step installs `platforms;android-35` and `build-tools;35.0.0`; the emulator runner supplies the API 26/35 system images. Keep assemble, lint, connected SQLite tests and executed-test verification in the gate.

The pinned action installs command-line tools 16.0, which support the workflow's JDK 17. The [current upstream README](https://github.com/android-actions/setup-android#the-deprecated-tools-package) documents the removed package and explicit `platform-tools` input. Local validation passed actionlint and diff checks. The individual job and complete-run log downloads returned TLS EOF; attribution uses the failed step, pinned action source and the upstream reproduction rather than an unavailable local copy of the job log.
