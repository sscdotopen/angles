package io.ssc.angles.pipeline.titan

import com.thinkaurelius.titan.core.{TitanGraph, TitanFactory}
import org.apache.commons.configuration.PropertiesConfiguration

/**
 * Provides several methods for opening pre-configured titan graphs.
 */
object TitanConnector {

  def openDefaultGraph() : TitanGraph = {
    val config = new PropertiesConfiguration("titan-cassandra-es.properties")
    TitanFactory.open(config)
  }

}
