package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.labels.ActionLink
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.event.HyperlinkEvent

class HyperskillProjectAction : DumbAwareAction("Open ${EduNames.JBA} Project") {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isHyperskillSupportAvailable()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val account = HyperskillSettings.INSTANCE.account
    if (account == null || HyperskillConnector.getInstance().getCurrentUser(account) == null) {
      showBalloon(e, "Please <a href=\"\">login to ${EduNames.JBA}</a> and select a project.", true)
    }
    else {
      openHyperskillProject(account) { error -> showBalloon(e, error, false) }
    }
  }

  private fun showBalloon(e: AnActionEvent, message: String, authorize: Boolean) {
    val builder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
    builder.setHideOnClickOutside(true)
    builder.setClickHandler(HSHyperlinkListener(authorize), true)
    val balloon = builder.createBalloon()

    val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
    if (component is ActionLink) {
      balloon.showInCenterOf(component)
    }
    else {
      val relativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(e.dataContext)
      balloon.show(relativePoint, Balloon.Position.above)
    }
  }

  companion object {
    fun openHyperskillProject(account: HyperskillAccount, showError: (String) -> Unit) {
      val projectId = getSelectedProjectIdUnderProgress(account)
      if (projectId == null) {
        showError(SELECT_PROJECT)
        return
      }

      val result = HyperskillProjectOpener.open(HyperskillOpenStageRequest(projectId, null))
      if (result is Err) {
        showError(result.error)
      }
    }
  }
}

class HSHyperlinkListener(private val authorize: Boolean) : ActionListener, NotificationListener {
  override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
    authorizeOrBrowse()
  }

  override fun actionPerformed(e: ActionEvent?) {
    authorizeOrBrowse()
  }

  private fun authorizeOrBrowse() {
    if (authorize) {
      HyperskillConnector.getInstance().doAuthorize()
      EduCounterUsageCollector.loggedIn(HYPERSKILL, EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
    }
    else {
      BrowserUtil.browse(HYPERSKILL_PROJECTS_URL)
    }
  }
}
