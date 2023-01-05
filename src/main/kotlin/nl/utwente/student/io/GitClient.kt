package nl.utwente.student.io

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import java.io.File
import java.lang.Exception
import java.nio.file.Paths

object GitClient {
    fun cloneRepository(url: String, outDir: String?): File? {
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
                println("Clone completed.")

                return file
            } catch (ex: Exception) {
                System.err.println("Clone failed.")
                ex.printStackTrace()
            }
        } else {
            println("$projectName already exists at ${file.absolutePath}. Trying to pull latest changes.")

            try {
                Git.open(file).pull().call()
                println("Pull completed.")

                return file
            } catch (ex: Exception) {
                System.err.println("Pull failed.")
                ex.printStackTrace()
            }
        }

        return null
    }

    private class GitProcessMonitor(val projectName: String) : ProgressMonitor {
        private var currentTask: String? = ""

        override fun start(totalTasks: Int) {
            println("[$projectName] Total Tasks: $totalTasks")
        }

        override fun beginTask(title: String?, totalWork: Int) {
            currentTask = title
            println("[$projectName] Starting task ($totalWork): $title")
        }

        override fun update(completed: Int) {
        }

        override fun endTask() {
            println("[$projectName] Task ended: $currentTask")
        }

        override fun isCancelled(): Boolean {
            return false
        }
    }

}
