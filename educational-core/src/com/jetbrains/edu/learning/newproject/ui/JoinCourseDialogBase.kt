package com.jetbrains.edu.learning.newproject.ui

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import javax.swing.JComponent

open class JoinCourseDialogBase(private val course: Course, settings: CourseDisplaySettings) : OpenCourseDialogBase() {
  private val panel: JoinCoursePanel = JoinCoursePanel(settings)

  init {
    title = course.name
    panel.bindCourse(course)
    panel.setValidationListener(course, object : JoinCoursePanel.ValidationListener {
      override fun onInputDataValidated(isInputDataComplete: Boolean) {
        isOKActionEnabled = isInputDataComplete
      }
    })
  }

  override val courseInfo: CourseInfo
    get() = CourseInfo(course, { panel.locationString }, { panel.projectSettings })

  override fun createCenterPanel(): JComponent = panel

  override fun setError(error: ErrorState) {
    val message = error.message ?: return
    panel.updateErrorText(message)
  }
}
