package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.coursecreator.CCTestCase.checkPainters
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.handlers.AnswerPlaceholderDeleteHandler

abstract class AnswerPlaceholderTestBase : EduActionTestCase() {
  val defaultPlaceholderText = "type here"
  val defaultTaskText = "fun foo(): String = TODO()"

  fun doTest(
    name: String,
    action: CCAnswerPlaceholderAction,
    taskFile: TaskFile,
    taskFileExpected: TaskFile
  ) {
    val taskFileUnchanged = copy(taskFile)
    val virtualFile = findFile(name)
    myFixture.openFileInEditor(virtualFile)
    if (taskFileExpected.answerPlaceholders.isNotEmpty()) {
      val placeholderExpected = taskFileExpected.answerPlaceholders[0]
      if (placeholderExpected.offset != 0 && placeholderExpected.endOffset != 9 && action !is CCEditAnswerPlaceholder) {
        myFixture.editor.selectionModel.setSelection(placeholderExpected.offset, placeholderExpected.endOffset)
      }
    }
    myFixture.testAction(action)

    checkPlaceholders(taskFileExpected, taskFile)
    checkPainters(taskFile)

    if (action is CCAddAnswerPlaceholder) {
      val document = myFixture.getDocument(myFixture.file)
      val handler = EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(document)
      assertInstanceOf(handler, AnswerPlaceholderDeleteHandler::class.java)
    }
    //TODO: CCEditAnswerPlaceholder is not an UndoableAction
    if (action !is CCEditAnswerPlaceholder) {
      UndoManager.getInstance(project).undo(FileEditorManager.getInstance(project).getSelectedEditor(virtualFile))
      assertEquals(taskFileUnchanged.name, taskFile.name)
      assertEquals(taskFileUnchanged.text, taskFile.text)
      assertEquals(taskFileUnchanged.answerPlaceholders.size, taskFile.answerPlaceholders.size)
      assertEquals(taskFileUnchanged.task, taskFile.task)
      checkPlaceholders(taskFileUnchanged, taskFile)
    }
  }

  private fun checkPlaceholders(taskFileExpected: TaskFile, taskFileActual: TaskFile) {
    val placeholdersActual = taskFileActual.answerPlaceholders
    val placeholdersExpected = taskFileExpected.answerPlaceholders
    assertEquals(placeholdersExpected.size, placeholdersActual.size)
    placeholdersExpected.forEachIndexed { i, placeholderExpected ->
      run {
        val placeholderActual = placeholdersActual[i]
        assertNotNull(placeholderActual)
        assertEquals(placeholderExpected.offset, placeholderActual.offset)
        assertEquals(placeholderExpected.length, placeholderActual.length)
        assertEquals(placeholderExpected.index, placeholderActual.index)
        assertEquals(placeholderExpected.placeholderText, placeholderActual.placeholderText)
        assertEquals(placeholderExpected.possibleAnswer, placeholderActual.possibleAnswer)
        assertEquals(placeholderExpected.taskFile.text, placeholderActual.taskFile.text)
        assertEquals(placeholderExpected.taskFile.name, placeholderActual.taskFile.name)

        val expectedDependency = placeholderExpected.placeholderDependency
        if (expectedDependency == null) {
          assertNull(placeholderActual.placeholderDependency)
        }
        else {
          val actualDependency = placeholderActual.placeholderDependency
          assertNotNull(actualDependency!!)
          assertEquals(expectedDependency.fileName, actualDependency.fileName)
          assertEquals(expectedDependency.taskName, actualDependency.taskName)
          assertEquals(expectedDependency.lessonName, actualDependency.lessonName)
          assertEquals(expectedDependency.isVisible, actualDependency.isVisible)
        }
      }
    }
  }

  fun copy(taskFile: TaskFile): TaskFile {
    val copy = TaskFile()
    TaskFile.copy(taskFile, copy)
    copy.setText(taskFile.text)
    copy.task = taskFile.task
    taskFile.answerPlaceholders.forEachIndexed { i, taskFilePlaceholder ->
      run {
        copy.answerPlaceholders[i].taskFile = taskFilePlaceholder.taskFile
      }
    }
    return copy
  }
}