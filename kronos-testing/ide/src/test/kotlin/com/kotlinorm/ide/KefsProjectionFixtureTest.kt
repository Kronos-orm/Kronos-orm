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
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventListener
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleStateModificationKind
import org.jetbrains.kotlin.analysis.api.platform.modification.publishModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.resolveExtensionFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
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
        configureEditorProject(myFixture)
        // Load KEFS before creating the source whose editor behavior is under test.
        // Loading a source module does not need an editor. Keeping warmup editor-free
        // also lets an independent project fixture enforce its real editor lifetime.
        awaitFir(myFixture.addFileToProject("PluginLoading.kt", "package probe.loading") as KtFile)
    }

    private fun configureEditorProject(fixture: com.intellij.testFramework.fixtures.CodeInsightTestFixture) {
        val project = fixture.project
        val module = fixture.module
        val disposable = fixture.testRootDisposable
        val roots = listOf("probe.core.jar", "probe.syntax.jar", "probe.cli.jar", "probe.repository")
            .flatMap {
                val artifact = File(requireNotNull(System.getProperty(it)))
                listOf(artifact.absolutePath, artifact.canonicalPath)
            }.distinct()
        VfsRootAccess.allowRootAccess(disposable, *roots.toTypedArray())
        WriteAction.runAndWait<RuntimeException> {
            val sdk = JavaSdk.getInstance().createJdk("KEFS fixture JDK ${project.locationHash}", System.getProperty("java.home"), false)
            ProjectJdkTable.getInstance().addJdk(sdk, disposable)
            ModuleRootModificationUtil.setModuleSdk(module, sdk)
        }
        configureKefs(project)
        PsiTestUtil.addLibrary(disposable, module, "kotlin-stdlib", "",
            KotlinArtifacts.kotlinStdlibPath.toString())
        for (name in listOf("core", "syntax")) {
            PsiTestUtil.addLibrary(disposable, module, "kronos-$name", "",
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
        assertPropertyDocumentation(file, propertyName, "Int?")
    }

    private fun assertPropertyDocumentation(file: KtFile, propertyName: String, expectedType: String) {
        val html = quickDocumentation(file)
        println("KEFS property quick documentation: $html")
        val definition = Regex("(?s)<div class=['\"]definition['\"]>(.*?)</div>")
            .find(html)?.groupValues?.get(1) ?: error("No documentation definition: $html")
        val text = com.intellij.openapi.util.text.StringUtil.unescapeXmlEntities(
            definition.replace(Regex("<[^>]+>"), "")).replace(Regex("\\s+"), " ").trim()
        assertEquals("public final var $propertyName: $expectedType", text)
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
        assertDeclarationNavigation(file, expectedName, "Int?")
    }

    @OptIn(KaPlatformInterface::class)
    private fun assertDeclarationNavigation(file: KtFile, expectedName: String, expectedType: String) {
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
        assertEquals(expectedType, property.typeReference?.text?.removePrefix("kotlin."))
        ApplicationManager.getApplication().executeOnPooledThread(Callable {
            ApplicationManager.getApplication().runReadAction(Computable {
                analyze(file) {
                    assertSame("Generated navigation file must resolve in its source module", useSiteModule,
                        KotlinProjectStructureProvider.getModule(project, property, useSiteModule))
                    val unrelated = org.jetbrains.kotlin.psi.KtPsiFactory.contextual(file)
                        .createPhysicalFile("UnrelatedNavigation.kt", "class UnrelatedNavigation")
                    assertFalse("Navigation support must not admit unrelated temporary files",
                        analysisScope.contains(unrelated.virtualFile))
                }
                analyze(property) {
                    assertEquals(expectedName, property.symbol.name.asString())
                    assertNotNull("Generated navigation property must support stock analysis", property.symbol.returnType)
                }
            })
        }).get()
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


    fun testCompiledEntityProjectionTypesAndNavigation() {
        val root = java.nio.file.Files.createTempDirectory("kronos-kefs-compiled-entity").toFile()
        try {
            VfsRootAccess.allowRootAccess(testRootDisposable, root.absolutePath)
            val source = File(root, "BinaryPerson.kt").apply {
                writeText("""
                    package binary.fixture
                    import com.kotlinorm.interfaces.KPojo
                    data class BinaryPerson(var id: Int? = null, var name: String? = null) : KPojo
                """.trimIndent())
            }
            val binary = File(root, "binary-entity.jar")
            compileEntityFixture(source, binary)
            PsiTestUtil.addLibrary(testRootDisposable, module, "binary-entity", "", binary.absolutePath)
            val file = sourceFile("BinaryEntityProjection.kt", """
                val row = binary.fixture.BinaryPerson().select { [it.id.alias("uid"), it.name] }.first()
                val value: Int? = row.u<caret>id
            """)
            awaitFir(file)
            val errors = editorErrors()
            assertTrue("Binary model projection has editor errors: $errors", errors.isEmpty())
            assertPropertyDocumentation(file, "uid", "Int?")
            assertDeclarationNavigation(file, "uid", "Int?")
        } finally {
            root.deleteRecursively()
        }
    }

    /** Compile outside the IDE process so its compiler loader never sees CLI compiler classes. */
    private fun compileEntityFixture(source: File, binary: File) {
        val home = File(requireNotNull(System.getProperty("kronos.test.kotlin.home")))
        check(File(home, "lib/kotlin-compiler.jar").isFile) { "Kotlin compiler SDK is missing at $home" }
        val javaName = if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java"
        val javaExecutable = File(System.getProperty("java.home"), "bin/$javaName")
        val classpath = listOf(System.getProperty("probe.core.jar"), System.getProperty("probe.syntax.jar"),
            File(home, "lib/kotlin-stdlib.jar").absolutePath).joinToString(File.pathSeparator)
        val output = File(binary.parentFile, "compiler.log")
        val process = ProcessBuilder(javaExecutable.absolutePath, "-Dkotlin.home=${home.absolutePath}",
            "-classpath", File(home, "lib/*").absolutePath, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-no-stdlib", "-no-reflect", "-classpath", classpath, "-jvm-target", "17",
            "-d", binary.absolutePath, source.absolutePath)
            .redirectErrorStream(true).redirectOutput(output).start()
        try {
            process.outputStream.close()
            val completed = process.waitFor(90, java.util.concurrent.TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            }
            val diagnostics = output.takeIf(File::isFile)?.readText()?.takeLast(12000).orEmpty()
            assertTrue("Binary fixture compiler timed out: $diagnostics", completed)
            assertEquals("Binary fixture compilation failed: $diagnostics", 0, process.exitValue())
            assertTrue("Binary fixture did not produce a JAR: $diagnostics", binary.isFile)
        } finally {
            if (process.isAlive) {
                process.destroyForcibly()
                process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    fun testEditedProjectionIsReanalyzed() {
        val file = sourceFile("EditedProjection.kt", """
            val row = Person().select { [it.id.alias("uid")] }.first()
            val value: Int? = row.u<caret>id
        """)
        awaitFir(file)
        assertPropertyDocumentation(file, "uid", "Int?")
        val document = myFixture.editor.document
        val changed = document.text.replace("it.id.alias(\"uid\")", "it.name.alias(\"label\")")
            .replace("val value: Int? = row.uid", "val value: String? = row.label")
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            document.setText(changed)
            com.intellij.psi.PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        myFixture.editor.caretModel.moveToOffset(changed.lastIndexOf("row.label") + "row.la".length)
        awaitFir(file)
        val errors = editorErrors()
        assertTrue("Edited projection has editor errors: $errors", errors.isEmpty())
        assertPropertyDocumentation(file, "label", "String?")
        assertDeclarationNavigation(file, "label", "String?")
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            val start = document.text.lastIndexOf("row.label") + "row.".length
            document.deleteString(start, start + "label".length)
            com.intellij.psi.PsiDocumentManager.getInstance(project).commitDocument(document)
            myFixture.editor.caretModel.moveToOffset(start)
        }
        awaitFir(file)
        val names = myFixture.completeBasic()?.map { it.lookupString }.orEmpty().toSet()
        assertTrue("Updated alias is missing: $names", "label" in names)
        assertFalse("Old alias survived reanalysis: $names", "uid" in names)
    }

    fun testColdProjectionHighlightingDocumentationAndNavigation() {
        val file = sourceFile("ColdProjection.kt", """
            val row = Person().select { [it.id.alias("uid")] }.first()
            val value: Int? = row.u<caret>id
        """)
        // Exercise the daemon's first-analysis path without prewarming FIR diagnostics.
        val errors = editorErrors()
        assertTrue("Cold projection has editor errors: $errors", errors.isEmpty())
        assertPropertyDocumentation(file, "uid", "Int?")
        assertDeclarationNavigation(file, "uid", "Int?")
    }

    fun testNavigationSourcesFollowEditsDeletionAndRecreation() {
        val template = sourceText("""
            val row = Person().select { [it.id.alias("uid0")] }.first()
            val value: Int? = row.uid0
        """)
        var file = myFixture.addFileToProject("navigation/Editable.kt", template) as KtFile
        val previousFiles = mutableListOf<com.intellij.openapi.vfs.VirtualFile>()
        for (iteration in 0..4) {
            myFixture.configureFromExistingVirtualFile(file.virtualFile)
            myFixture.editor.caretModel.moveToOffset(file.text.lastIndexOf("row.uid$iteration") + "row.u".length)
            awaitFir(file)
            assertTrue("Edited projection has editor errors", editorErrors().isEmpty())
            assertDeclarationNavigation(file, "uid$iteration", "Int?")
            assertOutsideAnalysisScope(file, previousFiles)
            previousFiles += navigationFileAtCaret()
            if (iteration < 4) {
                com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
                    val document = myFixture.editor.document
                    document.setText(document.text.replace("uid$iteration", "uid${iteration + 1}"))
                    com.intellij.psi.PsiDocumentManager.getInstance(project).commitDocument(document)
                }
            }
        }
        FileEditorManager.getInstance(project).closeFile(file.virtualFile)
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) { file.delete() }
        UIUtil.dispatchAllInvocationEvents()
        val probe = myFixture.addFileToProject("navigation/Probe.kt", "class NavigationProbe") as KtFile
        awaitFir(probe)
        assertOutsideAnalysisScope(probe, previousFiles)
        file = myFixture.addFileToProject("navigation/Editable.kt", template) as KtFile
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        myFixture.editor.caretModel.moveToOffset(file.text.lastIndexOf("row.uid0") + "row.u".length)
        awaitFir(file)
        assertTrue("Recreated projection has editor errors", editorErrors().isEmpty())
        assertPropertyDocumentation(file, "uid0", "Int?")
        assertDeclarationNavigation(file, "uid0", "Int?")
        assertOutsideAnalysisScope(file, previousFiles)
    }

    fun testNonphysicalPreviewsKeepPhysicalNavigationStable() {
        val physical = sourceFile("PhysicalProjection.kt", """
            val row = Person().select { [it.id.alias("uid")] }.first()
            val value: Int? = row.u<caret>id
        """)
        awaitFir(physical)
        assertDeclarationNavigation(physical, "uid", "Int?")
        val originalNavigation = navigationFileAtCaret()
        // Preview PSI can itself be contextual. Follow the full context chain back
        // to the physical source while keeping each preview's output independent.
        val context = org.jetbrains.kotlin.psi.KtPsiFactory.contextual(physical)
            .createFile("PreviewContext.kt", "val previewContext = 0")
        assertNull(context.virtualFile)
        repeat(3) { index ->
            val alias = "previewLabel$index"
            val text = """
                import com.kotlinorm.orm.select.select
                fun previewProjection$index() {
                    val row = Person().select { [it.name.alias("$alias")] }.first()
                    val value: String? = row.$alias
                    println(value)
                }
            """.trimIndent()
            val preview = org.jetbrains.kotlin.psi.KtPsiFactory.contextual(context)
                .createFile("Preview$index.kt", text)
            assertFalse(preview.isPhysical)
            assertNull(preview.virtualFile)
            awaitFir(preview)
            val declaration = resolvedProjectionProperty(preview, text.lastIndexOf("row.$alias") + "row.".length)
            assertEquals(alias, declaration.name)
            assertEquals("String?", declaration.typeReference?.text?.removePrefix("kotlin."))
            assertNotSame(originalNavigation, declaration.containingFile.virtualFile)
            awaitFir(physical)
            assertSame("A preview replaced the unchanged physical declaration", originalNavigation, navigationFileAtCaret())
            assertPropertyDocumentation(physical, "uid", "Int?")
        }
        assertDeclarationNavigation(physical, "uid", "Int?")
    }

    @OptIn(KaPlatformInterface::class, KaImplementationDetail::class)
    fun testExternalModuleUpdateRebuildsNavigationSources() {
        val file = sourceFile("ModuleReloadProjection.kt", """
            val row = Person().select { [it.id.alias("uid")] }.first()
            val value: Int? = row.u<caret>id
        """)
        awaitFir(file)
        assertDeclarationNavigation(file, "uid", "Int?")
        val obsolete = navigationFileAtCaret()
        val sourceModule = requireNotNull(obsolete.resolveExtensionFileModule)
        WriteAction.runAndWait<RuntimeException> {
            sourceModule.publishModuleStateModificationEvent(KotlinModuleStateModificationKind.UPDATE)
        }
        UIUtil.dispatchAllInvocationEvents()
        assertNull("An external module update retained a stale navigation association", obsolete.resolveExtensionFileModule)
        assertOutsideAnalysisScope(file, listOf(obsolete))
        awaitFir(file)
        val refreshed = navigationFileAtCaret()
        println("KEFS module update navigation: reused=${refreshed === obsolete}, associated=${refreshed.resolveExtensionFileModule}")
        val errors = editorErrors()
        assertTrue("Reloaded projection has editor errors: $errors", errors.isEmpty())
        assertPropertyDocumentation(file, "uid", "Int?")
        assertDeclarationNavigation(file, "uid", "Int?")
        assertNotSame("Module update reused a stale declaration", obsolete, navigationFileAtCaret())
        assertOutsideAnalysisScope(file, listOf(obsolete))
    }

    @OptIn(KaPlatformInterface::class, KaImplementationDetail::class)
    fun testClosingIndependentProjectReleasesNavigationSources() {
        val factory = com.intellij.testFramework.fixtures.JavaTestFixtureFactory.getFixtureFactory()
        val builder = com.intellij.testFramework.fixtures.JavaTestFixtureFactory.createFixtureBuilder("KronosNavigationProjectLifetime")
        builder.addModule(com.intellij.testFramework.builders.JavaModuleFixtureBuilder::class.java)
        val fixture = factory.createCodeInsightFixture(builder.fixture)
        fixture.setUp()
        val independent = fixture.project
        val projectJob = requireNotNull((independent as com.intellij.openapi.components.ComponentManagerEx)
            .getCoroutineScope().coroutineContext[kotlinx.coroutines.Job])
        val navigation: com.intellij.openapi.vfs.VirtualFile
        try {
            PsiTestUtil.addSourceContentToRoots(fixture.module, fixture.tempDirFixture.findOrCreateDir(""))
            configureEditorProject(fixture)
            awaitFir(fixture.addFileToProject("PluginLoading.kt", "package probe.loading") as KtFile)
            UIUtil.dispatchAllInvocationEvents()
            val file = fixture.configureByText("ClosingProjection.kt", sourceText("""
                val row = Person().select { [it.id.alias("uid")] }.first()
                val value: Int? = row.u<caret>id
            """)) as KtFile
            awaitFir(file)
            val errors = fixture.doHighlighting().filter { it.severity == com.intellij.lang.annotation.HighlightSeverity.ERROR }
            assertTrue("Independent project has editor errors: $errors", errors.isEmpty())
            val declaration = resolvedProjectionProperty(file, fixture.caretOffset)
            assertEquals("uid", declaration.name)
            navigation = declaration.containingFile.virtualFile
            assertNotNull(navigation.resolveExtensionFileModule)
        } finally {
            // A real fixture/project close invokes registered Disposer lifetimes.
            // Never dispose the shared light project or call plugin internals.
            fixture.tearDown()
        }
        assertTrue("The independent project did not close", independent.isDisposed)
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching(
            "Closed project service coroutines did not finish", { projectJob.isCompleted }, 30)
        assertTrue("Closing a project must cancel its service scope", projectJob.isCancelled)
        assertNull("Closing a project retained generated module state", navigation.resolveExtensionFileModule)
        val live = sourceFile("LiveProjectAfterClose.kt", """
            val row = Person().select { [it.id.alias("uid")] }.first()
            val value: Int? = row.u<caret>id
        """)
        awaitFir(live)
        assertPropertyDocumentation(live, "uid", "Int?")
        assertDeclarationNavigation(live, "uid", "Int?")
    }

    private fun resolvedProjectionProperty(file: KtFile, offset: Int): org.jetbrains.kotlin.psi.KtProperty =
        ApplicationManager.getApplication().executeOnPooledThread(Callable {
            ApplicationManager.getApplication().runReadAction(Computable {
                val target = file.findReferenceAt(offset)?.resolve()?.navigationElement
                val property = target as? org.jetbrains.kotlin.psi.KtProperty
                    ?: error("No readable projected declaration at $offset: $target")
                analyze(property) {
                    assertEquals(property.name, property.symbol.name.asString())
                    assertNotNull(property.symbol.returnType)
                }
                property
            })
        }).get()

    @OptIn(KaPlatformInterface::class)
    fun testTenColdProjectionsShareOneScopeInitialization() {
        UIUtil.dispatchAllInvocationEvents()
        val updates = java.util.concurrent.atomic.AtomicInteger()
        project.analysisMessageBus.connect(testRootDisposable).subscribe(KotlinModificationEvent.TOPIC,
            object : KotlinModificationEventListener {
                override fun onModification(event: KotlinModificationEvent) {
                    if (event is KotlinModuleStateModificationEvent &&
                        event.modificationKind == KotlinModuleStateModificationKind.UPDATE) updates.incrementAndGet()
                }
            })
        val declarations = (0..9).joinToString("\n") { index -> """
            val row$index = Person().select { [it.id.alias("uid$index")] }.first()
            val value$index: Int? = row$index.uid$index
        """.trimIndent() }
        val file = sourceFile("TenColdProjections.kt", declarations + "\nprintln(row9.u<caret>id9)")
        val errors = editorErrors()
        assertTrue("Ten cold projections have editor errors: $errors", errors.isEmpty())
        assertPropertyDocumentation(file, "uid9", "Int?")
        assertDeclarationNavigation(file, "uid9", "Int?")
        println("KEFS cold projection scope: projections=10 moduleUpdates=${updates.get()}")
        assertTrue("Scope initialization grew with projection count: ${updates.get()} updates", updates.get() <= 1)
    }

    private fun navigationFileAtCaret(): com.intellij.openapi.vfs.VirtualFile =
        ApplicationManager.getApplication().executeOnPooledThread(Callable {
            ApplicationManager.getApplication().runReadAction(Computable {
                GotoDeclarationAction.findAllTargetElements(project, myFixture.editor, myFixture.caretOffset)
                    .single().navigationElement.containingFile.virtualFile
            })
        }).get()

    private fun assertOutsideAnalysisScope(file: KtFile, excluded: List<com.intellij.openapi.vfs.VirtualFile>) {
        ApplicationManager.getApplication().executeOnPooledThread(Callable {
            ApplicationManager.getApplication().runReadAction(Computable {
                analyze(file) {
                    excluded.forEach { assertFalse("Obsolete navigation source remains analyzable: $it", analysisScope.contains(it)) }
                }
            })
        }).get()
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

    private fun sourceFile(name: String, body: String): KtFile =
        myFixture.configureByText(name, sourceText(body)) as KtFile

    private fun sourceText(body: String): String = """
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
    """.trimIndent()

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
            val cancellation = java.util.concurrent.atomic.AtomicBoolean()
            val analysis = ApplicationManager.getApplication().executeOnPooledThread(Callable {
                try {
                    ApplicationManager.getApplication().runReadAction(Computable {
                        analyze(file) {
                            // Complete the first FIR analysis before exercising editor services. A
                            // new generated source schedules an official module update, so normal
                            // IDE cancellation must finish before the next analysis can use its PSI.
                            file.collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                            var registrationModule = useSiteModule
                            while (registrationModule is KaDanglingFileModule) {
                                registrationModule = registrationModule.contextModule
                            }
                            KotlinCompilerPluginsProvider.getInstance(file.project)
                                ?.getRegisteredExtensions(registrationModule, FirExtensionRegistrarAdapter)
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
                } catch (failure: com.intellij.openapi.progress.ProcessCanceledException) {
                    cancellation.set(true)
                    throw failure
                }
            })
            try {
                val completed: List<String>? = analysis.get()
                if (completed == null) {
                    check(cancellation.get()) { "IDE analysis returned null without cancellation" }
                    UIUtil.dispatchAllInvocationEvents()
                    Thread.sleep(25)
                    continue
                }
                names = completed
            } catch (failure: java.util.concurrent.ExecutionException) {
                if (failure.cause !is com.intellij.openapi.progress.ProcessCanceledException) throw failure
                UIUtil.dispatchAllInvocationEvents()
                Thread.sleep(25)
                continue
            }
            if ("com.kotlinorm.compiler.fir.KronosFirExtensionRegistrar" in names) {
                println("KEFS loaded byte-identical compiler classes (${expectedClassBytes.size}), artifact SHA-256=$expectedArtifactHash")
                return
            }
            UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(100)
        }
        fail("KEFS did not load Kronos FIR registrar; actual registrars: $names")
    }

    private fun configureKefs(targetProject: com.intellij.openapi.project.Project) {
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
        val settings = requireNotNull(targetProject.getService(type("KefsSettings")))
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
