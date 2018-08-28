package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.RefreshTaskFileAction
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSeparator


class TaskDescriptionPanel : SimpleToolWindowPanel(true, true), DataProvider, Disposable {
  override fun dispose() {

  }

  val icon = object: AsyncProcessIcon("check") {
    override fun setVisible(aFlag: Boolean) {
      super.setVisible(aFlag)
    }
  }

  init {
    val defaultBackground = EditorColorsManager.getInstance().globalScheme.defaultBackground
    icon.isVisible = false
    val nextButton = JButton("Next")
    nextButton.background = defaultBackground
    nextButton.isVisible = false

    val label = JBLabel("<html>sample text<br><br><br>" +
                        "sample text<br><br><br>\n" +
                        "sample text\n" +
                        "sample text<br><br><br>\n" +
                        "sample text\n <br><br><br>" +
                        "<br><br><br>" +
                        "<br><br><br>" +
                        "<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>" +
                        "" +
                        "" +
                        "" +
                        "" +
                        "" +
                        "" +
                        "sample text\n" +
                        "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" +
                        "sample text<br><br><br>\n" +
                        "sample text\n" +
                        "sample text\n" + "sample text\n" + "sample text\n" +
                        "sample text\n" +
                        "sample text\n" + "sample text\n</html>")
    val panel = JPanel(BorderLayout())


    panel.add(JBScrollPane(label), BorderLayout.CENTER)

    val bottomPanel = JPanel(BorderLayout())
    bottomPanel.border = JBUI.Borders.empty(10, 0, 0, 0)
    bottomPanel.add(JSeparator(), BorderLayout.NORTH)


    val action = ActionManager.getInstance().getAction(CheckAction.ACTION_ID)

    val toolbar = ActionManager.getInstance().createButtonToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(action))



    val checkPanel = JPanel(BorderLayout())
    checkPanel.add(toolbar, BorderLayout.WEST)


    val middlePanel = JPanel(BorderLayout())
    middlePanel.add(icon, BorderLayout.WEST)
    middlePanel.add(JPanel(), BorderLayout.CENTER)
    checkPanel.add(middlePanel, BorderLayout.CENTER)


//    val actionsPanel = JPanel()
//    actionsPanel.add(JBLabel("1"))
//    actionsPanel.add(JBLabel("2"))
//    actionsPanel.border = JBUI.Borders.empty(16 + 3, 0, 0, 0)
//
    val commentAction = object : AnAction(AllIcons.Ide.Notifications), RightAlignedToolbarAction {
      override fun actionPerformed(e: AnActionEvent) {

      }

    }
    val refreshAction = ActionManager.getInstance().getAction(RefreshTaskFileAction.ACTION_ID)
    val toolbar1 = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(
       commentAction), true)
    toolbar1.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
    toolbar1.adjustTheSameSize(true)
//    val component1 = ActionButton(refreshAction, refreshAction.templatePresentation.clone(), ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
    //TODO: compensate depending on OS
//    component1.border = JBUI.Borders.empty(3, 0, 3, 0)

    val toolbar2 = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, DefaultActionGroup(
      refreshAction), true)
    toolbar2.layoutPolicy = ActionToolbar.NOWRAP_LAYOUT_POLICY
    toolbar2.adjustTheSameSize(true)
    val component2 = toolbar2.component
    component2.border = JBUI.Borders.empty(0, 0, 0, 0)
//    component2.insets.set(5, 0, -5, 0)
//    component2.minimumSize = JBUI.size(20, 20)

    val component1 = toolbar1.component
    component1.border = JBUI.Borders.empty(0, 0, 0, 0)
//    component1.insets.set(5, 0, -5, 0)
//    component1.minimumSize = JBUI.size(20, 20)

    val actionsPanel = JPanel(HorizontalLayout(0))
    actionsPanel.add(component1)
    actionsPanel.add(component2)


    checkPanel.add(actionsPanel, BorderLayout.EAST)

    checkPanel.border = JBUI.Borders.empty(16, 0, 0, 0)
//    checkPanel.background = JBColor.RED

    bottomPanel.add(checkPanel, BorderLayout.CENTER)

    panel.add(bottomPanel, BorderLayout.SOUTH)
    panel.border = JBUI.Borders.empty(0, 15, 15, 15)

//    UIUtil.setBackgroundRecursively(panel, defaultBackground)

    setContent(panel)
  }

  companion object {
    fun getToolWindow(project: Project): TaskDescriptionPanel? {
      if (project.isDisposed) return null

      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
      if (toolWindow != null) {
        val contents = toolWindow.contentManager.contents
        for (content in contents) {
          val component = content.component
          if (component is TaskDescriptionPanel) {
            return component
          }
        }
      }
      return null
    }
  }
}