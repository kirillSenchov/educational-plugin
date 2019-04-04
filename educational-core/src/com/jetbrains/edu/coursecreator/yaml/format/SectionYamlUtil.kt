@file:JvmName("SectionYamlUtil")

package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jetbrains.edu.coursecreator.yaml.InvalidYamlFormatException
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem

private const val CONTENT = "content"

/**
 * Mixin class is used to deserialize [Section] item.
 * Update [SectionChangeApplier] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonDeserialize(builder = SectionBuilder::class)
abstract class SectionYamlMixin {
  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var items: List<StudyItem>
}

@JsonPOJOBuilder(withPrefix = "")
private class SectionBuilder(@JsonProperty(CONTENT) val content: List<String?>) {
  @Suppress("unused") //used for deserialization
  private fun build(): Section {
    val section = Section()
    val items = content.mapIndexed { index: Int, title: String? ->
      if (title == null) {
        throw InvalidYamlFormatException("Unnamed item")
      }
      val titledStudyItem = TitledStudyItem(title)
      titledStudyItem.index = index
      titledStudyItem
    }
    section.items = items
    return section
  }
}

class SectionChangeApplier<T : Section> : StudyItemChangeApplier<T>() {
  override fun applyChanges(existingItem: T, deserializedItem: T) {
    updateItemContainerChildren(existingItem, deserializedItem)
  }
}