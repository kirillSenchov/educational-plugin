package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.yaml.format.TaskFileBuilder
import com.jetbrains.edu.learning.yaml.format.TaskFileYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE

@JsonDeserialize(builder = StudentTaskFileBuilder::class)
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, TEXT)
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
abstract class StudentTaskFileYamlMixin : TaskFileYamlMixin() {
  @JsonProperty(TEXT)
  private lateinit var myText: String
}

private class StudentTaskFileBuilder(
  val text: String?,
  name: String?,
  placeholders: List<AnswerPlaceholder> = mutableListOf(),
  visible: Boolean = true
) : TaskFileBuilder(name, placeholders, visible) {
  override fun createTaskFile(): TaskFile {
    return super.createTaskFile().also { it.setText(text) }
  }
}
