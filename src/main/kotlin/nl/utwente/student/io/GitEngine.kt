package nl.utwente.student.io

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import java.io.File
import java.lang.Exception
import java.nio.file.Paths

object GitEngine {
    fun clone(url: String, outDir: String?): File? {
        val projectName = url.split("/").last().dropLast(4)
        val cloneDirectory = outDir ?: "projects/${projectName}"

        // Clone repository via provided url into directory, defaults to /projects/repoName
        val file = Paths.get(System.getProperty("user.dir"), cloneDirectory).toFile()

        if (!file.exists()) {
            println("Cloning $projectName into ${file.absolutePath}.")
            try {
                Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(file)
                    .setProgressMonitor(GitProcessMonitor(projectName))
                    .call()
                println("[$projectName] Clone completed.")

                return file
            } catch (ex: Exception) {
                System.err.println("[$projectName] Clone failed.")
                ex.printStackTrace()
            }
        } else {
            println("$projectName already exists at ${file.absolutePath}. Trying to pull latest changes.")

            try {
                Git.open(file).pull().call()
                println("[$projectName] Pull completed.")

                return file
            } catch (ex: Exception) {
                System.err.println("[$projectName] Pull failed.")
                ex.printStackTrace()
            }
        }

        return null
    }

    private class GitProcessMonitor(val projectName: String) : ProgressMonitor {

        override fun start(totalTasks: Int) {}

        override fun beginTask(title: String?, totalWork: Int) {
            println("[$projectName] $title")
        }

        override fun update(completed: Int) {
        }

        override fun endTask() {
        }

        override fun isCancelled(): Boolean {
            return false
        }
    }

}
