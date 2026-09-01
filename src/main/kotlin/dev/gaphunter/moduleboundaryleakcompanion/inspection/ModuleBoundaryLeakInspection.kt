package dev.gaphunter.moduleboundaryleakcompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import dev.gaphunter.moduleboundaryleakcompanion.detect.ModuleBoundaryLeakFinder
import dev.gaphunter.moduleboundaryleakcompanion.model.BoundaryLeakHit
import dev.gaphunter.moduleboundaryleakcompanion.review.ReviewPrompt

/**
 * Flags an `import` of an `internal`-package class from a DIFFERENT
 * Gradle/Maven module than the one that declares it -- hidden
 * architecture coupling that precedes cascading-deployment incidents
 * in large monorepos/microservices (a module's "private" internals
 * change, breaking an undeclared consumer that should never have had
 * access).
 *
 * Runs via `checkFile` (same shape as every other inspection in this
 * catalog); [ModuleBoundaryLeakFinder] does the real work, combining a
 * plain-text import-line scan with real platform module resolution
 * ([com.intellij.openapi.module.ModuleUtilCore]) -- the first
 * mechanism in this catalog to reason about build-system module
 * structure alongside source PSI.
 */
class ModuleBoundaryLeakInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null

        val hits = ModuleBoundaryLeakFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: BoundaryLeakHit): String =
        "Import '${hit.importedPackage}' crosses a module boundary into an internal package -- " +
            "declared in module '${hit.owningModuleName}' but imported from module '${hit.importingModuleName}', " +
            "which was never meant to have access"
}
