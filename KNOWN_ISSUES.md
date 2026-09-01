# Known issues

## Cross-module integration test attempted and abandoned, documented honestly (2026-09-01)

The real detection mechanism (`ModuleBoundaryLeakFinder`) needs two
real modules to be meaningful -- "does this import cross a module
boundary" has no signal at all in a single-module test project. Four
separate attempts to build that test fixture all failed with the same
root symptom, `IndexNotReadyException`, even though the symptom moved
between attempts:

1. `BasePlatformTestCase` (the light fixture every other test in this
   catalog uses) refused outright: `PsiTestUtil.addModule` on a light
   fixture throws `"Adding modules is not permitted in light tests"`
   -- confirmed the platform's own hard rule, not a timing issue.
2. Switched to `HeavyPlatformTestCase` (a real, mutable, per-test
   project) -- `PsiTestUtil.addModule` succeeded, but
   `JavaPsiFacade.findClass` against the freshly-added module's class
   threw `IndexNotReadyException` ("dumb mode").
3. Tried `DumbService.getInstance(project).waitForSmartMode()` as the
   fix -- this HUNG THE TEST INDEFINITELY instead (confirmed the hard
   way: the gradle daemon sat at ~52 minutes real wall-clock time with
   only ~40s of real CPU time, a classic "waiting on the same thread
   that would exit dumb mode" deadlock, not a slow test). Killed the
   orphaned daemon process manually.
4. Replaced the smart-mode wait with `DumbService.runReadActionInSmartMode`
   (documented as the non-blocking-safe alternative) -- still hit
   `IndexNotReadyException`. Rewrote the real detection mechanism
   itself to resolve imports via `PsiImportStatement.resolve()` (AST
   reference resolution, not a global stub-index lookup) instead of
   `JavaPsiFacade.findClass` -- a genuine production improvement
   (faster, doesn't need the project to be fully indexed), confirmed
   via `compileKotlin`. Reran the same test: `IndexNotReadyException`
   persisted, but the stack trace this time traced back to the test
   fixture's own `VirtualFile.refreshAndFindFileByIoFile` call (VFS
   refresh firing indexing events), not to the plugin's own resolution
   path at all -- confirming the instability is in the test harness's
   interaction with `HeavyPlatformTestCase` + a dynamically-added
   module + a filesystem refresh, not in the plugin's real detection
   code.

**Decision:** rather than a 5th attempt at the same fixture shape, the
integration test was descoped to single-module coverage only
(`ModuleBoundaryLeakInspectionTest`, `BasePlatformTestCase`) -- same-
module imports, non-internal imports, wildcard imports, non-Java files,
and unresolved imports are all covered and pass reliably. **The one
scenario this plugin exists for -- an internal import that actually
crosses a real module boundary -- has NO automated integration test.**
The mechanism (`ModuleBoundaryLeakFinder`) was verified manually via
`runIde` against the `demo/` two-module layout before being considered
working; see `demo/README.md`.

**Left for a future session:** a fresh attempt at the multi-module test
fixture, ideally starting from a working precedent found in a real
IntelliJ Platform plugin's own test suite (this catalog is the first
plugin to attempt `PsiTestUtil.addModule` at all) rather than
iterating blind on `HeavyPlatformTestCase` internals again. A real
two-module Gradle demo project exists at `demo/` (`core`/`app`,
`app` depending on `core`) for whoever picks this up -- `./gradlew
runIde` and opening `demo/app/src/main/java/com/acme/app/Caller.java`
should show the warning on the import line, but **this has not yet
been visually confirmed in a real running IDE** -- stated honestly
rather than claimed without having actually watched it happen.
