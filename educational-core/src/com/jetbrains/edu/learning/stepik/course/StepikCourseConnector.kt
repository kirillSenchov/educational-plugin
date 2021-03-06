package com.jetbrains.edu.learning.stepik.course

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import com.jetbrains.edu.learning.stepik.StepikLanguage
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.*

object StepikCourseConnector {
  private val LOG = Logger.getInstance(StepikCourseConnector::class.java.name)

  @JvmStatic
  fun getCourseIdFromLink(link: String): Int {
    try {
      val url = URL(link)
      val pathParts = url.path.split("/").dropLastWhile { it.isEmpty() }
      for (i in pathParts.indices) {
        val part = pathParts[i]
        if (part == "course" && i + 1 < pathParts.size) {
          return Integer.parseInt(pathParts[i + 1])
        }
      }
    }
    catch (e: MalformedURLException) {
      LOG.warn(e.message)
    }

    return -1
  }

  fun getCourseInfoByLink(link: String): EduCourse? {
    val courseId: Int = try {
      Integer.parseInt(link)
    }
    catch (e: NumberFormatException) {
      getCourseIdFromLink(link)
    }

    if (courseId != -1) {
      val info = StepikConnector.getInstance().getCourseInfo (courseId) ?: return null

      // do not convert idea_compatible courses to StepikCourse
      return if (info.isCompatible) info else stepikCourseFromRemote(info)
    }
    return null
  }

  fun getSupportedLanguages(remoteCourse: StepikCourse): List<StepikLanguage> {
    val languages = ArrayList<StepikLanguage>()
    try {
      val codeTemplates = getFirstCodeTemplates(remoteCourse)
      for (languageName in codeTemplates.keys) {
        val stepikLanguage = StepikLanguage.langOfName(languageName)
        val id = stepikLanguage.id
        val language = Language.findLanguageByID(id) ?: continue
        if (language.id in EduConfiguratorManager.supportedEduLanguages) {
          languages.add(stepikLanguage)
        }
      }
    }
    catch (e: IOException) {
      LOG.warn(e.message)
    }

    return languages
  }

  private fun getFirstCodeTemplates(remoteCourse: StepikCourse): Map<String, String> {
    val unitsIds = StepikCourseLoader.getUnitsIds(remoteCourse)
    val lessons = StepikCourseLoader.getLessonsFromUnitIds(unitsIds)
    for (lesson in lessons) {
      val allStepSources = StepikConnector.getInstance().getStepSources(lesson.steps)

      for (stepSource in allStepSources) {
        val step = stepSource.block
        if (step != null && step.name == "code" && step.options != null) {
          val codeTemplates = (step.options as PyCharmStepOptions).codeTemplates
          if (codeTemplates != null) {
            return codeTemplates
          }
        }
      }
    }
    return emptyMap()
  }
}
