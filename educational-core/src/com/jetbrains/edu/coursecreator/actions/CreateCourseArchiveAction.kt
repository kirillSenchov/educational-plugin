package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.CCUtils.askToWrapTopLevelLessons
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.ui.CCCreateCourseArchiveDialog
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.learning.courseFormat.ext.hasTopLevelLessons
import com.jetbrains.edu.learning.messages.EduCoreActionBundle
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.createCourseArchive
import java.io.File

abstract class CreateCourseArchiveAction(title: String) : DumbAwareAction(title) {

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    val project = e.project
    presentation.isEnabledAndVisible = project != null && isCourseCreator(project)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    if (course.hasSections && course.hasTopLevelLessons) {
      if (!askToWrapTopLevelLessons(project, (course as EduCourse))) {
        return
      }
    }

    val dlg = CCCreateCourseArchiveDialog(project, course.name, showAuthorField())
    if (!dlg.showAndGet()) {
      return
    }

    val locationPath = dlg.locationPath

    if (showAuthorField()) {
      val authorName = dlg.authorName
      course.setAuthorsAsString(arrayOf(authorName))
      PropertiesComponent.getInstance(project).setValue(AUTHOR_NAME, authorName)
    }

    val errorMessage = createCourseArchive(project, dlg.zipName, locationPath)
    if (errorMessage == null) {
      invokeLater {
        Messages.showInfoMessage("Course archive was saved to $locationPath",
                                 EduCoreActionBundle.message("create.course.archive.success.message"))
      }
      PropertiesComponent.getInstance(project).setValue(LAST_ARCHIVE_LOCATION, locationPath)
      createCourseArchive()
    }
    else {
      Messages.showErrorDialog(project, errorMessage, EduCoreErrorBundle.message("failed.to.create.course.archive"))
    }
  }

  /**
   * @return null if course archive was created successfully, non-empty error message otherwise
   */
  fun createCourseArchive(project: Project, zipName: String, locationDir: String?): String? {
    FileDocumentManager.getInstance().saveAllDocuments()
    return ApplicationManager.getApplication().runWriteAction<String>(getArchiveCreator(project, File(locationDir, "$zipName.zip")))
  }

  abstract fun showAuthorField(): Boolean
  abstract fun getArchiveCreator(project: Project, zipFile: File): CourseArchiveCreator

  companion object {
    const val LAST_ARCHIVE_LOCATION = "Edu.CourseCreator.LastArchiveLocation"
    const val AUTHOR_NAME = "Edu.Author.Name"
  }
}