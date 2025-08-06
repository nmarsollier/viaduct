package viaduct.tenant.codegen.bytecode

import viaduct.utils.timer.Timer

data class CodeGenArgs(
    val moduleName: String?,
    val pkgForGeneratedClasses: String,
    val includeIneligibleTypesForTestingOnly: Boolean,
    val excludeCrossModuleFields: Boolean,
    val javaTargetVersion: Int?,
    val workerNumber: Int,
    val workerCount: Int,
    val timer: Timer,
)
