package com.jetbrains.edu.coursecreator.actions.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonAppend
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.AnnotatedClass
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter
import com.fasterxml.jackson.databind.util.Annotations
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ENVIRONMENT
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ITEMS
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.LANGUAGE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PLUGIN_VERSION
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.SUMMARY
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TITLE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.VENDOR
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.VERSION
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.pluginVersion

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonAutoDetect(setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder(VERSION, ENVIRONMENT, SUMMARY, TITLE, PROGRAMMING_LANGUAGE, LANGUAGE, COURSE_TYPE, PLUGIN_VERSION,  VENDOR,
                   SOLUTIONS_HIDDEN, ITEMS)
@JsonAppend(props = [JsonAppend.Prop(VersionPropertyWriter::class, name = VERSION, type = Int::class),
                     JsonAppend.Prop(PluginVersionPropertyWriter::class, name = PLUGIN_VERSION, type = String::class)])
abstract class MarketplaceCourseMixin : LocalEduCourseMixin() {

  @JsonProperty(VENDOR)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private val myVendor: Vendor? = null
}

private class PluginVersionPropertyWriter : VirtualBeanPropertyWriter {

  @Suppress("unused")
  constructor()

  constructor(propDef: BeanPropertyDefinition, contextAnnotations: Annotations, declaredType: JavaType) : super(propDef,
                                                                                                                contextAnnotations,
                                                                                                                declaredType)

  override fun withConfig(config: MapperConfig<*>?,
                          declaringClass: AnnotatedClass,
                          propDef: BeanPropertyDefinition,
                          type: JavaType): VirtualBeanPropertyWriter {
    return PluginVersionPropertyWriter(propDef, declaringClass.annotations, type)
  }

  override fun value(bean: Any, gen: JsonGenerator, prov: SerializerProvider): Any {
    return pluginVersion(EduNames.PLUGIN_ID) ?: "unknown"
  }
}
