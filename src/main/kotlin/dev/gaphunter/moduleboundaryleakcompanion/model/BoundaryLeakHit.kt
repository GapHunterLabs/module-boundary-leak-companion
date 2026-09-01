package dev.gaphunter.moduleboundaryleakcompanion.model

import com.intellij.psi.PsiElement

/** One import statement whose target package is `internal` (by path convention) and whose resolved file lives in a different module than the importing file. */
data class BoundaryLeakHit(
    val anchor: PsiElement,
    val importedPackage: String,
    val importingModuleName: String,
    val owningModuleName: String,
)
