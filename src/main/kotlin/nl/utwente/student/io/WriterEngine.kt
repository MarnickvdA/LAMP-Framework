package nl.utwente.student.io

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.models.SupportedLanguage
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths

object WriterEngine {

    fun write(modules: List<Module>?, output: File): File? {
        val file = modules?.let { writeModules(it, output) }

        return file?.also {
            println("Transformed ${modules.size} module(s), now located in ${it.absolutePath}")
        }
    }

    private fun writeModules(modules: List<Module>, outputDir: File): File {
        val jaxbMarshaller: Marshaller = JAXBContext.newInstance(Module::class.java.packageName).createMarshaller()
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        outputDir.mkdirs()

        modules.forEach { module ->
            if (module.moduleScope == null) {
                System.err.println(
                    "Cannot write module in ${module.fileName}"
                )
            } else {
                val outputFile = Paths.get(
                    outputDir.absolutePath,
                    "${module.componentName}.${module.moduleScope?.identifier?.value}.${SupportedLanguage.METAMODEL.fileExtension}"
                ).toFile()

                println("${module.fileName}: Writing module ${module.moduleScope?.identifier?.value} to ${outputFile.name}.")
                try {
                    val output = FileOutputStream(outputFile)
                    jaxbMarshaller.marshal(module, output)
                    output.close()
                } catch (e: JAXBException) {
                    System.err.println("Writing ${module.fileName} failed!")
                    e.printStackTrace()
                }
            }
        }

        return outputDir
    }
}