# Dokka module READMEs belong to source-set includes

## Observed failure

The official Dokka CLI generated complete API sites without the module README body.
The Maven module had nine HTML pages and working assets, but its introduction, usage example,
compiler flags and implementation explanation were absent.

## Verified correction

Dokka 2.2.0 exposes `includes` on both its global configuration and `DokkaSourceSet` model.
For the individual module sites, attach `README.md` to the `main` source set, or to `common`
for a multiplatform module. A top-level `includes` entry alone did not populate these module
pages. Use `# Module <module-name>` as the README's opening heading.

The production site verifier checks the README introduction, section headings and code blocks
in the rendered module index. It checks local resources and relative page links across every
generated HTML file, resolving them from the page's own directory. The existing incomplete
Maven site is rejected by this verifier.

The Toolchain publisher can supply a Javadoc JAR containing only its manifest and the exact
`Empty Javadoc JAR` README. The release finalizer replaces that placeholder with the API website
entry point and license, while preserving populated Javadoc archives. The finalized repository
then receives refreshed SNAPSHOT descriptors and checksums. The current module metadata does
not reference Javadoc files; its other artifact sizes and hashes remain valid.

## Verification

The official CLI rendered all eight modules from their current sources and actual main JVM
classpaths: 4,206 API pages passed README body/code and local-link checks. The Maven and Gradle
module pages include both compiler language flags. The merged site contains the API landing
page and eight working module links, with 4,207 API HTML pages in total. The core page uses the
current `SqlManager.registerDatabase` API. The `/api/` entry resolves the finalized Javadoc
archive's documentation link, and the API redirect remains ahead of the SPA fallback.

An actual fifteen-target staging copy passed the publication gate after finalization: 50 empty
Javadoc placeholders became API entry points, four populated documentation archives remained
byte-identical, and 58 SNAPSHOT descriptors registered all 285 payloads with current timestamps
and all four valid checksum sidecars.

Evidence: `/tmp/kronos-python-dokka-all38/verification-final.json` and
`/tmp/kronos-dokka-site-verification54.json`, and
`/tmp/kronos-release-docs55/verification.json` from the verified run.
