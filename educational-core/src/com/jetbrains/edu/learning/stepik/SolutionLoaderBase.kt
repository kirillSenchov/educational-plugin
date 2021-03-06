package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.*
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.editor.EduEditor
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.update.UpdateNotification
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.util.*
import java.util.Collections.max
import java.util.concurrent.Callable
import java.util.concurrent.Future

abstract class SolutionLoaderBase(protected val project: Project) : Disposable {

  private var futures: Map<Int, Future<Boolean>> = HashMap()

  fun loadSolutionsInBackground() {
    val course = StudyTaskManager.getInstance(project).course ?: return
    ProgressManager.getInstance().run(object : Backgroundable(project, EduCoreBundle.message("update.loading.solutions")) {
      override fun run(progressIndicator: ProgressIndicator) {
        loadAndApplySolutions(course, progressIndicator)
      }
    })
  }

  fun loadSolutionsInBackground(course: Course, tasksToUpdate: List<Task>, force: Boolean) {
    ProgressManager.getInstance().run(object : Backgroundable(project, EduCoreBundle.message("update.loading.solutions")) {
      override fun run(progressIndicator: ProgressIndicator) {
        loadAndApplySolutions(course, tasksToUpdate, progressIndicator, force)
      }
    })
  }

  @VisibleForTesting
  @JvmOverloads
  fun loadAndApplySolutions(course: Course, progressIndicator: ProgressIndicator? = null) {
    val tasksToUpdate = provideTasksToUpdate(course)
    loadAndApplySolutions(course, tasksToUpdate, progressIndicator)
  }

  private fun loadAndApplySolutions(course: Course, tasksToUpdate: List<Task>, progressIndicator: ProgressIndicator? = null,
                                    force: Boolean = false) {
    val submissions = if (progressIndicator != null)
      ApplicationUtil.runWithCheckCanceled(Callable { loadSubmissions(tasksToUpdate) }, progressIndicator)
    else loadSubmissions(tasksToUpdate)

    if (submissions != null) {
      updateTasks(course, tasksToUpdate, submissions, progressIndicator, force)
    }
    else {
      LOG.warn("Can't get submissions")
    }

    runReadAction {
      if (project.isDisposed) return@runReadAction
      project.messageBus.syncPublisher(loadingTopic).solutionLoaded(course)
    }
  }

  protected open fun updateTasks(course: Course, tasks: List<Task>, submissions: List<Submission>, progressIndicator: ProgressIndicator?,
                                 force: Boolean = false) {
    progressIndicator?.isIndeterminate = false
    cancelUnfinishedTasks()
    val tasksToUpdate = tasks.filter { task -> task !is TheoryTask }
    var finishedTaskCount = 0
    val futures = HashMap<Int, Future<Boolean>>(tasks.size)
    for (task in tasksToUpdate) {
      invokeAndWaitIfNeeded {
        if (project.isDisposed) return@invokeAndWaitIfNeeded
        for (editor in getOpenTaskEditors(project, task)) {
          editor.startLoading()
        }
      }
      futures[task.id] = ApplicationManager.getApplication().executeOnPooledThread<Boolean> {
        try {
          ProgressManager.checkCanceled()
          updateTask(project, task, submissions, force)
        }
        finally {
          if (progressIndicator != null) {
            synchronized(progressIndicator) {
              finishedTaskCount++
              progressIndicator.fraction = finishedTaskCount.toDouble() / tasksToUpdate.size
              progressIndicator.text = "Loading solution $finishedTaskCount of ${tasksToUpdate.size}"
            }
          }
          invokeAndWaitIfNeeded {
            if (project.isDisposed) return@invokeAndWaitIfNeeded
            for (editor in getOpenTaskEditors(project, task)) {
              editor.stopLoading()
              editor.validateTaskFile()
            }
          }
        }
      }
    }

    synchronized(this) {
      this.futures = futures
    }

    val connection = project.messageBus.connect()
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val task = EduUtils.getTaskForFile(project, file) ?: return
        val future = futures[task.id] ?: return
        if (!future.isDone) {
          (source.getSelectedEditor(file) as? EduEditor)?.startLoading()
        }
      }
    })

    try {
      waitAllTasks(futures.values)
    }
    finally {
      connection.disconnect()
    }

    val needToShowNotification = needToShowUpdateNotification(futures.values)
    runInEdt {
      if (project.isDisposed) return@runInEdt
      if (needToShowNotification) {
        UpdateNotification(NOTIFICATION_TITLE, NOTIFICATION_CONTENT).notify(project)
      }
      EduUtils.synchronize()
      ProjectView.getInstance(project).refresh()
    }
  }

  private fun getOpenTaskEditors(project: Project, task: Task): List<EduEditor> {
    return FileEditorManager.getInstance(project)
      .allEditors
      .filterIsInstance<EduEditor>()
      .filter { it.taskFile.task == task }
  }

  private fun waitAllTasks(tasks: Collection<Future<*>>) {
    for (task in tasks) {
      if (isUnitTestMode) {
        EduUtils.waitAndDispatchInvocationEvents(task)
      }
      else {
        try {
          task.get()
        }
        catch (e: Exception) {
          LOG.warn(e)
        }
      }
    }
  }

  private fun needToShowUpdateNotification(tasks: Collection<Future<*>>): Boolean {
    return tasks.any { future ->
      try {
        future.get() == true
      }
      catch (e: Exception) {
        LOG.warn(e)
        false
      }
    }
  }

  @Synchronized
  private fun cancelUnfinishedTasks() {
    for (future in futures.values) {
      if (!future.isDone) {
        future.cancel(true)
      }
    }
  }

  /**
   * @return true if solutions for given task are incompatible with current plugin version, false otherwise
   */
  fun updateTask(project: Project, task: Task, submissions: List<Submission>, force: Boolean = false): Boolean {
    val taskSolutions = loadSolution(task, submissions)
    ProgressManager.checkCanceled()
    if (!taskSolutions.hasIncompatibleSolutions && taskSolutions.solutions.isNotEmpty()) {
      applySolutions(project, task, taskSolutions, force)
    }
    return taskSolutions.hasIncompatibleSolutions
  }

  override fun dispose() {
    cancelUnfinishedTasks()
  }

  protected abstract val loadingTopic: Topic<SolutionLoadingListener>
  protected abstract fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions
  protected abstract fun loadSubmissions(tasks: List<Task>): List<Submission>?
  abstract fun provideTasksToUpdate(course: Course): List<Task>

  interface SolutionLoadingListener {
    fun solutionLoaded(course: Course)
  }

  companion object {

    private val LOG = Logger.getInstance(SolutionLoaderBase::class.java)

    private const val NOTIFICATION_TITLE = "Outdated EduTools Plugin"
    private const val NOTIFICATION_CONTENT = "<html>Your version of EduTools plugin is outdated to apply all solutions.\n" + "<a href=\"\">Update plugin</a> to avoid compatibility problems.\n"

    private fun updatePlaceholders(taskFile: TaskFile, updatedPlaceholders: List<AnswerPlaceholder>) {
      val answerPlaceholders = taskFile.answerPlaceholders
      if (answerPlaceholders.size != updatedPlaceholders.size) {
        LOG.warn("")
        return
      }
      for ((answerPlaceholder, updatedPlaceholder) in answerPlaceholders.zip(updatedPlaceholders)) {
        answerPlaceholder.placeholderText = updatedPlaceholder.placeholderText
        answerPlaceholder.status = updatedPlaceholder.status
        answerPlaceholder.offset = updatedPlaceholder.offset
        answerPlaceholder.length = updatedPlaceholder.length
        answerPlaceholder.selected = updatedPlaceholder.selected
      }
    }

    private fun Task.modificationDate(project: Project): Date {
      val taskDir = getTaskDir(project) ?: return Date(0)
      return Date(max(taskFiles.values.map { EduUtils.findTaskFileInDir(it, taskDir)?.timeStamp ?: 0 }))
    }

    private fun Task.modifiedBefore(project: Project, taskSolutions: TaskSolutions): Boolean {
      val solutionDate = taskSolutions.date ?: return true
      return solutionDate.isSignificantlyAfter(modificationDate(project))
    }

    private fun applySolutions(project: Project,
                               task: Task,
                               taskSolutions: TaskSolutions,
                               force: Boolean) {
      invokeAndWaitIfNeeded {
        if (project.isDisposed) return@invokeAndWaitIfNeeded
        task.status = taskSolutions.checkStatus
        YamlFormatSynchronizer.saveItem(task)
        val lesson = task.lesson
        if (task.course.isStudy && lesson is FrameworkLesson && lesson.currentTask() != task) {
          applySolutionToNonCurrentTask(project, task, taskSolutions)
        }
        else {
          if (force || EduUtils.isNewlyCreated(project) || task.modifiedBefore(project, taskSolutions)) {
            applySolutionToCurrentTask(project, task, taskSolutions)
          }
        }
      }
    }

    private fun applySolutionToNonCurrentTask(project: Project, task: Task, taskSolutions: TaskSolutions) {
      val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
      frameworkLessonManager.saveExternalChanges(task, taskSolutions.solutions.mapValues { it.value.text })
      for (taskFile in task.taskFiles.values) {
        val solution = taskSolutions.solutions[taskFile.name] ?: continue
        updatePlaceholders(taskFile, solution.placeholders)
        taskFile.isVisible = solution.isVisible
      }
    }

    private fun applySolutionToCurrentTask(project: Project, task: Task, taskSolutions: TaskSolutions) {
      val taskDir = task.getTaskDir(project) ?: error("Directory for task `${task.name}` not found")
      for ((path, solution) in taskSolutions.solutions) {
        val taskFile = task.getTaskFile(path)
        if (taskFile == null) {
          GeneratorUtils.createChildFile(taskDir, path, solution.text)
          val createdFile = task.getTaskFile(path)
          if (createdFile == null) {
            val help = if (isUnitTestMode) "Don't you forget to use `withVirtualFileListener`?" else ""
            LOG.error("taskFile $path should be created moment ago. $help")
            continue
          }
          createdFile.isVisible = solution.isVisible
        }
        else {
          val vFile = taskDir.findFileByRelativePath(path) ?: continue
          taskFile.isVisible = solution.isVisible

          if (EduUtils.isTestsFile(project, vFile)) continue
          updatePlaceholders(taskFile, solution.placeholders)
          EduDocumentListener.modifyWithoutListener(task, path) {
            runUndoTransparentWriteAction {
              val document = FileDocumentManager.getInstance().getDocument(vFile) ?: error("No document for ${path}")
              document.setText(solution.text)
            }
          }
        }
      }
    }
  }

  protected data class Solution(val text: String, val isVisible: Boolean, val placeholders: List<AnswerPlaceholder>)

  protected class TaskSolutions @JvmOverloads constructor(
    val date: Date?,
    val checkStatus: CheckStatus,
    val solutions: Map<String, Solution> = emptyMap(),
    val hasIncompatibleSolutions: Boolean = false
  ) {
    companion object {
      val EMPTY = TaskSolutions(null, CheckStatus.Unchecked)
      val INCOMPATIBLE = TaskSolutions(null, CheckStatus.Unchecked, hasIncompatibleSolutions = true)
    }
  }
}
