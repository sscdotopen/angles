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
