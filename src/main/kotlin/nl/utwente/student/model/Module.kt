package nl.utwente.student.model

import org.w3c.dom.Document
import java.io.File

data class Module(val moduleName: String, val packageName: String, val moduleDocument: Document, val file: File)
