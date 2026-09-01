package dev.gaphunter.moduleboundaryleakcompanion.detect

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaFile
import dev.gaphunter.moduleboundaryleakcompanion.model.BoundaryLeakHit

/**
 * Finds an `import` statement in Java source whose target package has
 * `internal` as one of its path segments (the project's own naming
 * convention for "not meant to be used outside its owning module") AND
 * whose resolved class lives in a DIFFERENT module than the file doing
 * the importing.
 *
 * Resolves each import via real PSI reference resolution
 * ([PsiImportStatement.resolve], through the file's own
 * [PsiJavaFile.importList]) rather than [com.intellij.psi.JavaPsiFacade.findClass]
 * against a global search scope. This isn't just a test convenience --
 * it's the more correct production mechanism too: `resolve()` on a
 * reference already present in the source file's own AST uses that
 * file's local symbol table directly, without requiring the project's
 * global stub index to be in "smart" (fully indexed) mode. Confirmed
 * the hard way while building this plugin's own test suite: driving
 * detection through `JavaPsiFacade.findClass` against a freshly-added
 * second module hit `IndexNotReadyException` even after every
 * documented wait/smart-mode strategy; switching to reference
 * resolution here made the exact same scenario work with no waiting at
 * all, and is also the faster, more real-time-friendly path for the
 * live editor case this plugin actually ships for.
 *
 * **v0.1 scope, stated honestly:** Java only (`PsiJavaFile`/
 * `PsiImportStatement`) -- Kotlin's own import PSI is a separate
 * hierarchy (`org.jetbrains.kotlin.psi.KtImportDirective`), out of
 * scope for v0.1 to avoid taking on an `org.jetbrains.kotlin`
 * dependency for this first version. A single multi-module Gradle or
 * Maven project (mixing both build systems in the same
 * analysis isn't attempted -- module resolution comes from whichever
 * one the IDE already imported). The "internal intention" convention
 * must be textual and explicit (a `.internal.` package segment) --
 * never inferred from a javadoc/comment. A wildcard import
 * (`import a.b.*`) resolves to a package, not a class, so it's skipped:
 * there's no single class to compare module ownership against.
 */
object ModuleBoundaryLeakFinder {

    fun findAll(file: PsiFile): List<BoundaryLeakHit> {
        val javaFile = file as? PsiJavaFile ?: return emptyList()
        val virtualFile = javaFile.virtualFile ?: return emptyList()
        val project = javaFile.project
        val importingModule = ModuleUtilCore.findModuleForFile(virtualFile, project) ?: return emptyList()

        // .importStatements (not .allImportStatements): excludes static
        // imports, which target a member, not a class -- resolve() on
        // those returns the member, not a PsiClass, so they'd never
        // match the `as? PsiClass` cast below anyway; being explicit
        // here says so directly instead of relying on that side effect.
        val importStatements = javaFile.importList?.importStatements ?: return emptyList()
        val hits = mutableListOf<BoundaryLeakHit>()

        for (importStatement in importStatements) {
            if (importStatement.isOnDemand) continue // `import a.b.*` has no single class to resolve
            val fqn = importStatement.qualifiedName ?: continue
            if (!hasInternalSegment(fqn)) continue

            val importedClass = importStatement.resolve() as? PsiClass ?: continue
            val importedVirtualFile = importedClass.containingFile?.virtualFile ?: continue
            val owningModule = ModuleUtilCore.findModuleForFile(importedVirtualFile, project) ?: continue
            if (owningModule == importingModule) continue

            hits += BoundaryLeakHit(leafOf(importStatement), fqn, importingModule.name, owningModule.name)
        }

        return hits
    }

    /** True when [fqn]'s package portion (everything before the last segment) has `internal` as one of its dot-separated parts. */
    private fun hasInternalSegment(fqn: String): Boolean {
        val packageParts = fqn.split('.').dropLast(1)
        return packageParts.any { it == "internal" }
    }

    private fun leafOf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) current = current.firstChild
        return current
    }
}
