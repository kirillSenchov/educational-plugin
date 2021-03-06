package com.jetbrains.edu.learning.taskDescription.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.jetbrains.edu.learning.EduUtils;

public class EduCondition implements Condition<Project>, DumbAware {
  @Override
  public boolean value(Project project) {
    return EduUtils.isEduProject(project);
  }
}
