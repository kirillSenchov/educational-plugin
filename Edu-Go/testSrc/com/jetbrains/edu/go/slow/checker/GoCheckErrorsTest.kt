package com.jetbrains.edu.go.slow.checker

import com.goide.GoLanguage
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.nullValue
import org.hamcrest.CoreMatchers
import org.junit.Assert

class GoCheckErrorsTest : GoCheckersTestBase() {
  override fun createCourse(): Course {
    return course(language = GoLanguage.INSTANCE) {
      lesson {
        eduTask("EduTestFailed") {
          goTaskFile("task.go", """
            package task

            // todo: replace this with an actual task
            func Sum(a, b int) int {
              return a + b + 1
            }
          """)
          taskFile("go.mod", """
            module task1
          """)
          goTaskFile("test/task_test.go", """
            package test
            
            import (
            	task "task1"
            	"testing"
            )
            
            //todo: replace this with an actual test
            func TestSum(t *testing.T) {
            	type args struct {
            		a int
            		b int
            	}
            	tests := []struct {
            		name string
            		args args
            		want int
            	}{
            		{"1", args{1, 1}, 2},
            		{"2", args{1, 2}, 3},
            	}
            	for _, tt := range tests {
            		t.Run(tt.name, func(t *testing.T) {
            			if got := task.Sum(tt.args.a, tt.args.b); got != tt.want {
            				t.Errorf("Sum() = %v, want %v", got, tt.want)
            			}
            		})
            	}
            }

          """)
        }
        outputTask("OutputTestFailed") {
          goTaskFile("main.go", """
              package main
              import "fmt"

              func main() {
	              fmt.Print("No")
              }
          """)
          taskFile("go.mod", """
            module task2
          """)
          taskFile("output.txt", "Yes")
        }
        outputTask("OutputMultilineTestFailed") {
          goTaskFile("main.go", """
              package main
              import "fmt"

              func main() {
	              fmt.Println("1\n2")
              }
          """)
          taskFile("go.mod", """
            module task3
          """)
          taskFile("output.txt") {
            withText("1\n\n2\n\n")
          }
        }
      }
    }
  }

  fun `test go errors`() {
    val incorrect = EduCoreBundle.message("check.incorrect")
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals(CheckStatus.Failed, checkResult.status)
      val (messageMatcher, diffMatcher) = when (task.name) {
        "EduTestFailed" -> CoreMatchers.equalTo(incorrect) to nullValue()
        "OutputTestFailed" -> CoreMatchers.equalTo(incorrect) to
          CheckResultDiffMatcher.diff(CheckResultDiff(expected = "Yes", actual = "No"))
        "OutputMultilineTestFailed" -> CoreMatchers.equalTo(incorrect) to
          CheckResultDiffMatcher.diff(CheckResultDiff(expected = "1\n\n2\n\n", actual = "1\n2\n"))
        else -> error("Unexpected task name: ${task.name}")
      }
      Assert.assertThat("Checker output for ${task.name} doesn't match", checkResult.message, messageMatcher)
      Assert.assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, diffMatcher)
    }
    doTest()
  }
}