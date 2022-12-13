package nl.utwente.student.parsers

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import nl.utwente.student.metamodel.Module
import nl.utwente.student.model.JavaFile
import org.antlr.v4.runtime.*
import org.eclipse.jgit.api.Git
import java.io.*
import java.nio.file.Paths.get

class JavaProjectParser {

    @Throws(RuntimeException::class)
    fun executeWithArgs(args: Array<out String>) {
        if (args.size == 2) {
            when (args[0]) {
                "--path" -> this.executeWithFilePath(args[1])
                "--projectUrl" -> this.executeWithGitHttpUrl(args[1])
                else -> throw RuntimeException("Invalid input")
            }
        }
    }

    @Throws(IOException::class)
    private fun executeWithFilePath(path: String) {
        if (!path.endsWith(".java")) throw IOException("File is not a Java file.")

        val file = get(System.getProperty("user.dir"), path).toFile()
        val javaFile = JavaFile.parse(file)
        val modules = javaFile.extractModulesFromAST()

        modules.forEach { module -> writeToXML(module, this.getOutputFile(module)) }
    }

    private fun executeWithGitHttpUrl(url: String) {
        if (!url.endsWith(".git")) throw IOException("Invalid git repository url")

        // Clone repository via provided url into /input directory
        val repoName = url.split("/").last().dropLast(4)

        val file = get(System.getProperty("user.dir"), "/projects/${repoName}").toFile()

        if (!file.exists()) {
            Git.cloneRepository()
                .setURI(url)
                .setDirectory(file)
                .call()
        }

        // Retrieve all files ending with .java extension
        file.walk()
            .filter { it.isFile && it.name.endsWith(".java") }
            .forEach {
                val javaFile = JavaFile.parse(it)

                val modules = javaFile.extractModulesFromAST()

                // Phase 4: Write documents to files
                modules.parallelStream().forEach { module -> writeToXML(module, this.getProjectOutputFile(module)) }
            }
    }


    private fun getOutputFile(module: Module): File {
        val fileName = "${module.packageName}.${module.identifier}.xml"

        val path = get(
            System.getProperty("user.dir"),
            "out",
            "input",
        ).toFile()

        // Make folders if they do not exist yet.
        path.mkdirs()

        return get(path.absolutePath, fileName).toFile()
    }

    private fun getProjectOutputFile(module: Module): File {
        val projectName = module.metadata.file.path.split("LAMP-Framework/projects/")[1].split("/")[0]
        val fileName = "${module.packageName}.${module.identifier}.xml"

        val path = get(
            System.getProperty("user.dir"),
            "out",
            projectName
        ).toFile()

        // Make folders if they do not exist yet.
        path.mkdirs()

        return get(path.absolutePath, fileName).toFile()
    }

    private fun writeToXML(module: Module, toFile: File) {
        println("Writing ${module.metadata.file.name} to ${toFile.name}.")
        try {
            val jaxbMarshaller: Marshaller = JAXBContext.newInstance(Module::class.java.packageName).createMarshaller()
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

            val output = FileOutputStream(toFile)
            jaxbMarshaller.marshal(module, output)
            output.close()
        } catch (e: JAXBException) {
            System.err.println("Writing ${module.metadata.file.name} failed!")
            e.printStackTrace()
        }
    }
}