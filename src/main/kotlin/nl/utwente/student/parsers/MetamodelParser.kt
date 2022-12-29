package nl.utwente.student.parsers

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Unmarshaller
import java.io.File
import java.nio.file.Paths
import nl.utwente.student.metamodel.v2.Module

class MetamodelParser(input: String, output: String) : Parser(input, output) {

    private val jaxbMarshaller: Unmarshaller =
        JAXBContext.newInstance(Module::class.java.packageName).createUnmarshaller()

    override fun parseFile(file: File): List<Module> {
        if (!isValidFile(file)) {
            System.err.println("Couldn't read file ${file.name}")
            return emptyList()
        }

        return try {
            (jaxbMarshaller.unmarshal(file) as? Module)?.let { listOf(it) }
        } catch (e: JAXBException) {
            System.err.println("Reading from ${file.name} failed!")
            e.printStackTrace()
            null
        } ?: listOf()
    }

    override fun parseDirectory(directory: File): List<Module> {
        val modules = mutableListOf<Module>()

        directory.listFiles()
            ?.flatMap(this::parseFile)
            ?.let { modules.addAll(it) }

        return modules
    }

    override fun isValidFile(file: File): Boolean {
        return !file.isDirectory && file.name.endsWith(".xml")
    }
}