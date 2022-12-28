package nl.utwente.student.parsers

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Unmarshaller
import java.io.File
import java.nio.file.Paths
import nl.utwente.student.metamodel.v2.Module

class MetamodelProjectParser {

    fun readProjectFromDirectory(directoryPath: String): List<Module> {
        val file = Paths.get(System.getProperty("user.dir"), directoryPath).toFile()
        val modules = mutableListOf<Module>()

        file.listFiles()
            ?.filter { !it.isDirectory }
            ?.mapNotNull(this::readFromXML)
            ?.let { modules.addAll(it) }

        return modules
    }

    private fun readFromXML(fromFile: File): Module? {
        try {
            val jaxbMarshaller: Unmarshaller = JAXBContext.newInstance(Module::class.java.packageName).createUnmarshaller()
            return jaxbMarshaller.unmarshal(fromFile) as? Module
        } catch (e: JAXBException) {
            System.err.println("Reading from ${fromFile.name} failed!")
            e.printStackTrace()
        }

        return null
    }
}