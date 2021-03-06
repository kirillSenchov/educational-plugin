package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreActionBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader
import com.jetbrains.edu.learning.stepik.api.loadAndFillAdditionalCourseInfo
import com.jetbrains.edu.learning.stepik.api.loadAndFillLessonAdditionalInfo
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import icons.EducationalCoreIcons
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered") // Hyperskill.xml
class GetHyperskillLesson : DumbAwareAction(
  EduCoreActionBundle.lazyMessage("get.lesson", HYPERSKILL, StepikNames.STEPIK),
  EduCoreActionBundle.lazyMessage("get.lesson.description", HYPERSKILL, StepikNames.STEPIK),
  EducationalCoreIcons.JB_ACADEMY_ENABLED
) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = CCPluginToggleAction.isCourseCreatorFeaturesEnabled
                                         && isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val lessonId = Messages.showInputDialog(
      EduCoreActionBundle.message("get.lesson.enter.lesson.id"),
      EduCoreActionBundle.message("get.lesson", HYPERSKILL, StepikNames.STEPIK),
      EducationalCoreIcons.JB_ACADEMY_ENABLED
    )
    if (lessonId != null && lessonId.isNotEmpty()) {
      ProgressManager.getInstance().run(object : Task.Modal(
        project,
        EduCoreActionBundle.message("get.course.loading"),
        true
      ) {
        override fun run(indicator: ProgressIndicator) {
          val course = createCourse(lessonId) ?: return
          val configurator = course.configurator
          if (configurator == null) {
            val environment = if (course.environment == EduNames.DEFAULT_ENVIRONMENT) "default" else course.environment
            showError(
              EduCoreErrorBundle.message("failed.to.create.lesson.no.configuration", course.language, environment),
              EduCoreErrorBundle.message("failed.to.create.lesson")
            )
            return
          }
          runInEdt {
            CCNewCourseDialog(
              EduCoreActionBundle.message("get.lesson", HYPERSKILL, StepikNames.STEPIK),
              EduCoreBundle.message("label.create"),
              course
            ).show()
          }
        }
      })
    }
  }

  @VisibleForTesting
  fun createCourse(lessonId: String): HyperskillCourse? {
    val course = HyperskillCourse()
    val lesson = StepikConnector.getInstance().getLesson(Integer.valueOf(lessonId))
    if (lesson == null) {
      showIncorrectCredentialsError()
      return null
    }
    val allStepSources = StepikConnector.getInstance().getStepSources(lesson.steps)
    val tasks = StepikCourseLoader.getTasks(course, lesson, allStepSources)
    for (task in tasks) {
      lesson.addTask(task)
    }

    val languageAndEnvironment = getLanguageAndEnvironment(lesson)
    if (languageAndEnvironment == null) {
      showError(
        EduCoreErrorBundle.message("failed.to.create.lesson.undefined.language"),
        EduCoreErrorBundle.message("failed.to.create.lesson")
      )
      return null
    }

    @NonNls val hyperskillLessonName = "Hyperskill lesson $lessonId"
    course.apply {
      name = hyperskillLessonName
      description = hyperskillLessonName
      language = languageAndEnvironment.first
      environment = languageAndEnvironment.second
    }

    val hyperskillLesson = FrameworkLesson(lesson)
    course.addItem(hyperskillLesson, 0)
    loadAndFillAdditionalCourseInfo(course)
    loadAndFillLessonAdditionalInfo(lesson, course)

    return course
  }

  private fun showIncorrectCredentialsError() {
    val stepikUser = EduSettings.getInstance().user

    val message = if (stepikUser == null) {
      EduCoreErrorBundle.message("failed.to.get.lesson.not.log.in", StepikNames.STEPIK)
    }
    else {
      EduCoreErrorBundle.message("failed.to.get.lesson.no.access", StepikNames.STEPIK, stepikUser.name)
    }
    showError(message, EduCoreErrorBundle.message("failed.to.get.lesson"))
  }

  private fun showError(message: String, title: String) {
    runInEdt {
      Messages.showErrorDialog(message, title)
    }
  }

  private fun getLanguageAndEnvironment(lesson: Lesson): Pair<String, String>? {
    for (task in lesson.taskList) {
      val taskFiles = task.taskFiles.values
      if (taskFiles.any { it.name.contains("androidTest") }) {
        return EduNames.KOTLIN to EduNames.ANDROID
      }
      if (taskFiles.any { it.name == "tests.py" }) {
        return EduNames.PYTHON to EduNames.DEFAULT_ENVIRONMENT
      }
      for (taskFile in taskFiles) {
        if (!taskFile.isVisible) {
          continue
        }
        val languageAndEnvironment = when (FileUtilRt.getExtension(taskFile.name)) {
          "java" -> EduNames.JAVA to EduNames.DEFAULT_ENVIRONMENT
          "py" -> EduNames.PYTHON to EduNames.UNITTEST //legacy environment was handled earlier
          "kt" -> EduNames.KOTLIN to EduNames.DEFAULT_ENVIRONMENT
          "js", "html" -> EduNames.JAVASCRIPT to EduNames.DEFAULT_ENVIRONMENT
          "scala" -> EduNames.SCALA to EduNames.DEFAULT_ENVIRONMENT
          else -> null
        }

        if (languageAndEnvironment != null) {
          return languageAndEnvironment
        }
      }
    }
    return null
  }
}