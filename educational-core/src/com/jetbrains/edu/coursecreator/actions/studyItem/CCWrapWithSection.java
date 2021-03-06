package com.jetbrains.edu.coursecreator.actions.studyItem;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCStudyItemPathInputValidator;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.messages.EduCoreActionBundle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.edu.coursecreator.StudyItemType.SECTION_TYPE;
import static com.jetbrains.edu.coursecreator.StudyItemTypeKt.getPresentableTitleName;

public class CCWrapWithSection extends DumbAwareAction {
  protected static final Logger LOG = Logger.getInstance(CCWrapWithSection.class);

  public CCWrapWithSection() {
    super(EduCoreActionBundle.lazyMessage("wrap.with.section"), EduCoreActionBundle.lazyMessage("wrap.with.section.description"), null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    final VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
    if (project == null || virtualFiles == null) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }

    final ArrayList<Lesson> lessonsToWrap = getLessonsToWrap(virtualFiles, course);

    wrapLessonsIntoSection(project, course, lessonsToWrap);
  }

  @NotNull
  private static ArrayList<Lesson> getLessonsToWrap(@NotNull VirtualFile[] virtualFiles, @NotNull Course course) {
    final ArrayList<Lesson> lessonsToWrap = new ArrayList<>();
    for (VirtualFile file : virtualFiles) {
      final Lesson lesson = course.getLesson(file.getName());
      if (lesson != null) {
        lessonsToWrap.add(lesson);
      }
    }
    return lessonsToWrap;
  }

  public static void wrapLessonsIntoSection(@NotNull Project project, @NotNull Course course, @NotNull List<Lesson> lessonsToWrap) {
    if (lessonsToWrap.isEmpty()) {
      return;
    }
    int sectionIndex = course.getSections().size() + 1;
    InputValidator validator = new CCStudyItemPathInputValidator(course, SECTION_TYPE, OpenApiExtKt.getCourseDir(project));
    String sectionName = Messages.showInputDialog(EduCoreActionBundle.message("wrap.with.section.enter.name"),
                                                  getPresentableTitleName(SECTION_TYPE), null, EduNames.SECTION + sectionIndex, validator);
    if (sectionName == null) {
      return;
    }
    CCUtils.wrapIntoSection(project, course, lessonsToWrap, sectionName);
  }

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getProject();
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    if (project == null || !CCUtils.isCourseCreator(project)) {
      return;
    }
    final VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
    if (virtualFiles == null || virtualFiles.length == 0) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    final ArrayList<Lesson> lessonsToWrap = getLessonsToWrap(virtualFiles, course);
    if (!lessonsToWrap.isEmpty()) {
      presentation.setEnabledAndVisible(true);
    }
  }
}
