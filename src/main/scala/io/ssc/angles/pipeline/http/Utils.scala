/**
 * Angles
 * Copyright (C) 2014  Sebastian Schelter
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
