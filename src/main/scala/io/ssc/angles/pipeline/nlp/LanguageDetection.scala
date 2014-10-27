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

package io.ssc.angles.pipeline.nlp

import com.cybozu.labs.langdetect.DetectorFactory
import io.ssc.angles.Config
import org.slf4j.LoggerFactory

object LanguageDetection {

  val log = LoggerFactory.getLogger(LanguageDetection.getClass)

  DetectorFactory.loadProfile(Config.property("angles.dataDir") + "/language-profiles")

  def isGerman(text: String): Boolean = {
    val detector = DetectorFactory.create()
    detector.append(text)
    try {
      val mostProbableLanguage = detector.detect()
      mostProbableLanguage == "de"
    } catch {
      case t: Throwable =>
        log.warn("Unable to detect language")
        false
    }
  }

}
