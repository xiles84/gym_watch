package com.gymwatch.core.application

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The hexagon, enforced.
 *
 * These are not style checks. If one fails, the core has picked up a dependency
 * on the outside world and the "swap an adapter without touching the core"
 * promise is gone. Fix the dependency, never the test.
 *
 * Scoped by package rather than by file path so it behaves the same on Windows
 * and CI, and restricted to `main` so these rules do not police themselves.
 */
class ArchitectureTest {

    private val coreFiles: List<KoFileDeclaration>
        get() = Konsist.scopeFromProject()
            .files
            .filter { it.hasPackage("com.gymwatch.core..") }
            .filter { it.resideInSourceSet("main") }

    @Test
    fun `core never imports the Android SDK`() {
        coreFiles.assertFalse { file ->
            file.imports.any {
                it.name.startsWith("android.") || it.name.startsWith("androidx.")
            }
        }
    }

    @Test
    fun `core never depends on an adapter`() {
        coreFiles.assertFalse { file ->
            file.imports.any { it.name.startsWith("com.gymwatch.adapters") }
        }
    }

    @Test
    fun `core never reaches for a wall clock`() {
        // System.currentTimeMillis() jumps when the user or the network changes
        // the time, which would corrupt a running chronometer. Time enters the
        // core only through ClockPort.
        //
        // Comments are stripped before matching: ClockPort's own KDoc names the
        // forbidden call in order to explain why it is forbidden, and a rule
        // that fails on its own rationale is a rule people delete.
        val banned = listOf("System.currentTimeMillis", "Instant.now", "LocalDateTime.now")

        val offenders = coreFiles.filter { file ->
            file.text.lineSequence()
                .map { it.trim() }
                .filterNot { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") }
                .any { line -> banned.any(line::contains) }
        }

        assertTrue(
            offenders.isEmpty(),
            "Time must enter the core only through ClockPort, but found a wall clock in: " +
                offenders.map { it.name },
        )
    }

    @Test
    fun `driven ports are interfaces, never concrete classes`() {
        val concretePorts = coreFiles
            .filter { it.hasPackage("com.gymwatch.core.domain.port..") }
            .flatMap { it.classes() }
            .map { it.name }

        assertTrue(
            concretePorts.isEmpty(),
            "Ports must stay abstractions, but found concrete classes: $concretePorts",
        )
    }

    @Test
    fun `the core is not empty`() {
        // Guards against a scope-filter typo silently turning every rule above
        // into a vacuous pass.
        assertTrue(
            coreFiles.size >= 8,
            "expected the core to be scanned, got ${coreFiles.size} files",
        )
    }
}
