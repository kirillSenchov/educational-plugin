package com.jetbrains.edu.learning.checkio.checker

import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResult.Companion.failedToCheck
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.details.CheckDetailsView.Companion.getInstance
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class CheckiOTaskChecker(
  task: EduTask,
  private val envChecker: EnvironmentChecker,
  project: Project,
  oAuthConnector: CheckiOOAuthConnector,
  interpreterName: String,
  testFormTargetUrl: String
) : TaskChecker<EduTask>(task, project) {

  private val missionCheck: CheckiOMissionCheck = when (EduSettings.getInstance().javaUiLibraryWithCheck) {
    JavaUILibrary.JCEF -> {
      getJCEFCheckiOMissionCheck(project, task, oAuthConnector, interpreterName, testFormTargetUrl)
      ?: throw RuntimeException("Can't get JCEF CheckiO checker")
    }
    else -> JavaFxCheckiOMissionCheck(project, task, oAuthConnector, interpreterName, testFormTargetUrl)
  }

  override fun check(indicator: ProgressIndicator): CheckResult {
    return try {
      val possibleError = envChecker.checkEnvironment(project)
      if (possibleError != null) {
        return CheckResult(CheckStatus.Unchecked, possibleError)
      }

      // workaround for JBR-2259
      // panel must be shown with JCEF, otherwise request won't be done
      val isJCEF = EduSettings.getInstance().javaUiLibraryWithCheck == JavaUILibrary.JCEF
      if (isJCEF) {
        getInstance(project).showResult("CheckiO Response", missionCheck.getPanel())
      }

      val checkResult = ApplicationUtil.runWithCheckCanceled(missionCheck, ProgressManager.getInstance().progressIndicator)

      if (!isJCEF && checkResult.status != CheckStatus.Unchecked) {
        getInstance(project).showResult("CheckiO Response", missionCheck.getPanel())
      }
      checkResult
    }
    catch (e: Exception) {
      LOG.warn(e.message)
      failedToCheck
    }
  }
}