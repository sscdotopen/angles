package io.ssc.angles

import java.util.Properties


object Config {

  val props = new Properties()
  props.load(getClass.getResourceAsStream("/angles-conf.properties"))

  def property(key: String) = props.getProperty(key)
}
