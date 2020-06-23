package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.TestActionEvent
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.intellij.lang.annotations.Language

class HyperskillSubmissionsTest : EduTestCase() {

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    loginFakeUser()
    configureResponses()
  }

  private fun configureResponses() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      val path = request.path
      MockResponseFactory.fromString(
        when {
          "/api/ws.*".toRegex().matches(path) -> webSocketConfiguration
          "/api/attempts.*".toRegex().matches(path) -> attempt
          "/api/submissions.*".toRegex().matches(path) -> submissions
          else -> "{}"
        }
      )
    }
  }

  fun `test edu tasks submissions loaded`() {
    courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    } as HyperskillCourse

    doTestSubmissionsLoaded(1)
  }

  fun `test code problem submissions loaded`() {
    courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      lesson("Problems") {
        codeTask("task1", stepId = 2) {
          taskFile("Task.txt", "fun foo() {}")
        }
      }
    } as HyperskillCourse

    doTestSubmissionsLoaded(2)
  }

  fun `test submission added after edu task check`() {
    courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 3) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
    } as HyperskillCourse
    doTestSubmissionsAddedAfterTaskCheck(3, EduNames.CORRECT)
  }

  fun `test submission added after code task check with periodically check`() {
    doTestSubmissionsAddedAfterCodeTaskCheck()
  }

  fun `test submission added after code task check with web sockets`() {
    var state: HyperskillCheckCodeTaskTest.MockWebSocketState = HyperskillCheckCodeTaskTest.MockWebSocketState.INITIAL

    mockConnector.withWebSocketListener(object : WebSocketListener() {
      override fun onMessage(webSocket: WebSocket, text: String) {
        when (state) {
          HyperskillCheckCodeTaskTest.MockWebSocketState.INITIAL -> {
            webSocket.confirmConnection()
            state = HyperskillCheckCodeTaskTest.MockWebSocketState.CONNECTION_CONFIRMED
          }
          HyperskillCheckCodeTaskTest.MockWebSocketState.CONNECTION_CONFIRMED -> {
            webSocket.confirmSubscription()
            webSocket.send(submissionResult)
          }
        }
      }
    })

    doTestSubmissionsAddedAfterCodeTaskCheck()
  }

  private fun doTestSubmissionsAddedAfterTaskCheck(taskId: Int, checkStatus: String) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    assertNull("SubmissionsManager should not contain submissions before task check",
               submissionsManager.getSubmissionsFromMemory(setOf(taskId)))

    NavigationUtils.navigateToTask(project, findTask(0, 0))
    val action = CheckAction()
    action.actionPerformed(TestActionEvent(action))

    val future = ApplicationManager.getApplication().executeOnPooledThread { Thread.sleep(1000) }
    EduUtils.waitAndDispatchInvocationEvents(future)

    val submissions = submissionsManager.getSubmissionsFromMemory(setOf(taskId))
    checkSubmissions(submissions, checkStatus)
  }

  private fun doTestSubmissionsAddedAfterCodeTaskCheck() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      lesson("Problems") {
        codeTask("task1", stepId = 4) {
          taskFile("Task.txt", "fun foo() {}")
        }
      }
    } as HyperskillCourse

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "Wrong solution" }

    doTestSubmissionsAddedAfterTaskCheck(4, EduNames.WRONG)
  }

  private fun doTestSubmissionsLoaded(taskId: Int) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    assertNull("SubmissionsManager should not contain submissions before task check",
               submissionsManager.getSubmissionsFromMemory(setOf(taskId)))
    submissionsManager.prepareSubmissionsContent()

    val submissions = submissionsManager.getSubmissions(setOf(taskId))
    checkSubmissions(submissions)
  }

  private fun checkSubmissions(submissions: List<Submission>?, checkStatus: String? = null) {
    assertNotNull(submissions)
    assertTrue(submissions!!.size == 1)
    val submission = submissions[0]
    assertNotNull(submission)
    if (checkStatus != null) {
      assertEquals(checkStatus, submissions[0].status)
    }
  }

  private fun WebSocket.confirmConnection() = send("Connection confirmed")

  private fun WebSocket.confirmSubscription() = send("Subscription confirmed")

  @Language("JSON")
  private val submissions = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "7565799",
          "id": "7565000",
          "status": "wrong",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        }
      ]
    }
  """

  @Language("JSON")
  private val attempt = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": "",
          "id": 7565799,
          "status": "active",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        }
      ]
    }
  """

  @Language("JSON")
  private val submissionResult: String = """
    {
      "result": {
        "channel": "submission#6242591-0",
        "data": {
          "data": $submissions
        }
      }
    }    
   """

  private val webSocketConfiguration = """{"token": "fakeToken","url": "fakeUrl"}"""
}