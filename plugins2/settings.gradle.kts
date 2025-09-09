@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenLocal {
            content {
                includeGroup("com.airbnb.viaduct")
            }
        }
        mavenCentral()
    }

    repositories {
        gradlePluginPortal()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

/* 

Note about using mavenLocal:

This plugin needs the codegen project from the larger build.  However,
as a Gradle plugin project, the Kotlin version of this project needs
to agree with the Kotlin version used by the Gradle that's doing the
actual build, which is 1.9.  Right now, the larger build is stuck on
Kotlin 1.8.  So unfortunately this project cannot be just another
subproject of the larger build.

Making this plugin project an included build of the larger project
doesn't solve the problem, because included builds can't depend on
projects from the including build.

Making the larger project an included build of this plugins project is
a theoretical possibility.  (These plugins are intended for use by
_consumers_ of Viaduct, not by the Viaduct build itself.)  However, in
practice they trip up some of our pre-compiled plugin scripts, so we
backed out of that approach (although we should continue to look into
it).

So the current solution is to publish the codegen project to the Maven
local cache, and for the plugin project to depend on it from there.
Eventually we can bump the Kotlin version of the larger build to 1.9,
or we can fix the pre-compiled build script problem, so we can stop
using the local cache.
 
*/
