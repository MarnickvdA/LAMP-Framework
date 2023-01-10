package nl.utwente.student.utils

import nl.utwente.student.metamodel.v3.*
import nl.utwente.student.metamodel.v3.Unit
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

fun getFile(fileOrDir: String?): File? {
    if (fileOrDir == null) return null

    return try {
        Paths.get(System.getProperty("user.dir"), fileOrDir).toFile()
    } catch (ex: Exception) {
        try {
            Paths.get(fileOrDir).toFile()
        } catch (ex: Exception) {
            return null
        }
    }
}

fun ModuleRoot.getUniqueName(withLocation: Boolean = true): String {
    return this.module.getUniqueName(this.componentName, withLocation)
}

fun Module.getUniqueName(componentName: String?, withLocation: Boolean = true): String {
    return listOfNotNull(
        componentName,
        this.identifier?.value + if (withLocation) getUniquePosition(this) else "",
    ).joinToString(".")
}
private fun getName(moduleRoot: ModuleRoot?, identifier: String, sourceElement: SourceElement): String {
    return ((moduleRoot?.module?.getUniqueName(moduleRoot.componentName, false)?.let { "$it://" })
        ?: "") + identifier + getUniquePosition(sourceElement)
}
fun Unit.getUniqueName(moduleRoot: ModuleRoot?): String {
    return getName(moduleRoot, this.identifier.value, this)
}

fun Property.getUniqueName(moduleRoot: ModuleRoot?): String {
    return getName(moduleRoot, this.identifier.value, this)
}

fun Lambda.getUniqueName(moduleRoot: ModuleRoot?): String {
    return getName(moduleRoot, "Lambda", this)
}

private fun getFullSignature(prefix: Expression?): MutableList<String?> {
    return when (prefix) {
        is UnitCall -> getFullSignature(prefix.innerScope?.firstOrNull())
            .also { it.add(getFullSignature(prefix.reference).joinToString(".")) }
        is Identifier -> mutableListOf(prefix.value)
        else -> mutableListOf()
    }
}

fun UnitCall.getUniqueName(moduleRoot: ModuleRoot?): String {
    return getName(moduleRoot, getFullSignature(this).joinToString("."), this)
}

fun Assignment.getUniqueName(moduleRoot: ModuleRoot?): String {
    return getName(moduleRoot, getFullSignature(this.reference).joinToString("."), this)
}

fun getUniquePosition(sourceElement: SourceElement): String {
//    return "$" + md5("${metadata.startLine}:${metadata.startOffset}:${metadata.endLine}:${metadata.endOffset}").substring(0..8)
    return "$" + "[${sourceElement.metadata.startLine}:${sourceElement.metadata.startOffset},${sourceElement.metadata.endLine}:${sourceElement.metadata.endOffset}]"
}

private fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
}