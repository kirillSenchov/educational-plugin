package com.jetbrains.edu.learning.checkio.newProjectUI

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.*
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.LinkInfo
import com.jetbrains.edu.learning.newproject.ui.LoginPanel
import com.jetbrains.edu.learning.newproject.ui.TabInfo

class CheckiOCoursesPanel(platformProvider: CoursesPlatformProvider) : CoursesPanel(platformProvider) {
  private val loginComponent = CheckiOLoginPanel()

  init {
    coursesListPanel.addListener {
      if (selectedCourse != null) {
        val checkiOConnectorProvider = selectedCourse?.configurator as? CheckiOConnectorProvider
        if (checkiOConnectorProvider == null) {
          loginComponent.isVisible = false
          return@addListener
        }

        val checkiOAccount = checkiOConnectorProvider.oAuthConnector.account
        val isLoggedIn = checkiOAccount == null
        loginComponent.isVisible = isLoggedIn
      }
    }
  }

  override fun tabInfo(): TabInfo? {
    val infoText = EduCoreBundle.message("checkio.courses.explanation", CheckiONames.CHECKIO, EduNames.PYTHON, EduNames.JAVASCRIPT)
    val linkText = EduCoreBundle.message("course.dialog.go.to.website")
    val linkInfo = LinkInfo(linkText, CheckiONames.CHECKIO_OAUTH_HOST)
    return TabInfo(infoText, linkInfo, loginComponent)
  }

  private inner class CheckiOLoginPanel : LoginPanel(true,
                                                     EduCoreBundle.message("course.dialog.log.in.label.before.link"),
                                                     EduCoreBundle.message("course.dialog.log.in.to", CheckiONames.CHECKIO),
                                                     {
                                                       val checkiOConnectorProvider = (selectedCourse?.configurator as CheckiOConnectorProvider?)!!
                                                       val checkiOOAuthConnector = checkiOConnectorProvider.oAuthConnector
                                                       checkiOOAuthConnector.doAuthorize(
                                                         Runnable { coursePanel.hideErrorPanel() },
                                                         Runnable { coursePanel.hideErrorPanel() },
                                                         Runnable { doValidation(selectedCourse) }
                                                       )
                                                     })
}
