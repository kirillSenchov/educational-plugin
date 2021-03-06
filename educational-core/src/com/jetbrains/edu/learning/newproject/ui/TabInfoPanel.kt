package com.jetbrains.edu.learning.newproject.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.ColorUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

private const val FONT_SIZE = 13.0f
private const val LEFT_RIGHT_OFFSET = 13

private const val TOP_LOGIN_OFFSET = 17
private const val BOTTOM_LOGIN_OFFSET = 5

private const val TOP_BOTTOM_INFO_OFFSET = 13

class TabInfoPanel(tabInfo: TabInfo) : JPanel() {
  private val infoText: String
  private val infoLink: LinkInfo?
  private val loginComponent: JPanel?

  init {
    layout = BorderLayout()

    infoText = tabInfo.description
    infoLink = tabInfo.linkInfo
    loginComponent = tabInfo.loginComponent

    if (loginComponent != null) {
      loginComponent.border = JBUI.Borders.empty(TOP_LOGIN_OFFSET, LEFT_RIGHT_OFFSET, BOTTOM_LOGIN_OFFSET, LEFT_RIGHT_OFFSET)
      add(loginComponent, BorderLayout.NORTH)
    }

    val infoPanel = TabInfoHtmlPanel()
    infoPanel.border = JBUI.Borders.empty(TOP_BOTTOM_INFO_OFFSET, LEFT_RIGHT_OFFSET)
    val scrollPane = JBScrollPane(infoPanel).apply {
      verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      border = null
    }
    add(scrollPane, BorderLayout.CENTER)
  }

  fun hideLoginPanel() {
    loginComponent?.isVisible = false
  }

  private inner class TabInfoHtmlPanel : HtmlPanel() {
    init {
      super.update()
    }

    private val link get() = if (infoLink != null) " <a href='${infoLink.url}'>${infoLink.text}</a>" else ""

    override fun getBody(): String = "<span style='color: $color'>$infoText</span> $link"

    private val color get() = "#${ColorUtil.toHex(GRAY_COLOR)}"

    override fun getBodyFont(): Font = Font(TypographyManager().bodyFont, Font.PLAIN, JBUI.scaleFontSize(FONT_SIZE))
  }
}

open class LoginPanel(isVisible: Boolean, beforeLinkText: String, linkText: String, loginHandler: () -> Unit) : JPanel(BorderLayout()) {

  init {
    val hyperlinkLabel = HyperlinkLabel().apply {
      setHyperlinkText("$beforeLinkText ", linkText, "")
      addHyperlinkListener { loginHandler() }
      setIcon(AllIcons.General.BalloonInformation)
      foreground = beforeLinkForeground
      font = Font(TypographyManager().bodyFont, Font.PLAIN, JBUI.scaleFontSize(FONT_SIZE))
      iconTextGap = 5
    }

    this.add(hyperlinkLabel, BorderLayout.CENTER)
    this.isVisible = isVisible
  }

  open val beforeLinkForeground: Color
    get() = UIUtil.getLabelForeground()
}

class TabInfo(val description: String, val linkInfo: LinkInfo? = null, val loginComponent: LoginPanel? = null)

class LinkInfo(val text: String, val url: String)