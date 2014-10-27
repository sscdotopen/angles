package io.ssc.angles.pipeline.http

import org.htmlcleaner.HtmlCleaner

object Utils {

  val cleaner = new HtmlCleaner

  val cleanerProperties = cleaner.getProperties
  cleanerProperties.setTranslateSpecialEntities(true)
  cleanerProperties.setAdvancedXmlEscape(true)
  cleanerProperties.setTransResCharsToNCR(true)
  cleanerProperties.setTransSpecialEntitiesToNCR(true)
  cleanerProperties.setAllowHtmlInsideAttributes(false)
  cleanerProperties.setKeepWhitespaceAndCommentsInHead(false)
  cleanerProperties.setOmitCdataOutsideScriptAndStyle(true)
  cleanerProperties.setOmitComments(true)
  cleanerProperties.setIgnoreQuestAndExclam(true)
  cleanerProperties.setRecognizeUnicodeChars(true)
  cleanerProperties.setOmitComments(true)

  def clean(html: String) = {
    cleaner.clean(html)
  }

}
