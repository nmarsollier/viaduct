plugins {
    id("orchestration")
    id("versioning")
    alias(libs.plugins.kotlin.jvm) apply false
    id("com.vanniktech.maven.publish") version "0.34.0" apply false
}
