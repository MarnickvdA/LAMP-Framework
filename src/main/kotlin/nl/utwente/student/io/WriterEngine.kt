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
        val file = modules?.let { writeModules(it, output) }

        return file?.also {
            println("Transformed ${modules.size} module(s), now located in ${it.absolutePath}")
        }
    }

    private fun writeModules(modules: List<ModuleRoot>, outputDir: File): File {
        val jaxbMarshaller: Marshaller = JAXBContext.newInstance(ModuleRoot::class.java.packageName).createMarshaller()
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        outputDir.mkdirs()

        modules.forEach { moduleRoot ->
            if (moduleRoot.module == null) {
                System.err.println(
                    "Cannot write module in ${moduleRoot.fileName}"
                )
            } else {
                val outputFile = Paths.get(
                    outputDir.absolutePath,
                    "${moduleRoot.componentName}.${moduleRoot.module?.identifier?.value}.${SupportedLanguage.METAMODEL.fileExtension}"
                ).toFile()

                println("${moduleRoot.fileName}: Writing module ${moduleRoot.module?.identifier?.value} to ${outputFile.name}.")
                try {
                    val output = FileOutputStream(outputFile)
                    jaxbMarshaller.marshal(moduleRoot, output)
                    output.close()
                } catch (e: JAXBException) {
                    System.err.println("Writing ${moduleRoot.fileName} failed!")
                    e.printStackTrace()
                }
            }
        }

        return outputDir
    }
}