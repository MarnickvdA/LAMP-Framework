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
                    "${moduleRoot.componentName}.${moduleRoot.module?.identifier?.value}.${SupportedLanguage.METAMODEL.fileExtension}"
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