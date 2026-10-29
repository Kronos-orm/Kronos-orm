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

package com.kotlinorm.compiler.fir.ide

import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.lang.ref.WeakReference

/**
 * Creates readable navigation declarations in either compiler host. CLI compilers only need PSI.
 * IDE hosts also need a project-scoped content extension and a module update before a new file
 * can be analyzed. All host ABI access lives here because the embeddable compiler relocates
 * IntelliJ types, while the IDE compiler uses their original names.
 */
internal fun KtPsiSourceElement.generatedNavigationFile(name: String, text: String, context: Boolean): KtFile {
    val original = KtPsiSourceElement::class.java.getMethod("getPsi").callHost(this)!!
    val host = NavigationHost.instance ?: return createNavigationFile(original, name, text)
    return host.file(original, name, text, context)
}

private fun createNavigationFile(original: Any, name: String, text: String): KtFile {
    val factory = KtPsiFactory::class.java.methods.single { it.name == "contextual" && it.parameterCount == 1 }
        .callHost(null, original) as KtPsiFactory
    return factory.createPhysicalFile(name, text)
}

/** Optional IDE integration, never initialized when the Analysis API is absent. */
private class NavigationHost(private val loader: ClassLoader) {
    private val psiElement = type("com.intellij.psi.PsiElement")
    private val psiFile = type("com.intellij.psi.PsiFile")
    private val projectType = type("com.intellij.openapi.project.Project")
    private val applicationType = type("com.intellij.openapi.application.Application")
    private val disposableType = type("com.intellij.openapi.Disposable")
    private val scopeType = type("com.intellij.psi.search.GlobalSearchScope")
    private val generatedScopeType = type("org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBaseResolveExtensionGeneratedFilesScope")
    private val resolutionScopeType = type("org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScope")
    private val providerType = type(STRUCTURE_PROVIDER)
    private val provider = providerType.getField("Companion").get(null)
    private val danglingType = type("org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule")
    private val refinerType = type("org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinContentScopeRefiner")
    private val association = type("org.jetbrains.kotlin.analysis.api.platform.projectStructure.UtilsKt")
        .methods.single { it.name == "setResolveExtensionFileModule" && it.parameterCount == 2 }
    private val application = type("com.intellij.openapi.application.ApplicationManager")
        .getMethod("getApplication").callHost(null)!!
    private val projects = mutableMapOf<Any, ProjectSources>()

    fun file(original: Any, name: String, text: String, context: Boolean): KtFile {
        val project = psiElement.getMethod("getProject").callHost(original)!!
        val moduleContext = sourceModule(original, project)
        val origin = sourceOrigin(original)
        val offset = psiElement.getMethod("getTextOffset").callHost(original) as Int
        val file = psiElement.getMethod("getContainingFile").callHost(original)!!
        val copied = moduleContext.dangling || file !== origin.file || !origin.physical
        val state = synchronized(projects) { projects.getOrPut(project) { ProjectSources(project) } }
        return state.file(origin, SourceSlot(offset, context), name, text, original, moduleContext.module, copied)
    }

    private fun sourceOrigin(original: Any): SourceOrigin {
        var file = psiElement.getMethod("getContainingFile").callHost(original)!!
        val visited = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Any, Boolean>())
        while (visited.add(file)) {
            val copy = psiFile.getMethod("getOriginalFile").callHost(file)!!
            if (copy !== file && copy !in visited) { file = copy; continue }
            val origin = type("org.jetbrains.kotlin.analysis.api.projectStructure.DanglingFilesKt")
                .getMethod("getCopyOrigin", psiFile).callHost(null, file)
            if (origin != null && origin !in visited) { file = origin; continue }
            if (KtFile::class.java.isInstance(file) && psiFile.getMethod("getVirtualFile").callHost(file) == null) {
                val context = type("org.jetbrains.kotlin.psi.KtPsiFactoryKt")
                    .getMethod("getAnalysisContext", KtFile::class.java).callHost(null, file)
                val contextFile = context?.let { psiElement.getMethod("getContainingFile").callHost(it) }
                if (contextFile != null && contextFile !in visited) { file = contextFile; continue }
            }
            break
        }
        val provider = psiFile.getMethod("getViewProvider").callHost(file)!!
        val virtualFile = psiFile.getMethod("getVirtualFile").callHost(file)
            ?: type("com.intellij.psi.FileViewProvider").getMethod("getVirtualFile").callHost(provider)!!
        val physical = psiElement.getMethod("isPhysical").callHost(file) == true &&
            psiFile.getMethod("getVirtualFile").callHost(file) != null
        return SourceOrigin(virtualFile, file, sourceStamp(file), physical)
    }

    private fun sourceStamp(file: Any): Long = psiFile.getMethod("getModificationStamp").callHost(file) as Long
    private fun sourceValid(file: Any): Boolean = psiElement.getMethod("isValid").callHost(file) as Boolean

    private fun sourceModule(original: Any, project: Any): ModuleContext {
        var module = provider.javaClass.methods.single { it.name == "getModule" && it.parameterCount == 3 }
            .callHost(provider, project, original, null)!!
        val dangling = danglingType.isInstance(module)
        while (danglingType.isInstance(module)) {
            module = danglingType.getMethod("getContextModule").callHost(module)!!
        }
        return ModuleContext(module, dangling)
    }

    private inner class ProjectSources(private val project: Any) {
        private val origins = linkedMapOf<Any, SourceGeneration>()
        private val pendingModules = linkedMapOf<Any, Long>()
        private var revision = 0L
        private var queued = false
        private var registered = false
        private var disposed = false
        private var publicationModules = emptySet<Any>()
        private var publicationFailure: Throwable? = null
        private val disposable = hostProxy(disposableType, "Kronos navigation source lifetime") { method, _ ->
            check(method == "dispose")
            synchronized(this) {
                disposed = true
                origins.values.forEach(::releaseGeneration)
                origins.clear()
                pendingModules.clear()
            }
            synchronized(projects) { projects.remove(project) }
            null
        }

        init {
            type("com.intellij.openapi.util.Disposer").getMethod("register", disposableType, disposableType)
                .callHost(null, project, disposable)
            // Each host owns its files even when an earlier KEFS loader already
            // installed the dynamic scope. Scope membership never replaces lifetime hooks.
            registerSourceChanges()
            registerModuleChanges()
        }

        fun file(origin: SourceOrigin, slot: SourceSlot, name: String, text: String, original: Any,
                 module: Any, copied: Boolean): KtFile {
            val result: KtFile
            val needsUpdate: Boolean
            synchronized(this) {
                if (disposed) cancelAnalysis()
                publicationFailure?.let { throw it }
                pruneChangedOrigins()
                val generation = if (origin.physical) origins.getOrPut(origin.key) {
                    SourceGeneration(origin, module)
                } else null
                generation?.module = module
                result = if (copied) {
                    // Preview/completion views belong to the current FIR session. Project state
                    // only holds weak references for invalidation, never the copied source PSI.
                    createNavigationFile(original, name, text).also { generated ->
                        generation?.copies?.apply { removeIf { it.get() == null }; add(WeakReference(generated)) }
                    }
                } else {
                    val current = requireNotNull(generation)
                    val previous = current.files[slot]
                    if (previous != null && previous.name == name && previous.text == text) previous.file else {
                        previous?.let { clearAssociation(it.file) }
                        createNavigationFile(original, name, text).also {
                            current.files[slot] = SourceFile(it, name, text)
                        }
                    }
                }
                val virtualFile = KtFile::class.java.getMethod("getVirtualFile").callHost(result)!!
                association.callHost(null, virtualFile, module)
                val companion = resolutionScopeType.getField("Companion").get(null)
                val scope = companion.javaClass.methods.single { it.name == "forModule" && it.parameterCount == 1 }
                    .callHost(companion, module)!!
                needsUpdate = scopeType.getMethod("contains", type("com.intellij.openapi.vfs.VirtualFile"))
                    .callHost(scope, virtualFile) != true
                if (needsUpdate) pendingModules[module] = ++revision
                queueUpdate()
            }
            // Only the first project extension registration needs a new module scope. Once the
            // dynamic scope is present, all newly tagged files are immediately analyzable.
            if (needsUpdate) cancelAnalysis()
            return result
        }

        private fun queueUpdate() {
            if (queued || disposed || pendingModules.isEmpty() || publicationFailure != null) return
            queued = true
            applicationType.getMethod("invokeLater", Runnable::class.java).callHost(application, Runnable {
                applicationType.getMethod("runWriteAction", Runnable::class.java)
                    .callHost(application, Runnable { publish() })
            })
        }

        private fun publish() {
            val snapshot = synchronized(this) {
                if (disposed || projectType.getMethod("isDisposed").callHost(project) == true) {
                    queued = false
                    return
                }
                try {
                    if (!registered) register()
                    pruneChangedOrigins()
                    Publication(revision, pendingModules.keys.toList()).also { publicationModules = it.modules.toSet() }
                } catch (failure: Throwable) {
                    if (!isControlFlow(failure)) publicationFailure = failure
                    queued = false
                    throw failure
                }
            }
            try {
                val kind = type("org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleStateModificationKind")
                    .getField("UPDATE").get(null)
                val update = type("org.jetbrains.kotlin.analysis.api.platform.modification.UtilsKt")
                    .methods.single { it.name == "publishModuleStateModificationEvent" && it.parameterCount == 2 }
                snapshot.modules.forEach { update.callHost(null, it, kind) }
                synchronized(this) {
                    pendingModules.entries.removeIf { it.value <= snapshot.revision }
                }
            } catch (failure: Throwable) {
                synchronized(this) { if (!isControlFlow(failure)) publicationFailure = failure }
                throw failure
            } finally {
                synchronized(this) {
                    publicationModules = emptySet()
                    queued = false
                    queueUpdate()
                }
            }
        }

        private fun register() {
            val refiner = hostProxy(refinerType, "Kronos generated navigation sources") { method, arguments ->
                when (method) {
                    "getEnlargementScopes" -> enlargementScopes(arguments!![0])
                    "getRestrictionScopes" -> emptyList<Any>()
                    else -> error("Unsupported content scope method: $method")
                }
            }
            val companion = refinerType.getField("Companion").get(null)
            val pointName = companion.javaClass.getMethod("getEP_NAME").callHost(companion)!!
            val point = type("com.intellij.openapi.extensions.ExtensionPointName")
                .methods.single { it.name == "getPoint" && it.parameterCount == 1 }.callHost(pointName, project)!!
            type("com.intellij.openapi.extensions.ExtensionPoint").getMethod("registerExtension", Any::class.java, disposableType)
                .callHost(point, refiner, disposable)
            registered = true
        }

        private fun registerSourceChanges() {
            val listenerType = type("com.intellij.psi.PsiTreeChangeListener")
            val listener = hostProxy(listenerType, "Kronos navigation source changes") { method, _ ->
                if (!method.startsWith("before")) synchronized(this) { pruneChangedOrigins(); queueUpdate() }
                null
            }
            val managerType = type("com.intellij.psi.PsiManager")
            val manager = managerType.getMethod("getInstance", projectType).callHost(null, project)
            managerType.getMethod("addPsiTreeChangeListener", listenerType, disposableType)
                .callHost(manager, listener, disposable)
        }

        private fun registerModuleChanges() {
            val eventType = type("org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent")
            val companion = eventType.getField("Companion").get(null)
            val topic = companion.javaClass.getMethod("getTOPIC").callHost(companion)!!
            val listenerType = type("org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventListener")
            val listener = hostProxy(listenerType, "Kronos navigation module changes") { _, arguments ->
                val event = arguments!![0]
                val stateType = type("org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleStateModificationEvent")
                var releasedSources = false
                synchronized(this) {
                    if (stateType.isInstance(event)) {
                        val module = stateType.getMethod("getModule").callHost(event)!!
                        if (module !in publicationModules) {
                            origins.entries.removeIf {
                                (it.value.module == module).also { remove -> if (remove) {
                                    releasedSources = hasSources(it.value) || releasedSources
                                    releaseGeneration(it.value)
                                } }
                            }
                            pendingModules.remove(module)
                        }
                    } else if (event.javaClass.simpleName in setOf("KotlinGlobalModuleStateModificationEvent",
                            "KotlinGlobalSourceModuleStateModificationEvent")) {
                        releasedSources = origins.values.any(::hasSources)
                        origins.values.forEach(::releaseGeneration)
                        origins.clear()
                        pendingModules.clear()
                    }
                }
                if (releasedSources) {
                    // Module events invalidate FIR sessions, while stock Kotlin PSI
                    // references also cache their resolved navigation targets separately.
                    // Notify that public cache outside our catalog lock after removing files.
                    val managerType = type("com.intellij.psi.PsiManager")
                    val manager = managerType.getMethod("getInstance", projectType).callHost(null, project)
                    managerType.getMethod("dropResolveCaches").callHost(manager)
                }
                null
            }
            val bus = type("org.jetbrains.kotlin.analysis.api.platform.KotlinMessageBusProviderKt")
                .getMethod("getAnalysisMessageBus", projectType).callHost(null, project)!!
            val connection = type("com.intellij.util.messages.MessageBus").getMethod("connect", disposableType)
                .callHost(bus, disposable)!!
            type("com.intellij.util.messages.MessageBusConnection")
                .getMethod("subscribe", type("com.intellij.util.messages.Topic"), Any::class.java)
                .callHost(connection, topic, listener)
        }

        private fun pruneChangedOrigins() {
            val iterator = origins.values.iterator()
            while (iterator.hasNext()) {
                val generation = iterator.next()
                val origin = generation.origin
                if (!sourceValid(origin.file) || sourceStamp(origin.file) != origin.stamp) {
                    iterator.remove()
                    releaseGeneration(generation)
                    pendingModules[generation.module] = ++revision
                }
            }
        }

        private fun releaseGeneration(generation: SourceGeneration) {
            generation.files.values.forEach { clearAssociation(it.file) }
            generation.copies.forEach { it.get()?.let(::clearAssociation) }
            generation.files.clear()
            generation.copies.clear()
        }

        private fun hasSources(generation: SourceGeneration): Boolean =
            generation.files.isNotEmpty() || generation.copies.any { it.get() != null }

        private fun enlargementScopes(module: Any): List<Any> =
            listOf(generatedScopeType.getConstructor(List::class.java).newInstance(listOf(module)))
    }

    private fun clearAssociation(file: KtFile) {
        association.callHost(null, KtFile::class.java.getMethod("getVirtualFile").callHost(file), null)
    }

    private fun hostProxy(type: Class<*>, label: String, action: (String, Array<out Any>?) -> Any?): Any =
        Proxy.newProxyInstance(loader, arrayOf(type)) { proxy, method, arguments ->
            when (method.name) {
                "equals" -> proxy === arguments?.get(0)
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> label
                else -> action(method.name, arguments)
            }
        }

    private fun type(name: String): Class<*> = loader.loadClass(name)

    private fun isControlFlow(failure: Throwable): Boolean =
        type("com.intellij.openapi.diagnostic.ControlFlowException").isInstance(failure)

    private fun cancelAnalysis(): Nothing = throw type("com.intellij.openapi.progress.ProcessCanceledException")
        .getConstructor().newInstance() as Throwable

    companion object {
        private const val STRUCTURE_PROVIDER =
            "org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider"

        val instance: NavigationHost? by lazy {
            val loader = KtPsiFactory::class.java.classLoader
            try {
                loader.loadClass(STRUCTURE_PROVIDER)
            } catch (_: ClassNotFoundException) {
                return@lazy null
            }
            NavigationHost(loader)
        }
    }
}

private data class ModuleContext(val module: Any, val dangling: Boolean)
private data class SourceOrigin(val key: Any, val file: Any, val stamp: Long, val physical: Boolean)
private data class SourceSlot(val offset: Int, val context: Boolean)
private class SourceGeneration(val origin: SourceOrigin, var module: Any) {
    val files = linkedMapOf<SourceSlot, SourceFile>()
    val copies = mutableListOf<WeakReference<KtFile>>()
}
private data class SourceFile(val file: KtFile, val name: String, val text: String)
private data class Publication(val revision: Long, val modules: List<Any>)

/** In particular, preserve the IDE's cancellation exception instead of wrapping control flow. */
private fun Method.callHost(receiver: Any?, vararg arguments: Any?): Any? = try {
    invoke(receiver, *arguments)
} catch (failure: InvocationTargetException) {
    throw failure.targetException
}
