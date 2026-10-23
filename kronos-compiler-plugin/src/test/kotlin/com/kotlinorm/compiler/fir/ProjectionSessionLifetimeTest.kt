/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.compiler.fir

import com.kotlinorm.compiler.KronosCompilerTestEnvironment
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.FirThreadUnsafeCachesFactory
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.lang.ref.WeakReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class ProjectionSessionLifetimeTest {
    @Test
    fun `generated projections stay alive with their session and release it afterward`() {
        val retired = generatedProjectionSession()
        repeat(10) {
            System.gc()
            if (retired.get() == null) return
            Thread.sleep(10)
        }
        assertNull(retired.get(), "Projection models or generated members retained a retired FIR session")
    }

    @Test
    @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
    fun `lazy declarations and member types belong to their compilation session`() = withSource { source ->
        val first = session()
        val other = session()
        val generator = KronosProjectionDeclarationGenerationExtension(first)
        val model = model(first, source)
        KronosProjectionRegistry.register(first, model)
        assertEquals(setOf(model.classId, model.contextClassId), generator.getTopLevelClassIds())
        assertTrue(generator.hasPackage(model.classId.packageFqName))
        assertFalse(generator.hasPackage(FqName("unrelated.package")))
        assertSame(model.symbol, generator.generateTopLevelClassLikeDeclaration(model.classId))
        assertSame(model.contextSymbol, generator.generateTopLevelClassLikeDeclaration(model.contextClassId))
        assertSame(model.symbol, generator.generateTopLevelClassLikeDeclaration(model.classId))
        assertNull(generator.generateTopLevelClassLikeDeclaration(ClassId.topLevel(FqName("unrelated.Absent"))))

        assertNull(KronosProjectionRegistry.find(other, model.classId))
        assertNull(KronosProjectionRegistry.declarationGenerator(other))
        assertTrue(KronosProjectionRegistry.allTopLevelClassIds(other).isEmpty())
        KronosProjectionRegistry.refineAliasFieldType(other, "value", ProjectionAliasOccurrence(1, 0, 1),
            other.builtinTypes.intType.coneType)
        KronosProjectionDeclarationGenerationExtension(other)
        val different = model(other, source).let { item ->
            val fields = item.fields.map { it.copy(type = other.builtinTypes.intType.coneType) }
            item.copy(fields = fields, contextFields = fields)
        }
        KronosProjectionRegistry.register(other, different)
        KronosProjectionDeclarationGenerationExtension.ensureProjectionClassBound(other, different)
        KronosProjectionDeclarationGenerationExtension.ensureContextClassBound(other, different)
        val firstMembers = KronosProjectionDeclarationGenerationExtension.projectionMembers(first, model.symbol, model)
        val otherMembers = KronosProjectionDeclarationGenerationExtension.projectionMembers(other, different.symbol, different)
        assertNotSame(firstMembers, otherMembers)
        assertEquals(first.builtinTypes.stringType.coneType, firstMembers.properties.single().returnTypeRef.coneType)
        assertEquals(other.builtinTypes.intType.coneType, otherMembers.properties.single().returnTypeRef.coneType)
        KronosProjectionRegistry.registerDeclarationGenerator(first, generator)
        assertSame(model, KronosProjectionRegistry.find(first, model.classId))
        assertSame(generator, KronosProjectionRegistry.declarationGenerator(first))
    }

    @Test
    fun `nested alias refinement updates the nearest occurrence and invalidates its members once`() = withSource { source ->
        val owner = session()
        KronosProjectionDeclarationGenerationExtension(owner)
        val base = model(owner, source)
        val alias = Name.identifier("value")
        val fields = listOf(
            base.fields.single().copy(aliasOccurrence = ProjectionAliasOccurrence(1, 0, 100)),
            base.fields.single().copy(name = Name.identifier("value_2"), requestedName = alias,
                aliasOccurrence = ProjectionAliasOccurrence(1, 20, 40)),
        )
        val model = base.copy(fields = fields, contextFields = fields)
        KronosProjectionRegistry.register(owner, model)
        KronosProjectionDeclarationGenerationExtension.ensureProjectionClassBound(owner, model)
        KronosProjectionDeclarationGenerationExtension.ensureContextClassBound(owner, model)
        val original = KronosProjectionDeclarationGenerationExtension.projectionMembers(owner, model.symbol, model)
        val originalContext = KronosProjectionDeclarationGenerationExtension.projectionMembers(owner, model.contextSymbol, model)
        val occurrence = ProjectionAliasOccurrence(1, 25, 30)
        KronosProjectionRegistry.refineAliasFieldType(owner, "value", occurrence, owner.builtinTypes.intType.coneType)
        val refined = requireNotNull(KronosProjectionRegistry.find(owner, model.classId))
        assertEquals(listOf(owner.builtinTypes.stringType.coneType, owner.builtinTypes.intType.coneType),
            refined.fields.map { it.type })
        assertEquals(refined.fields, refined.contextFields)
        val updated = KronosProjectionDeclarationGenerationExtension.projectionMembers(owner, refined.symbol, refined)
        val updatedContext = KronosProjectionDeclarationGenerationExtension.projectionMembers(owner, refined.contextSymbol, refined)
        assertNotSame(original, updated)
        assertNotSame(originalContext, updatedContext)
        assertEquals(owner.builtinTypes.intType.coneType, updated.properties.last().returnTypeRef.coneType)
        KronosProjectionRegistry.refineAliasFieldType(owner, "value", occurrence, owner.builtinTypes.intType.coneType)
        KronosProjectionRegistry.refineAliasFieldType(owner, "value", occurrence.copy(scopeId = 2), owner.builtinTypes.longType.coneType)
        assertSame(refined, KronosProjectionRegistry.find(owner, model.classId))
        assertSame(updated, KronosProjectionDeclarationGenerationExtension.projectionMembers(owner, refined.symbol, refined))
    }

    @OptIn(SessionConfiguration::class, PrivateSessionConstructor::class)
    private fun session(): FirSession = object : FirSession(FirSession.Kind.Source) {}.also {
        it.register(FirModuleData::class, moduleData(it))
        it.register(FirCachesFactory::class, FirThreadUnsafeCachesFactory)
    }

    private fun model(session: FirSession, source: KtSourceElement): KronosProjectionModel {
        val classId = ClassId.topLevel(FqName("com.kotlinorm.generated.projection.LifetimeProjection"))
        val contextId = ClassId.topLevel(FqName("com.kotlinorm.generated.projection.LifetimeContext"))
        val fields = listOf(KronosProjectionField(Name.identifier("value"), session.builtinTypes.stringType.coneType, source))
        return KronosProjectionModel(
            classId, classId.shortClassName, FirRegularClassSymbol(classId),
            contextId, contextId.shortClassName, FirRegularClassSymbol(contextId),
            session.builtinTypes.anyType.coneType, fields, fields, emptySet(), source, source,
        )
    }

    private fun generatedProjectionSession(): WeakReference<FirSession> = withSource { source ->
        val session = session()
        KronosProjectionDeclarationGenerationExtension(session)
        val model = model(session, source)
        KronosProjectionRegistry.register(session, model)
        KronosProjectionDeclarationGenerationExtension.ensureProjectionClassBound(session, model)
        KronosProjectionDeclarationGenerationExtension.projectionMembers(session, model.symbol, model)
        System.gc()
        assertSame(model, KronosProjectionRegistry.find(session, model.classId))
        assertSame(model, KronosProjectionRegistry.findAny(model.classId))
        WeakReference(session)
    }

    @OptIn(CompilerConfiguration.Internals::class, ExperimentalCompilerApi::class)
    private fun <T> withSource(block: (KtSourceElement) -> T): T {
        KronosCompilerTestEnvironment.prepare()
        val create = KotlinCoreEnvironment::class.java.methods.single {
            it.name == "createForTests" && it.parameterTypes.firstOrNull()?.simpleName == "Disposable"
        }
        val disposableType = create.parameterTypes.first()
        val disposer = Class.forName(disposableType.packageName + ".util.Disposer")
        val disposable = disposer.getMethod("newDisposable").invoke(null)
        try {
            val configuration = CompilerConfiguration().apply {
                extensionsStorage = CompilerPluginRegistrar.ExtensionStorage()
            }
            val environment = create.invoke(null, disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
            val project = KotlinCoreEnvironment::class.java.getMethod("getProject").invoke(environment)
            val factory = KtPsiFactory::class.java.constructors.single {
                it.parameterCount == 1 && it.parameterTypes.single().simpleName == "Project"
            }.newInstance(project) as KtPsiFactory
            val source = KtRealPsiSourceElement::class.java.constructors.single { it.parameterCount == 1 }
                .newInstance(factory.createClass("class Source")) as KtSourceElement
            return block(source)
        } finally {
            val manager = Class.forName(disposableType.packageName + ".application.ApplicationManager")
            val application = manager.getMethod("getApplication").invoke(null)
            val applicationType = Class.forName(disposableType.packageName + ".application.Application")
            applicationType.getMethod("runWriteAction", Runnable::class.java).invoke(application, Runnable {
                disposer.getMethod("dispose", disposableType).invoke(null, disposable)
            })
        }
    }

    private fun moduleData(owner: FirSession): FirModuleData = object : FirModuleData() {
        override val name = Name.special("<projection-lifetime>")
        override val dependencies = emptyList<FirModuleData>()
        override val dependsOnDependencies = emptyList<FirModuleData>()
        override val allDependsOnDependencies = emptyList<FirModuleData>()
        override val friendDependencies = emptyList<FirModuleData>()
        override val platform = JvmPlatforms.defaultJvmPlatform
        override val isCommon = false
        override val session = owner
        override val stableModuleName = "projection-lifetime"
        override val areRedeclarationsEquivalent = false
    }
}
