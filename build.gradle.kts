// Root build file. Deliberately thin.
//
// Only the plugins the *current* phase needs are declared here. Android plugins
// get added in phase 3 — keeping them out means `:core` builds and tests run
// with no Android toolchain involved at all, which is the point of the
// hexagonal split (see docs/ARCHITECTURE.md).
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}
