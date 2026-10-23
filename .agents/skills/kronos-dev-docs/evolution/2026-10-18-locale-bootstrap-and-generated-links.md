# Language bootstrap and generated documentation links

## Symptoms

- A first-time Chinese browser stored `zh-CN` but returned the initial `en` field.
- A documentation deep link briefly inherited homepage/storage language because `Router.url` was still `/` while the root component was constructed.
- Links generated from page display labels pointed at absent fragments after titles or section headings changed.
- Converting NgDoc content to static HTML left custom tabs/callouts and popup SVGs without their Angular processors/styles.

## Durable rules

Use one LocaleService. Seed from a valid documentation hash deep link during initial navigation, then make `NavigationEnd` authoritative. Test both languages with `Router.url === '/'` and the opposite stored preference; assert that the translation service never receives the wrong intermediate language.

Page links must not infer fragments from translated display labels. Only section links require a fragment. Read generated NgDoc `headerContent` as well as `pageContent` when verifying IDs; page titles are rendered in the wrapper, not in the Markdown body. Validate all static links and local heading fragments after generation.

Static output must convert every NgDoc tab into a readable labelled code section, callouts into semantic blockquotes, and interactive demos into explicit interactive-app links. Remove popup-only event handlers/icons. Compare code-alternative/table counts to the same NgDoc generated content and reject any remaining `ng-doc-*` elements.

## Verification

The behavior suite covers first navigation, URL query/fragment preservation, reactive language-isolated search, clipboard controls and lifecycle cleanup. The policy suite covers route/order/link failures, Unicode highlight offsets and deployment merge/API redirect ordering. Production build invokes the complete static output gate. See `scripts/`, `src/app/architecture.spec.ts`, and `.github/workflows/kronos-docs-testing.yml`.
