package com.kotlinorm.ide

import com.intellij.facet.FacetManager
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.Computable
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.idea.base.plugin.artifacts.KotlinArtifacts
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.util.concurrent.Callable

/** Exercises stock Kotlin editor services with KEFS as the sole FIR loader. */
class KefsProjectionFixtureTest : BasePlatformTestCase() {
    private lateinit var expectedArtifactHash: String
    private lateinit var expectedClassBytes: Map<String, ByteArray>
    private fun File.sha256(): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(readBytes()).joinToString("") { "%02x".format(it) }

    override fun setUp() {
        super.setUp()
        val roots = listOf("probe.core.jar", "probe.syntax.jar", "probe.cli.jar", "probe.repository")
            .flatMap {
                val artifact = File(requireNotNull(System.getProperty(it)))
                listOf(artifact.absolutePath, artifact.canonicalPath)
            }.distinct()
        VfsRootAccess.allowRootAccess(testRootDisposable, *roots.toTypedArray())
        WriteAction.runAndWait<RuntimeException> {
            val sdk = JavaSdk.getInstance().createJdk("KEFS fixture JDK", System.getProperty("java.home"), false)
            ProjectJdkTable.getInstance().addJdk(sdk, testRootDisposable)
            ModuleRootModificationUtil.setModuleSdk(module, sdk)
        }
        configureKefs()
        PsiTestUtil.addLibrary(testRootDisposable, module, "kotlin-stdlib", "",
            KotlinArtifacts.kotlinStdlibPath.toString())
        for (name in listOf("core", "syntax")) {
            PsiTestUtil.addLibrary(testRootDisposable, module, "kronos-$name", "",
                System.getProperty("probe.$name.jar"))
        }
        WriteAction.runAndWait<RuntimeException> {
            val type = KotlinFacetType.INSTANCE
            val configuration = type.createDefaultConfiguration()
            configuration.settings.apply {
                useProjectSettings = false
                compilerArguments = K2JVMCompilerArguments().apply {
                    languageVersion = "2.4"
                    apiVersion = "2.4"
                    pluginClasspaths = arrayOf(System.getProperty("probe.cli.jar"))
                }
                compilerSettings = CompilerSettings().apply {
                    additionalArguments = "-Xcollection-literals"
                }
                updateMergedArguments()
            }
            val manager = FacetManager.getInstance(module)
            manager.createModifiableModel().apply {
                addFacet(manager.createFacet(type, "Kotlin", configuration, null))
                commit()
            }
        }
        // Load KEFS before creating the source whose editor behavior is under test.
        awaitFir(myFixture.configureByText("PluginLoading.kt", "package probe.loading") as KtFile)
    }

    override fun tearDown() {
        try {
            WriteAction.runAndWait<RuntimeException> {
                val manager = FacetManager.getInstance(module)
                val facets = manager.getFacetsByType(KotlinFacetType.INSTANCE.id)
                manager.createModifiableModel().apply {
                    facets.forEach { removeFacet(it) }
                    commit()
                }
            }
        } finally {
            super.tearDown()
        }
    }

    fun testProjectionAliasCompletionAndCollectionSyntax() {
        val file = myFixture.configureByText("Projection.kt", """
            import com.kotlinorm.annotations.Table
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.orm.select.select
            @Table("people") data class Person(var id: Int? = null, var name: String? = null) : KPojo
            fun projection() {
                val row = Person().select { [it.id.alias("uid"), it.name] }.first()
                row.<caret>
            }
        """.trimIndent()) as KtFile
        awaitFir(file)
        val completions = myFixture.completeBasic()?.map { it.lookupString }.orEmpty()
        assertTrue("Stock Kotlin completion did not expose uid: $completions", "uid" in completions)
        assertTrue("Stock Kotlin completion did not expose name: $completions", "name" in completions)
    }

    fun testProjectionTypesAndBracketSyntaxAreRecognized() {
        val file = myFixture.configureByText("ProjectionTypes.kt", """
            import com.kotlinorm.annotations.Table
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.orm.select.select
            @Table("people") data class Person(var id: Int? = null, var name: String? = null) : KPojo
            fun projection() {
                val row = Person().select { [it.id.alias("uid"), it.name] }.first()
                val uid: Int? = row.uid
                val name: String? = row.name
                println(uid to name)
            }
        """.trimIndent()) as KtFile
        awaitFir(file)
        val errors = myFixture.doHighlighting().filter {
            it.severity == com.intellij.lang.annotation.HighlightSeverity.ERROR
        }
        assertTrue("Unexpected stock editor errors: ${errors.map { it.description }}", errors.isEmpty())
    }

    fun testFirDiagnosticIsReportedWithoutKronosEditorExtensions() {
        val file = myFixture.configureByText("DuplicateProjection.kt", """
            import com.kotlinorm.annotations.Table
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.orm.join.join
            @Table("people") data class Person(var id: Int? = null, var companyId: Int? = null) : KPojo
            @Table("companies") data class Company(var id: Int? = null) : KPojo
            fun duplicate() {
                Person().join(Company()) { person, company ->
                    leftJoin { person.companyId == company.id }.select { [person.id, company.id] }
                }
            }
        """.trimIndent()) as KtFile
        awaitFir(file)
        val errors = myFixture.doHighlighting().filter {
            it.severity == com.intellij.lang.annotation.HighlightSeverity.ERROR
        }
        assertTrue("Kronos duplicate-field diagnostic missing: ${errors.map { it.description }}", errors.any {
            listOfNotNull(it.description, it.toolTip).joinToString(" ").contains("Projection output names conflict")
        })
        val conflictStart = myFixture.editor.document.text.indexOf("company.id]", myFixture.editor.document.text.indexOf(".select")) + "company.".length
        val conflict = errors.single {
            listOfNotNull(it.description, it.toolTip).joinToString(" ").contains("Projection output names conflict")
        }
        assertEquals(conflictStart, conflict.startOffset)
        assertEquals(conflictStart + "id".length, conflict.endOffset)
    }


    fun testJoinProjectionTypes() = assertNoEditorErrors("JoinTypes.kt", """
        val row = Person().join(Company()) { person, company ->
            leftJoin { person.companyId == company.id }
                .select { [person.id.alias("uid"), company.name.alias("companyName")] }
        }.first()
        val uid: Int? = row.uid
        val companyName: String? = row.companyName
    """)

    fun testJoinProjectionEmptySelectorCompletion() = assertCompletion("JoinCompletion.kt", """
        val row = Person().join(Company()) { person, company ->
            leftJoin { person.companyId == company.id }
                .select { [person.id.alias("uid"), company.name.alias("companyName")] }
        }.first()
        row.<caret>
    """, setOf("uid", "companyName"), setOf("id", "companyId"))

    fun testReselectedAliasTypes() = assertNoEditorErrors("Reselected.kt", """
        val row = Person()
            .select { [it.id.alias("uid"), it.name.alias("displayName"), it.age] }
            .select { [it.uid.alias("recordId"), it.displayName] }
            .first()
        val id: Int? = row.recordId
        val name: String? = row.displayName
    """)

    fun testIndependentProjectionAliasesDoNotLeak() = assertCompletion("Independent.kt", """
        val left = Person().select { it.id.alias("uid") }.first()
        val right = Person().select { it.id.alias("key") }.first()
        val uid: Int? = left.uid
        right.<caret>
    """, setOf("key"), setOf("uid", "id", "name"))

    fun testDerivedWindowAliasTypes() = assertNoEditorErrors("WindowTypes.kt", """
        val ranked = Person().select {
            [it.id, f.rowNumber().over { partitionBy(it.age); orderBy(it.createdAt.desc()) }.alias("rn")]
        }
        val row = ranked.select().where { it.rn == 1 }.first()
        val rank: Int? = row.rn
    """)

    fun testWindowAliasEmptySelectorCompletion() = assertCompletion("WindowCompletion.kt", """
        val ranked = Person().select {
            [it.id, f.rowNumber().over { partitionBy(it.age); orderBy(it.createdAt.desc()) }.alias("rn")]
        }
        ranked.select().where { it.<caret> }
    """, setOf("id", "rn"))

    fun testScalarSubqueryAliasTypes() = assertNoEditorErrors("ScalarSubquery.kt", """
        val row = Person().select {
            [it.id, Company().select { company -> company.id }.limit(1).alias("companyKey")]
        }.first()
        val key: Int? = row.companyKey
    """)

    fun testFilterSelectedAliasCompletion() = assertCompletion("FilterCompletion.kt", """
        Person().select { [it.id.alias("uid")] }.filter { it.<caret> }
    """, setOf("uid"), setOf("id", "name", "companyId"))

    fun testCollectionLiteralsAcrossProjectionGroupingAndSorting() = assertNoEditorErrors("Collections.kt", """
        Person().select { [it.id, it.name] }
            .groupBy { [it.id, it.name] }
            .orderBy { [it.id.desc(), it.name.asc()] }
    """)

    fun testNullableAndListProjectionTypes() = assertNoEditorErrors("Containers.kt", """
        val query = Person().select { [it.id.alias("uid")] }
        val optional: Int? = query.firstOrNull()?.uid
        val list: Int? = query.toList().single().uid
    """)

    fun testGenericProjectionPropertyType() = assertNoEditorErrors("GenericProperty.kt", """
        val row = Person().select { [it.id, it.tags.alias("labels")] }.first()
        val labels: List<String?>? = row.labels
    """)

    fun testIdentityProjectionKeepsSourceType() = assertNoEditorErrors("Identity.kt", """
        val row = Person().select { it }.first()
        val person: Person = row
        val name: String? = row.name
    """)

    fun testFunctionProjectionRequiresAliasDiagnostic() = assertEditorDiagnostic("MissingAlias.kt", """
        Person().select { [it.id, f.length(it.name)] }
    """, "Non-field select item must declare .alias", "f.length(it.name)")

    fun testScalarSubqueryRequiresLimitDiagnostic() = assertEditorDiagnostic("MissingLimit.kt", """
        Person().select { [it.id, Company().select { company -> company.id }.alias("companyKey")] }
    """, "Scalar subquery must explicitly use limit(1)", "Company().select")

    fun testContextAliasShadowRequiresOptInDiagnostic() = assertEditorDiagnostic("ContextShadow.kt", """
        Person().select { [it.id, f.length(it.name).alias("name")] }.orderBy { it.name.desc() }
    """, "Projection output names conflict", "name.desc()")

    fun testSameLayerWhereCannotReadWindowAlias() = assertEditorDiagnostic("WindowLayer.kt", """
        Person().select {
            [it.id, f.rowNumber().over { orderBy(it.id.asc()) }.alias("rn")]
        }.where { it.rn == 1 }
    """, "Unresolved reference 'rn'", "rn == 1")

    fun testProjectionPropertyQuickDocumentation() = assertPropertyDocumentation("PropertyDocumentation.kt", """
        val row = Person().select { [it.id.alias("uid"), it.name] }.first()
        println(row.u<caret>id)
    """, "uid")

    fun testWindowAliasPropertyQuickDocumentation() = assertPropertyDocumentation("WindowAliasDocumentation.kt", """
        Person().select {
            [it.id, f.rowNumber().over { orderBy(it.id.asc()) }.alias("rn")]
        }.orderBy { it.r<caret>n.asc() }
    """, "rn")

    private fun assertPropertyDocumentation(name: String, body: String, propertyName: String) {
        val file = sourceFile(name, body)
        awaitFir(file)
        val html = quickDocumentation(file)
        println("KEFS property quick documentation: $html")
        val definition = Regex("(?s)<div class=['\"]definition['\"]>(.*?)</div>")
            .find(html)?.groupValues?.get(1) ?: error("No documentation definition: $html")
        val text = com.intellij.openapi.util.text.StringUtil.unescapeXmlEntities(
            definition.replace(Regex("<[^>]+>"), "")).replace(Regex("\\s+"), " ").trim()
        assertEquals("public final var $propertyName: Int?", text)
    }

    fun testSourcePropertyQuickDocumentation() {
        val file = sourceFile("SourceDocumentation.kt", """
            val ordinary: Int? = 1
            println(ord<caret>inary)
        """)
        awaitFir(file)
        val html = quickDocumentation(file)
        println("KEFS source quick documentation: $html")
        assertFalse("Source property documentation is empty", html.isBlank())
    }

    fun testProjectionReceiverQuickDocumentation() {
        val file = sourceFile("ReceiverDocumentation.kt", """
            Person().select { [it.id.alias("uid"), it.name] }
                .orderBy { i<caret>t.uid.asc() }
        """)
        awaitFir(file)
        val html = quickDocumentation(file)
        println("KEFS receiver quick documentation: $html")
        assertFalse("Receiver documentation is empty", html.isBlank())
        assertFalse("Receiver documentation contains an unresolved type: $html", html.contains("ERROR CLASS"))
    }

    private fun quickDocumentation(file: KtFile): String {
        val offset = myFixture.caretOffset
        return ApplicationManager.getApplication().executeOnPooledThread(Callable {
            ApplicationManager.getApplication().runReadAction(Computable {
                val targets = IdeDocumentationTargetProvider.getInstance(project)
                    .documentationTargets(myFixture.editor, file, offset)
                assertTrue("Stock Kotlin documentation target missing at $offset", targets.isNotEmpty())
                val target = targets.first()
                assertTrue("Unexpected documentation target: ${target.javaClass.name}",
                    target.javaClass.name.startsWith("org.jetbrains.kotlin."))
                val result = target.computeDocumentation()
                assertTrue("Stock Kotlin documentation missing: $result", result is DocumentationData)
                (result as DocumentationData).html
            })
        }).get()
    }

    fun testProjectionPropertyGoToDeclaration() = assertDeclarationNavigation("PropertyNavigation.kt", """
        val row = Person().select { [it.id.alias("uid"), it.name] }.first()
        println(row.u<caret>id)
    """, "uid")

    fun testWindowAliasPropertyGoToDeclaration() = assertDeclarationNavigation("WindowAliasNavigation.kt", """
        Person().select {
            [it.id, f.rowNumber().over { orderBy(it.id.asc()) }.alias("rn")]
        }.orderBy { it.r<caret>n.asc() }
    """, "rn")

    private fun assertDeclarationNavigation(name: String, body: String, expectedName: String) {
        val file = sourceFile(name, body)
        awaitFir(file)
        val targets = ApplicationManager.getApplication().executeOnPooledThread(Callable {
            ApplicationManager.getApplication().runReadAction(Computable {
                GotoDeclarationAction.findAllTargetElements(project, myFixture.editor, myFixture.caretOffset)
                    .map { it.navigationElement }
            })
        }).get()
        println("KEFS declaration targets: ${targets.map { "${it.javaClass.name}: ${it.text}" }}")
        assertEquals("Expected one declaration for $expectedName", 1, targets.size)
        val property = targets.single() as? org.jetbrains.kotlin.psi.KtProperty
            ?: error("Expected a readable Kotlin property, got ${targets.single().javaClass.name}")
        assertEquals(expectedName, property.name)
        assertEquals("Int?", property.typeReference?.text?.removePrefix("kotlin."))
        assertTrue("Declaration cannot be opened", property.containingFile.virtualFile.isValid)
        assertTrue("Declaration cannot be navigated", property.canNavigate())
        val manager = FileEditorManager.getInstance(project)
        val targetFile = property.containingFile.virtualFile
        try {
            val editor = manager.openTextEditor(OpenFileDescriptor(project, targetFile, property.textOffset), false)
            assertNotNull("Declaration did not open in an editor", editor)
            assertTrue("Opened editor does not show the declaration", editor!!.document.text.contains(property.text))
        } finally {
            manager.closeFile(targetFile)
        }
    }


    fun testSharedProjectSettingsRoundTrip() {
        val plugin = requireNotNull(PluginManagerCore.getPlugin(PluginId.getId("com.github.mr3zee.kotlinPlugins")))
        val loader = requireNotNull(plugin.pluginClassLoader)
        fun type(name: String) = loader.loadClass("com.github.mr3zee.kefs.$name")
        val repositoryType = type("KotlinArtifactsRepository")
        val kindType = type("KotlinArtifactsRepository\$Type")
        fun repository(name: String, value: String, kind: String) = repositoryType.constructors
            .single { it.parameterCount == 3 }.newInstance(name, value, kindType.enumConstants.single { it.toString() == kind })
        val repositories = listOf(
            repository("Kronos Bootstrap", "${'$'}PROJECT_DIR${'$'}/build/repository/bootstrap", "PATH"),
            repository("Kronos Maven Central", "https://repo.maven.apache.org/maven2", "URL"),
            repository("Sonatype Snapshots", "https://central.sonatype.com/repository/maven-snapshots", "URL"),
        )
        val id = type("MavenId").getConstructor(String::class.java)
            .newInstance("com.kotlinorm:kronos-compiler-plugin")
        val exact = type("KotlinPluginDescriptor\$VersionMatching").enumConstants.single { it.toString() == "EXACT" }
        val descriptor = type("KotlinPluginDescriptor").constructors.single { it.parameterCount == 7 }
            .newInstance("Kronos", listOf(id), exact, true, false, repositories, null)
        val stateType = type("KefsSettings\$State")
        val state = stateType.constructors.single { it.parameterCount == 2 }.newInstance(repositories, listOf(descriptor))
        val serializer = type("KefsSettingsSerializerKt")
        val stored = serializer.getMethod("asStored", stateType).invoke(null, state)
        val component = com.intellij.util.xmlb.XmlSerializer.serialize(stored).apply {
            name = "component"
            setAttribute("name", "KotlinPluginsSettings")
        }
        val projectState = org.jdom.Element("project").setAttribute("version", "4").addContent(component)
        val output = File(System.getProperty("probe.settings.output"))
        output.parentFile.mkdirs()
        com.intellij.openapi.util.JDOMUtil.write(projectState, output.toPath(), "\n")
        val expanded = component.clone()
        com.intellij.openapi.components.PathMacroManager.getInstance(project).expandPaths(expanded)
        val storedType = type("KefsSettings\$StoredState")
        val restored = com.intellij.util.xmlb.XmlSerializer.deserialize(expanded, storedType)
        var roundTrip = serializer.getMethod("asState", storedType).invoke(null, restored)
        // Exercise the same normalization and persistence cycle used by KefsSettings.safeState().
        repeat(2) {
            val defaults = serializer.getMethod("withDefaults", stateType).invoke(null, roundTrip)
            val compact = serializer.getMethod("withoutDefaults", stateType).invoke(null, defaults)
            val persisted = serializer.getMethod("asStored", stateType).invoke(null, compact)
            roundTrip = serializer.getMethod("asState", storedType).invoke(null, persisted)
        }
        val plugins = stateType.getMethod("getPlugins").invoke(roundTrip) as List<*>
        val actual = requireNotNull(plugins.single { it!!.javaClass.getMethod("getName").invoke(it) == "Kronos" })
        assertEquals("Kronos", actual.javaClass.getMethod("getName").invoke(actual))
        assertEquals("EXACT", actual.javaClass.getMethod("getVersionMatching").invoke(actual).toString())
        assertEquals(true, actual.javaClass.getMethod("getEnabled").invoke(actual))
        assertEquals(false, actual.javaClass.getMethod("getIgnoreExceptions").invoke(actual))
        val ids = actual.javaClass.getMethod("getIds").invoke(actual) as List<*>
        assertEquals("com.kotlinorm:kronos-compiler-plugin", ids.single()!!.javaClass.getMethod("getId").invoke(ids.single()))
        val restoredRepos = actual.javaClass.getMethod("getRepositories").invoke(actual) as List<*>
        assertEquals(3, restoredRepos.size)
        val local = restoredRepos.single { it!!.javaClass.getMethod("getName").invoke(it) == "Kronos Bootstrap" }!!
        val path = local.javaClass.getMethod("getValue").invoke(local).toString()
        assertFalse("Project path macro was not expanded: $path", path.contains("PROJECT_DIR"))
        assertTrue("Local repository is not project relative: $path", path.startsWith(requireNotNull(project.basePath)))
    }

    private fun sourceFile(name: String, body: String): KtFile = myFixture.configureByText(name, """
        import com.kotlinorm.annotations.Table
        import com.kotlinorm.interfaces.KPojo
        import com.kotlinorm.orm.select.select
        import com.kotlinorm.orm.select.filter
        import com.kotlinorm.orm.join.join
        import com.kotlinorm.functions.bundled.exts.StringFunctions.length
        import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
        @Table("people") data class Person(
            var id: Int? = null, var name: String? = null, var companyId: Int? = null,
            var age: Long? = null, var createdAt: Long? = null,
            var tags: List<String?>? = null,
        ) : KPojo
        @Table("companies") data class Company(var id: Int? = null, var name: String? = null) : KPojo
        fun projection() {
            ${body.trimIndent()}
        }
    """.trimIndent()) as KtFile

    private fun assertNoEditorErrors(name: String, body: String) {
        val file = sourceFile(name, body)
        awaitFir(file)
        val errors = editorErrors()
        assertTrue("Unexpected stock editor errors: ${errors.map { it.description }}", errors.isEmpty())
    }

    private fun assertCompletion(name: String, body: String, expected: Set<String>, absent: Set<String> = emptySet()) {
        val file = sourceFile(name, body)
        awaitFir(file)
        val names = myFixture.completeBasic()?.map { it.lookupString }.orEmpty().toSet()
        assertTrue("Missing projection fields ${expected - names}; completions=$names", names.containsAll(expected))
        assertTrue("Unselected fields exposed: ${names intersect absent}", (names intersect absent).isEmpty())
    }

    private fun assertEditorDiagnostic(name: String, body: String, message: String, sourceText: String) {
        val file = sourceFile(name, body)
        awaitFir(file)
        val errors = editorErrors()
        val expected = errors.filter {
            listOfNotNull(it.description, it.toolTip).joinToString(" ").contains(message)
        }
        assertEquals("Expected one '$message', actual=${errors.map { it.description }}", 1, expected.size)
        val start = myFixture.editor.document.text.lastIndexOf(sourceText)
        assertTrue("Diagnostic source text missing: $sourceText", start >= 0)
        assertTrue("Diagnostic is not attached to $sourceText: ${expected.single()}",
            expected.single().startOffset < start + sourceText.length && expected.single().endOffset > start)
        assertTrue("Unexpected additional errors: ${errors.filterNot { it in expected }.map { it.description }}",
            errors.size == expected.size)
    }

    private fun editorErrors() = myFixture.doHighlighting().filter {
        it.severity == com.intellij.lang.annotation.HighlightSeverity.ERROR
    }

    @OptIn(KaPlatformInterface::class)
    private fun awaitFir(file: KtFile) {
        val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(60)
        var names = emptyList<String>()
        while (System.nanoTime() < deadline) {
            names = ApplicationManager.getApplication().executeOnPooledThread(Callable {
                ApplicationManager.getApplication().runReadAction(Computable {
                    analyze(file) {
                        KotlinCompilerPluginsProvider.getInstance(project)
                            ?.getRegisteredExtensions(useSiteModule, FirExtensionRegistrarAdapter)
                            ?.map {
                                if (it.javaClass.name == "com.kotlinorm.compiler.fir.KronosFirExtensionRegistrar") {
                                    val loader = it.javaClass.classLoader
                                    val mismatched = expectedClassBytes.keys.filter { name ->
                                        val loadedBytes = loader.getResourceAsStream(name)?.use { stream -> stream.readBytes() }
                                        loadedBytes == null || !loadedBytes.contentEquals(expectedClassBytes.getValue(name))
                                    }
                                    if (mismatched.isNotEmpty()) return@map "STALE:${it.javaClass.name}:$mismatched"
                                }
                                it.javaClass.name
                            }.orEmpty()
                    }
                })
            }).get()
            if ("com.kotlinorm.compiler.fir.KronosFirExtensionRegistrar" in names) {
                println("KEFS loaded byte-identical compiler classes (${expectedClassBytes.size}), artifact SHA-256=$expectedArtifactHash")
                return
            }
            UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(100)
        }
        fail("KEFS did not load Kronos FIR registrar; actual registrars: $names")
    }

    private fun configureKefs() {
        val plugin = requireNotNull(PluginManagerCore.getPlugin(PluginId.getId("com.github.mr3zee.kotlinPlugins")))
        val loader = requireNotNull(plugin.pluginClassLoader)
        fun type(name: String) = loader.loadClass("com.github.mr3zee.kefs.$name")
        val versions = requireNotNull(ApplicationManager.getApplication().getService(type("KotlinVersionService")))
        val compilerVersion = versions.javaClass.getMethod("getKotlinIdePluginVersion").invoke(versions)
        println("KEFS actual IDE Kotlin compiler: $compilerVersion")
        val expectedJar = File(System.getProperty("probe.repository"),
            "com/kotlinorm/kronos-compiler-plugin/$compilerVersion-${System.getProperty("probe.library.version")}/kronos-compiler-plugin-$compilerVersion-${System.getProperty("probe.library.version")}.jar")
        expectedArtifactHash = expectedJar.sha256()
        expectedClassBytes = java.util.zip.ZipFile(expectedJar).use { archive ->
            archive.entries().asSequence().filter {
                it.name.endsWith(".class") && (
                    it.name.startsWith("com/kotlinorm/compiler/fir/") ||
                    it.name == "com/kotlinorm/compiler/plugin/KronosCompilerPluginRegistrar.class" ||
                    it.name == "com/kotlinorm/compiler/plugin/KronosCommandLineProcessor.class"
                )
            }.associate { entry -> entry.name to archive.getInputStream(entry).use { it.readBytes() } }
        }
        check(expectedClassBytes.isNotEmpty())
        val settings = requireNotNull(project.getService(type("KefsSettings")))
        val pathType = type("KotlinArtifactsRepository\$Type").enumConstants.single { it.toString() == "PATH" }
        val repository = type("KotlinArtifactsRepository").constructors.single { it.parameterCount == 3 }
            .newInstance("kronos-probe", System.getProperty("probe.repository"), pathType)
        val id = type("MavenId").getConstructor(String::class.java)
            .newInstance("com.kotlinorm:kronos-compiler-plugin")
        val exact = type("KotlinPluginDescriptor\$VersionMatching").enumConstants.single { it.toString() == "EXACT" }
        val descriptor = type("KotlinPluginDescriptor").constructors.single { it.parameterCount == 7 }
            .newInstance("Kronos probe ${expectedArtifactHash.take(16)}", listOf(id), exact, true, false, listOf(repository), null)
        settings.javaClass.getMethod("updateToNewState", List::class.java, List::class.java)
            .invoke(settings, listOf(repository), listOf(descriptor))
    }
}
