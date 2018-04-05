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
import com.intellij.pom.PomModel
import com.intellij.pom.PomTransaction
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class RandomParsingTestGenerator(private val rootPath: String) {

    private fun writeToFile(data: String, directory: String, name: String) {
        val path = "$directory/$name"
        val file = File(path)
        val printWriter = PrintWriter(file)
        printWriter.use { it.write(data) }
    }

    fun process(numSamples: Int, createGenerator: (Long, Project) -> Generator) {
        File(rootPath).mkdirs()
        val disposable = object : Disposable {
            override fun dispose() {}
            override fun toString() = "RandomParsingTestGenerator"
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
        val tasks = ArrayList<Callable<Unit>>()
        for (i in 0 until numSamples) {
            val fileName = "rnd$i.kt"
            val generator = createGenerator(912301L + i, project)
            tasks.add(Callable {
                val file = generator.generate(fileName)
                println("done generation $fileName")
                val ast = DebugUtil.psiToString(file, false, false)
                val code = file.text
                writeToFile(code, rootPath, fileName)
                writeToFile(ast, rootPath, fileName.replace(".kt", ".txt"))
            })
        }
        val service = Executors.newFixedThreadPool(8)
        service.invokeAll(tasks).forEach(Future<Unit>::get)
        service.shutdownNow()
    }
}