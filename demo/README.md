# Demo data for screenshots

A real two-module Gradle project: `core` declares
`com.acme.internal.InternalHelper`; `app` (which depends on `core` via
`implementation(project(":core"))`) imports it in `Caller.java` --
exactly the cross-module internal-package import this plugin flags.

## How to get the screenshot

1. `./gradlew runIde` from `module-boundary-leak-companion`, open this
   `demo/` folder as the project (let Gradle import both modules).
2. Full Screen, open `app/src/main/java/com/acme/app/Caller.java` -- a
   warning should appear on the `import com.acme.internal.InternalHelper;`
   line.
3. Screenshot with the import line and warning visible, save into
   `module-boundary-leak-companion/docs/screenshots/`. Close the
   sandbox.
