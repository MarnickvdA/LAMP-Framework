package nl.utwente.student.io

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.utils.getFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths

object XMLWriter {
    fun writeModules(modules: List<Module>, outputDir: String): File? {
        val out: File? = getFile(outputDir)

        if (out == null) {
            System.err.println("Cannot use $outputDir as directory.")
            return null
        }

        val jaxbMarshaller: Marshaller = JAXBContext.newInstance(Module::class.java.packageName).createMarshaller()
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        out.mkdirs()

        modules.forEach { module ->
            if (module.moduleScope == null) {
                System.err.println(
                    "Cannot write module in ${module.fileName} at lines " +
                            "${module.metadata.startLine} to ${module.metadata.endLine}"
                )
            } else {
                val outputFile = Paths.get(
                    out.absolutePath,
                    "${module.packageName}.${module.moduleScope?.identifier?.value}.xml"
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

        return out
    }
}