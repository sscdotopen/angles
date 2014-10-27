package io.ssc.angles.pipeline.http

import java.util.Locale

import org.htmlcleaner.TagNode

import scala.collection.mutable


class MetadataExtractor(doc: TagNode) {

  def title(): List[String] = {
    val titleNode = doc.findElementByName("title", true)
    if (titleNode != null) {
      List(titleNode.getText.toString)
    } else {
      List()
    }
  }

  def metaKeywords(): List[String] = {
    parse("name", "keywords") { node: TagNode =>
      if (node.getAttributeByName("content") != null) {
        (node.getAttributeByName("content").split(",") map { _.toLowerCase(Locale.GERMAN) }).toList
      } else {
        List()
      }
    } ++ parse("name", "news_keywords") { node: TagNode =>
      if (node.getAttributeByName("content") != null) {
        (node.getAttributeByName("content").split(",") map { _.toLowerCase(Locale.GERMAN) }).toList
      } else {
        List()
      }
    }
  }

  def metaName(name: String) = parse("name", name) { attribute(_, "content") }
  def metaProperty(property: String) = parse("property", property) { attribute(_, "content") }
  def metaItemProb(property: String) = parse("itemprob", property) { attribute(_, "content") }

  private[this] def attribute(node: TagNode, attribute: String): List[String] = {
    if (node.hasAttribute(attribute)) {
      List(node.getAttributeByName(attribute))
    } else {
      List()
    }
  }

  private[this] def replacements = Map(
    "&quot;" -> "\"",
    "&amp;" -> "&",
    "&nbsp;" -> " ",
    "&raquo;" -> "»",
    "&#8230;" -> "…",
    "&rsquo;" -> "’",
    "&#x27;" -> "'",
    "&#39;" -> "'"
  )

  private[this] def clean(value: String) = {
    var cleanedValue = value
    for ((pattern, replacement) <- replacements) {
      cleanedValue = cleanedValue.replaceAll(pattern, replacement)
    }
    cleanedValue.trim
  }

  private[this] def parse(searchAttribute: String, searchValue: String)(f: TagNode => List[String]): List[String] = {
    val buffer = mutable.ListBuffer[String]()
    val node = doc.findElementByAttValue(searchAttribute, searchValue, true, false)
    if (node != null) {
      val values = f(node).map { clean(_) }
                          .filter { _ != "" }
      buffer ++= values
    }
    buffer.toList
  }

}
