package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.CourseSetListener
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.yaml.YamlTestCase
import com.jetbrains.edu.learning.yaml.checkConfigsExistAndNotEmpty

class YamlMigrationTest : YamlTestCase() {
  fun `test course doesn't serialize into xml`() {
    StudyTaskManager.getInstance(project).course = EduCourse()
    StudyTaskManager.getInstance(project).course!!.courseMode = CCUtils.COURSE_MODE
    assertNull(StudyTaskManager.getInstance(project).state)
  }

  fun `test yaml course doesn't loaded for yaml`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask()
      }
    }
    createStudyXml()
    createConfigFiles(project)

    val serializedCourse = StudyTaskManager.getInstance(project).serialize()
    StudyTaskManager.getInstance(project).loadState(serializedCourse)
    UIUtil.dispatchAllInvocationEvents()
    val connection = project.messageBus.connect()
    connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
      override fun courseSet(course: Course) {
        connection.disconnect()
        fail("Course is loaded in yaml project ")
      }
    })

    StudyTaskManager.getInstance(project).loadState(JDOMUtil.load("<component name=\"StudySettings\"></component>"))
    connection.disconnect()
  }

  fun `test configs created after migration`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask()
      }
    }
    createStudyXml()

    val serializedCourse = StudyTaskManager.getInstance(project).serialize()
    StudyTaskManager.getInstance(project).loadState(serializedCourse)
    UIUtil.dispatchAllInvocationEvents()
    checkConfigsExistAndNotEmpty(project, StudyTaskManager.getInstance(project).course!!)
  }

  private fun createStudyXml() {
    val projectDir = StudyTaskManager.getInstance(project).course!!.getDir(project)
    runInEdt {
      runWriteAction {
        val ideaDir = projectDir.createChildDirectory(this, ".idea")
        ideaDir.createChildData(this, "study_project.xml")
      }
    }
  }
}