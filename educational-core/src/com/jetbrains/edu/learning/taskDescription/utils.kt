@file:JvmName("TaskDescriptionUtil")

package com.jetbrains.edu.learning.taskDescription

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.KeymapUtil
import com.jetbrains.edu.learning.stepik.SOURCE
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

private const val SHORTCUT = "shortcut"
private const val SHORTCUT_ENTITY = "&$SHORTCUT:"
private const val SHORTCUT_ENTITY_ENCODED = "&amp;$SHORTCUT:"
private const val VIDEO_TAG = "video"
private const val IFRAME_TAG = "iframe"
private const val YOUTUBE_VIDEO_ID_LENGTH = 11
const val IMG_TAG = "img"
const val SCRIPT_TAG = "script"
const val SRC_ATTRIBUTE = "src"
private val LOG: Logger = Logger.getInstance("com.jetbrains.edu.learning.taskDescription.utils")
private val HYPERSKILL_TAGS = tagsToRegex({ "\\[$it](.*)\\[/$it]" }, "HINT", "PRE", "META") +
                              tagsToRegex({ "\\[$it-\\w+](.*)\\[/$it]" }, "ALERT")
private val YOUTUBE_LINKS_REGEX = "https?://(www\\.)?(youtu\\.be|youtube\\.com)/?(watch\\?v=|embed)?.*".toRegex()

private fun tagsToRegex(pattern: (String) -> String, vararg tags: String): List<Regex> = tags.map { pattern(it).toRegex() }

// see EDU-2444
fun removeHyperskillTags(text: StringBuffer) {
  var result: String = text.toString()
  for (regex in HYPERSKILL_TAGS) {
    result = result.replace(regex) { it.groupValues[1] }
  }

  text.delete(0, text.length)
  text.append(result)
}

fun replaceActionIDsWithShortcuts(text: StringBuffer) {
  var lastIndex = 0
  while (lastIndex < text.length) {
    lastIndex = text.indexOf(SHORTCUT_ENTITY, lastIndex)
    var shortcutEntityLength = SHORTCUT_ENTITY.length
    if (lastIndex < 0) {
      //`&` symbol might be replaced with `&amp;`
      lastIndex = text.indexOf(SHORTCUT_ENTITY_ENCODED)
      if (lastIndex < 0) {
        return
      }
      shortcutEntityLength = SHORTCUT_ENTITY_ENCODED.length
    }
    val actionIdStart = lastIndex + shortcutEntityLength
    val actionIdEnd = text.indexOf(";", actionIdStart)
    if (actionIdEnd < 0) {
      return
    }
    val actionId = text.substring(actionIdStart, actionIdEnd)
    var shortcutText = KeymapUtil.getFirstKeyboardShortcutText(actionId)
    if (shortcutText.isEmpty()) {
      shortcutText = "<no shortcut for action $actionId>"
    }
    text.replace(lastIndex, actionIdEnd + 1, shortcutText)
    lastIndex += shortcutText.length
  }
}


fun processYoutubeLink(text: String, taskId: Int): String {
  val document = Jsoup.parse(text)
  val videoElements = document.getElementsByTag(VIDEO_TAG)
  for (element in videoElements) {
    val sourceElements = element.getElementsByTag(SOURCE)
    if (sourceElements.size != 1) {
      LOG.warn("Incorrect number of youtube video sources for task ${taskId}")
      continue
    }
    val src = sourceElements.attr(SRC_ATTRIBUTE)
    val elementToReplaceWith = getClickableImageElement(src, taskId) ?: continue
    element.replaceWith(elementToReplaceWith)
  }
  val iframeElements = document.getElementsByTag(IFRAME_TAG)
  for (element in iframeElements) {
    val src = element.attr(SRC_ATTRIBUTE)
    val elementToReplace = getClickableImageElement(src, taskId) ?: continue
    element.replaceWith(elementToReplace)
  }
  return document.outerHtml()
}

private fun getClickableImageElement(src: String, taskId: Int): Element? {
  val youtubeVideoId = src.getYoutubeVideoId()
  if (youtubeVideoId == null) {
    LOG.warn("Incorrect youtube video link ${src} for task ${taskId}")
    return null
  }
  val textToReplace = "<a href=http://www.youtube.com/watch?v=${youtubeVideoId}><img src=http://img.youtube.com/vi/${youtubeVideoId}/0.jpg></a>"
  val documentToReplace = Jsoup.parse(textToReplace)
  val elements = documentToReplace.getElementsByTag("a")
  return if (elements.isNotEmpty()) {
    elements[0]
  }
  else null
}

fun String.getYoutubeVideoId(): String? {
  if (!YOUTUBE_LINKS_REGEX.matches(this)) {
    return null
  }
  val splitLink = this.split("?v=", "/embed/", ".be/", "&", "?")
  return if (splitLink.size >= 2) {
    val id = splitLink[1]
    return if (id.length == YOUTUBE_VIDEO_ID_LENGTH) id
    else null
  }
  else {
    null
  }
}

fun String.containsYoutubeLink(): Boolean = contains(YOUTUBE_LINKS_REGEX)

fun String.replaceEncodedShortcuts() = this.replace(SHORTCUT_ENTITY_ENCODED, SHORTCUT_ENTITY)

fun String.toShortcut(): String = "${SHORTCUT_ENTITY}$this;"

fun String.containsShortcut(): Boolean = startsWith(SHORTCUT_ENTITY) || startsWith(SHORTCUT_ENTITY_ENCODED)

fun link(url: String, text: String, right: Boolean = false): String = """<a${if (right) " class=right " else " "}href="$url">$text</a>"""
