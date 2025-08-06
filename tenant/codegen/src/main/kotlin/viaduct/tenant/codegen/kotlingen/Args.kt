package viaduct.tenant.codegen.kotlingen

import java.io.File

class Args(
    val tenantPackage: String,
    val tenantPackagePrefix: String,
    val grtPackage: String,
    val modernModuleGeneratedDir: File,
    val metainfGeneratedDir: File,
    val resolverGeneratedDir: File,
    val isFeatureAppTest: Boolean = false,
)
