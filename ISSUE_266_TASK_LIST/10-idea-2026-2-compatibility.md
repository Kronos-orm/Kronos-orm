# Task 10: Publish An IDEA 2026.2-Installable Plugin

Progress: 35%
Status: In Progress

## Goal

Ensure the plugin built and published by the release workflow is recognized by Marketplace and IntelliJ IDEA as installable on stable IDEA 2026.2.

## Completed

- Gradle targets stable IDEA `2026.2` and Java compiler build `262.8665.258`.
- Local signed `0.2.4` and `0.2.5-SNAPSHOT` ZIP descriptors contain `<idea-version since-build="262"/>`.
- The implementation explicitly sets `sinceBuild = "262"` and `untilBuild = "262.*"`.
- CI now statically selects Temurin 25, binds `publishPlugin` to the signed archive, treats structure/signature/descriptor/Plugin Verifier failures as fatal, and includes a final-artifact inspection script.
- The main agent has statically reviewed those build/workflow/script changes; no signing, descriptor assertion, or Plugin Verifier task has run against them yet.
- The formal IU-262.8665.258 IDEA test gate is green at 32/32; this proves the local plugin/test runtime path but does not prove signed artifact or Marketplace compatibility.
- Marketplace metadata investigation distinguishes artifact-range compatibility from approval/listing state: update `1110390` reports IDEA Pro 2026.2 compatibility but remains unapproved and unlisted.

## Remaining

- Build and sign the exact artifact used by `publishPlugin`, then inspect the nested `META-INF/plugin.xml` for the explicit `262..262.*` range.
- Run the structure, signature, descriptor, and Plugin Verifier checks to prove the fail-fast dependency graph works.
- Compare the Marketplace version/artifact metadata with the signed local artifact after upload.
- Wait for or complete Marketplace approval/listing before treating an IDE-side compatibility download failure as a build-range regression.
- Install the Marketplace-served version in the installed IDEA 2026.2 and record successful enablement/startup.

## Acceptance

- The final signed ZIP explicitly covers build 262 and passes Plugin Verifier for the installed/declared IDEA 2026.2 target.
- The publish workflow uploads that same signed artifact and cannot silently skip failure.
- Marketplace lists the released version as compatible with IntelliJ IDEA 2026.2.
- IDEA 2026.2 offers Install/Update instead of the incompatibility warning shown in the supplied screenshot.
- After installation, the plugin loads without dependency, Kotlin mode, or build-range errors.

## Verification Record

- 2026-07-19 local build/ZIP inspection: partial pass; Marketplace mismatch remains unresolved.
- 2026-07-19 Marketplace public API/download inspection: partial pass; update metadata recognizes IDEA Pro 2026.2 compatibility, but the update is unapproved/unlisted and cannot be selected through the build-compatible download endpoint.
- 2026-07-19 implementation static review: explicit `262..262.*` range, stable 2026.2 target, Temurin 25, signed-upload pinning, fail-fast structure/signature/descriptor/Verifier dependencies, and artifact inspection script are present.
- 2026-07-19 serialized build/sign/Verifier: not run; static configuration review is not artifact compatibility proof.
