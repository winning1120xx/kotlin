/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsSource
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.templates.standard.ScriptTemplateWithArgs
import kotlin.test.Test
import kotlin.test.*

class ScriptProviderTest {

    init {
        setIdeaIoUseFallback()
    }

    @Test
    fun testLazyScriptDefinitionsProvider() {

        val genDefCounter = AtomicInteger()
        val standardDef = FakeScriptDefinition()
        val shadedDef = FakeScriptDefinition(".x.kts")
        val provider = TestCliScriptDefinitionProvider(standardDef).apply {
            setScriptDefinitions(listOf(shadedDef, standardDef))
            setScriptDefinitionsSources(
                listOf(
                    TestScriptDefinitionSource(
                        genDefCounter,
                        ".y.kts",
                        ".x.kts"
                    )
                )
            )
        }

        assertEquals(0, genDefCounter.get())

        provider.isScript(File("a.kt").toScriptSource()).let {
            assertFalse(it)
            assertEquals(0, genDefCounter.get())
        }

        provider.isScript(File("a.y.kts").toScriptSource()).let {
            assertTrue(it)
            assertEquals(1, genDefCounter.get())
        }

        provider.isScript(File("a.x.kts").toScriptSource()).let {
            assertTrue(it)
            assertEquals(1, genDefCounter.get())
            assertEquals(1, shadedDef.matchCounter.get())
        }

        provider.isScript(File("a.z.kts").toScriptSource()).let {
            assertTrue(it)
            assertEquals(2, genDefCounter.get())
            assertEquals(1, standardDef.matchCounter.get())
        }

        provider.isScript(File("a.ktx").toScriptSource()).let {
            assertFalse(it)
            assertEquals(2, genDefCounter.get())
        }
    }
}

private open class FakeScriptDefinition(val suffix: String = ".kts") :
    ScriptDefinition.FromLegacy(defaultJvmScriptingHostConfiguration, KotlinScriptDefinition(ScriptTemplateWithArgs::class)) {
    val matchCounter = AtomicInteger()
    override fun isScript(script: SourceCode): Boolean {
        val path = script.locationId ?: return false
        return path.endsWith(suffix).also {
            if (it) matchCounter.incrementAndGet()
        }
    }

    override val isDefault: Boolean
        get() = suffix == ".kts"
}

private class TestScriptDefinitionSource(val counter: AtomicInteger, val defGens: Iterable<() -> FakeScriptDefinition>) :
    ScriptDefinitionsSource {
    constructor(counter: AtomicInteger, vararg suffixes: String) : this(counter, suffixes.map {
        {
            FakeScriptDefinition(
                it
            )
        }
    })

    override val definitions: Sequence<ScriptDefinition> = sequence {
        for (gen in defGens) {
            counter.incrementAndGet()
            yield(gen())
        }
    }
}

private class TestCliScriptDefinitionProvider(private val standardDef: ScriptDefinition) : CliScriptDefinitionProvider() {
    override fun getDefaultDefinition(): ScriptDefinition = standardDef
}
