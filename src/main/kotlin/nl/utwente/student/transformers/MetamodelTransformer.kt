package nl.utwente.student.transformers

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Unmarshaller
import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.models.SupportedLanguage
import java.io.File

class MetamodelTransformer(override val inputFile: File) : Transformer {
    private val jaxbMarshaller: Unmarshaller by lazy {
        JAXBContext.newInstance(Module::class.java.packageName).createUnmarshaller()
    }
    override val language: SupportedLanguage = SupportedLanguage.METAMODEL

    override fun transform(): List<Module> {
        return try {
            (jaxbMarshaller.unmarshal(inputFile) as? Module)?.let { listOf(it) }
        } catch (e: JAXBException) {
            System.err.println("Reading from ${inputFile.name} failed!")
            e.printStackTrace()
            null
        } ?: listOf()
    }
}