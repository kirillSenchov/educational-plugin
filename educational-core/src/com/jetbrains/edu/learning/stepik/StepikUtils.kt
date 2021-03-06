/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("StepikUtils")

package com.jetbrains.edu.learning.stepik

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager

const val UPDATE_NOTIFICATION_GROUP_ID = "Update.course"

private val LOG = Logger.getInstance(StepikAuthorizer::class.java)

fun setCourseLanguageEnvironment(info: EduCourse) {
  val courseFormat = info.type
  val languageIndex = courseFormat.indexOf(" ")
  if (languageIndex != -1) {
    val environmentIndex = courseFormat.indexOf(EduCourse.ENVIRONMENT_SEPARATOR, languageIndex + 1)
    if (environmentIndex != -1) {
      info.language = courseFormat.substring(languageIndex + 1, environmentIndex)
      info.environment = courseFormat.substring(environmentIndex + 1)
    }
    else {
      info.language = courseFormat.substring(languageIndex + 1)
    }
  }
  else {
    LOG.info(String.format("Language for course `%s` with `%s` type can't be set because it isn't \"pycharm\" course",
                           info.name, courseFormat))
  }
}

fun getStepikLink(task: Task, lesson: Lesson): String {
  return "${StepikNames.STEPIK_URL}/lesson/${lesson.id}/step/${task.index}"
}

fun updateCourse(project: Project, course: EduCourse) {
  StepikCourseUpdater(course, project).updateCourse()
  SubmissionsManager.getInstance(project).getSubmissions(course.allTasks.map { it.id }.toSet())
  StepikSolutionsLoader.getInstance(project).loadSolutionsInBackground()
}

fun showUpdateAvailableNotification(project: Project, updateAction: () -> Unit) {
  val notification = Notification(UPDATE_NOTIFICATION_GROUP_ID, EduCoreBundle.message("update.content"),
                                  EduCoreBundle.message("update.content.request"),
                                  NotificationType.INFORMATION,
                                  notificationListener(project, updateAction))
  notification.notify(project)
}

fun notificationListener(project: Project,
                         updateAction: () -> Unit): NotificationListener {
  return NotificationListener { notification, _ ->
    FileEditorManagerEx.getInstanceEx(project).closeAllFiles()
    notification.expire()
    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        updateAction()
      },
      "Updating Course", true, project)
  }
}
