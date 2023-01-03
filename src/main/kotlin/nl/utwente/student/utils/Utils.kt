package nl.utwente.student.utils

import nl.utwente.student.metamodel.v2.Assignment
import nl.utwente.student.metamodel.v2.Expression
import nl.utwente.student.metamodel.v2.Identifier
import nl.utwente.student.metamodel.v2.Lambda
import nl.utwente.student.metamodel.v2.Metadata
import nl.utwente.student.metamodel.v2.Module
import nl.utwente.student.metamodel.v2.Property
import nl.utwente.student.metamodel.v2.Unit
import nl.utwente.student.metamodel.v2.UnitCall
import org.antlr.v4.runtime.tree.ParseTree
import java.io.File
import java.lang.Exception
import java.math.BigInteger
import java.nio.file.Paths
import java.security.MessageDigest

fun ParseTree.getDepth(): Int {
    var depth = 0
    var parent: ParseTree? = this
    while (parent != null) {
        parent = parent.parent
        depth++
    }

    return depth
}

fun getFile(fileOrDir: String): File? {
    return try {
        Paths.get(System.getProperty("user.dir"), fileOrDir).toFile()
    } catch (ex: Exception) {
        try {
            Paths.get(fileOrDir).toFile()
        } catch (ex: Exception) {
            System.err.println("Invalid file path.")
            return null
        }
    }
}

fun Module.getUniqueName(): String {
    return mutableListOf(
        this.packageName,
        this.moduleScope?.identifier?.value + getUniquePosition(this.metadata),
    ).filterNotNull().joinToString(".")
}

fun Unit.getUniqueName(module: Module?): String {
    return mutableListOf(
        module?.packageName,
        module?.moduleScope?.identifier?.value,
        this.identifier.value + getUniquePosition(this.metadata)
    ).filterNotNull().joinToString(".")
}

fun Lambda.getUniqueName(module: Module?): String {
    return mutableListOf(
        module?.packageName,
        module?.moduleScope?.identifier?.value,
        "Lambda" + getUniquePosition(this.metadata)
    ).filterNotNull().joinToString(".")
}

fun Property.getUniqueName(module: Module?): String {
    return mutableListOf(
        module?.packageName,
        module?.moduleScope?.identifier?.value,
        this.identifier.value + getUniquePosition(this.metadata)
    ).filterNotNull().joinToString(".")
}

fun UnitCall.getUniqueName(module: Module?): String {
    fun getFullSignature(prefix: Expression?): MutableList<String?> {
        return when (prefix) {
            is UnitCall -> getFullSignature(prefix.nestedScope?.expressions?.first()).also { it.add(prefix.reference.value)}
            is Identifier -> mutableListOf(prefix.value)
            else -> mutableListOf()
        }
    }

    return getFullSignature(this).joinToString(".")+ getUniquePosition(this.metadata)
}

fun Assignment.getUniqueName(): String {
    // TODO Fix reference
    return this.reference.value + getUniquePosition(this.metadata)
}

private fun getUniquePosition(metadata: Metadata): String {
    return "$" + md5("${metadata.startLine}:${metadata.startOffset}:${metadata.endLine}:${metadata.endOffset}").substring(0..8)
}

private fun md5(input:String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
}