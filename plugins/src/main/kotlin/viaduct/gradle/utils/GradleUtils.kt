package viaduct.gradle.utils

import org.gradle.api.Project

fun computeChildProjectPath(
    project: Project,
    childProjectName: String
): String {
    val isRootProject = Project.PATH_SEPARATOR == project.path
    return when {
        isRootProject -> Project.PATH_SEPARATOR + childProjectName
        else -> project.path + Project.PATH_SEPARATOR + childProjectName
    }
}
