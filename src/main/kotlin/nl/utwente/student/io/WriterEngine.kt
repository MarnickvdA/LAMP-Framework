package nl.utwente.student.io

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import nl.utwente.student.metamodel.v3.ModuleRoot
import nl.utwente.student.models.SupportedLanguage
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths

object WriterEngine {

    fun write(modules: List<ModuleRoot>?, output: File): File? {
        return modules
            ?.let { writeModules(it, output) }
            ?.also { (file, successCount) ->
                println("Transformed $successCount of ${modules.size} file(s) to .${SupportedLanguage.METAMODEL.fileExtension} files, now located in ${file.absolutePath}")
            }?.first
    }

    fun writeToCSV(output: File, moduleMetrics: MetricResults, unitMetrics: MetricResults) {
        if (!output.exists()) {
            output.mkdirs()
        }

        fun writeMetricResults(toFile: File, results: MetricResults, metrics: Array<String>) {
            FileOutputStream(toFile).apply {
                val writer = bufferedWriter()
                writer.write(""""DeclarableId";${metrics.joinToString(";") { "\"${it}\"" }}""")
                writer.newLine()
                results.forEach { (moduleId, results) ->
                    writer.write(
                        "\"${moduleId}\";${
                            metrics.map { results.find { p -> p.first == it }?.second ?: 0 }.joinToString(";")
                        }"
                    )
                    writer.newLine()
                }
                writer.flush()
            }
        }

        if (moduleMetrics.isNotEmpty())
            writeMetricResults(
                Paths.get(output.absolutePath, "modules.csv").toFile(), moduleMetrics,
                arrayOf("MLOC", "WMC", "CWMC", "DIT", "NOC", "CBO", "RFC", "LCOM", "NOU", "LC")
            )

        if (unitMetrics.isNotEmpty())
            writeMetricResults(
                Paths.get(output.absolutePath, "units.csv").toFile(), unitMetrics,
                arrayOf("ULOC", "CC", "COCO", "PC", "LLOC")
            )
    }

    private fun writeModules(modules: List<ModuleRoot>, outputDir: File): Pair<File, Int> {
        val jaxbMarshaller: Marshaller = JAXBContext.newInstance(ModuleRoot::class.java.packageName).createMarshaller()
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        outputDir.mkdirs()
        var successCount = 0
        modules.forEach { moduleRoot ->
            if (moduleRoot.module == null) {
                System.err.println(
                    "Cannot write module for ${moduleRoot.filePath}"
                )
            } else {
                val outputFile = Paths.get(
                    outputDir.absolutePath,
                    "${moduleRoot.componentName}.${moduleRoot.module?.id}.${SupportedLanguage.METAMODEL.fileExtension}"
                ).toFile()

                try {
                    val output = FileOutputStream(outputFile)
                    jaxbMarshaller.marshal(moduleRoot, output)
                    output.close()
                    successCount++
                } catch (e: JAXBException) {
                    System.err.println("Writing ${moduleRoot.filePath} failed!")
                    e.printStackTrace()
                }
            }
        }

        return Pair(outputDir, successCount)
    }
}