package dev.gaphunter.moduleboundaryleakcompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Single-module coverage only -- see `KNOWN_ISSUES.md` for the honest
 * gap on cross-module coverage. [BasePlatformTestCase]'s light fixture
 * (single module, shared across tests) is enough to exercise every
 * same-module and wildcard-import case; a same-module internal import
 * is expected to never be flagged, which is exactly what this suite
 * checks.
 */
class ModuleBoundaryLeakInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(ModuleBoundaryLeakInspection::class.java)
    }

    fun `test importing an internal class from the SAME module produces no warning`() {
        myFixture.addFileToProject(
            "com/acme/internal/InternalHelper.java",
            """
            package com.acme.internal;
            public class InternalHelper {}
            """.trimIndent(),
        )
        myFixture.configureByText(
            "Caller.java",
            """
            import com.acme.internal.InternalHelper;

            class Caller {
                InternalHelper h;
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("crosses a module boundary") == true })
    }

    fun `test importing a non-internal class produces no warning`() {
        myFixture.addFileToProject(
            "com/acme/pub/PublicApi.java",
            """
            package com.acme.pub;
            public class PublicApi {}
            """.trimIndent(),
        )
        myFixture.configureByText(
            "Caller2.java",
            """
            import com.acme.pub.PublicApi;

            class Caller2 {
                PublicApi p;
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("crosses a module boundary") == true })
    }

    fun `test a wildcard import of an internal package is never flagged`() {
        myFixture.addFileToProject(
            "com/acme/internal/Widget.java",
            """
            package com.acme.internal;
            public class Widget {}
            """.trimIndent(),
        )
        myFixture.configureByText(
            "Caller3.java",
            """
            import com.acme.internal.*;

            class Caller3 {
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("crosses a module boundary") == true })
    }

    fun `test a non-java file is never scanned`() {
        myFixture.configureByText(
            "notes.txt",
            "import com.acme.internal.InternalHelper;",
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("crosses a module boundary") == true })
    }

    fun `test an unresolved import is silently skipped, never flagged`() {
        myFixture.configureByText(
            "Caller4.java",
            """
            import com.doesnotexist.internal.Ghost;

            class Caller4 {
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("crosses a module boundary") == true })
    }
}
