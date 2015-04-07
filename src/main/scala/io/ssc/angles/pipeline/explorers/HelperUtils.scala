package io.ssc.angles.pipeline.explorers

import org.apache.commons.lang3.StringUtils

/**
 * Created by xolor on 06.03.15.
 */
object HelperUtils {

  val toReplace = Array(" ", "|", ";", "(", ")", "[", "]", ",", "\"", "'", ":", "!", "-", "@", "{", "|", "}", "<", ">", "^", "\\")
  val replaceWith = Array("%20", "%7C", "%3B", "%28", "%29", "%5B", "%5D", "%2C", "%22", "%27", "%3A", "%21", "%2D", "%40", "%7B", "%7C", "%7D", "%3C", "%3D", "%5E", "%5C")

  def escapeURIString(uri: String): String = {
    var uriTemp = StringUtils.trim(uri)
    if ('\"'.equals(uriTemp.charAt(0))) {
      if ('\"'.equals(uriTemp.last)) {
        uriTemp = StringUtils.removeEnd(uriTemp, "\"")
      }
      uriTemp = StringUtils.removeStart(uriTemp, "\"")
    }

    // Find the "/" before the param string
    val paramSlashPos = StringUtils.ordinalIndexOf(uriTemp, "/", 3)
    var uriParams = StringUtils.substring(uriTemp, paramSlashPos + 1)
    val uriPreParam = StringUtils.substring(uriTemp, 0, paramSlashPos + 1)

    uriParams = StringUtils.replaceEach(uriParams, toReplace, replaceWith)

    uriPreParam + uriParams
  }

}
