/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator.random

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.PomModel
import com.intellij.pom.PomTransaction
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtParsingTestCase
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors

const val GENERATED_FILE_PREFIX = "rnd"

class RandomParsingTest : KtParsingTestCase(".", "kt", KotlinParserDefinition()) {

    companion object;

    val project = lazy {
        val disposable = object : Disposable {
            override fun dispose() {}
            override fun toString() = "RandomParsingTest"
        }
        val environment = KotlinCoreEnvironment.createForTests(
            disposable,
            KotlinTestUtils.newConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val project = environment.project as MockProject
        val pomModel = object : PomModelImpl(project) {
            override fun runTransaction(transaction: PomTransaction) = transaction.run()
        }
        TreeAspect(pomModel)
        project.registerService(PomModel::class.java, pomModel)
        Extensions.getRootArea().registerExtensionPoint(
            TreeCopyHandler.EP_NAME.name,
            TreeCopyHandler::class.java.canonicalName,
            ExtensionPoint.Kind.INTERFACE
        )
        project
    }

    private fun printToFile(data: String, directory: String, name: String) {
        val path = "$directory/$name"
        val file = File(path)
        val printWriter = PrintWriter(file)
        printWriter.use { it.write(data) }
    }

    fun generateFailedTestCodes(
        startWith: Int,
        maxNumberFailedTests: Int,
        dataPath: String,
        createGenerator: (Long, Project) -> Generator
    ) {
        val dataDirectory = File(dataPath)
        dataDirectory.mkdirs()
        dataDirectory.listFiles().map(File::delete)
        var numberFailedTests = 0
        val pool = Executors.newFixedThreadPool(8)
        val service = ExecutorCompletionService<Boolean>(pool)
        for (i in startWith..(startWith + 8))
            service.submit { generateSingleParsingTest(i, dataPath, createGenerator) }
        for (i in (startWith + 9)..Int.MAX_VALUE) {
            val isFail = service.take().get()
            if (i % 1000 == 0)
                println("test $i generation started")
            if (isFail && ++numberFailedTests >= maxNumberFailedTests)
                break
            service.submit { generateSingleParsingTest(i, dataPath, createGenerator) }
        }
        pool.shutdownNow()
    }

    private fun generateSingleParsingTest(i: Int, dataPath: String, createGenerator: (Long, Project) -> Generator): Boolean {
        val seed = 1729192L + i
        val fileName = "$GENERATED_FILE_PREFIX$i"
        val generator = createGenerator(seed, project.value)
        val file = generator.generate("$fileName.kt")
        val code = file.text
        val ast = DebugUtil.psiToString(file, false, false)
        val actualFile = generator.createFile("$fileName.kt", code)
        val actualText = DebugUtil.psiToString(actualFile, false, false)
        val expected = StringUtil.convertLineSeparators(ast.trim())
        val actual = StringUtil.convertLineSeparators(actualText.trim())
        if (!Comparing.equal(expected, actual)) {
            printToFile(code, dataPath, "$fileName.kt")
            printToFile(ast, dataPath, "$fileName.txt")
            System.err.println("INFO: failing test $fileName is generated")
            return true
        }
        return false
    }
}