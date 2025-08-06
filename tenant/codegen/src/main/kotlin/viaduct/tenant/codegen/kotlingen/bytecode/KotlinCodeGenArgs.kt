package viaduct.tenant.codegen.kotlingen.bytecode

import java.io.File
import viaduct.utils.timer.Timer

data class KotlinCodeGenArgs(
    val pkgForGeneratedClasses: String,
    val dirForOutput: File,
    val timer: Timer,
)
