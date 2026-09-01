<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Module Boundary Leak Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Warning on an `import` of an `internal` package (a `.internal.`
  package segment) from a different Gradle/Maven module than the one
  that declares it -- hidden architecture coupling that precedes
  cascading-deployment incidents in monorepos/microservices.
- Resolves imports via real PSI reference resolution
  (`PsiImportStatement.resolve()`), combined with the real platform
  module graph.

[Unreleased]: https://github.com/GapHunterLabs/module-boundary-leak-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/module-boundary-leak-companion/commits/0.1.0
