// Top-level build file where you can add configuration options common to all sub-projects/modules.
// AGP 9 ships built-in Kotlin support, so the Kotlin Android plugin is not applied.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
