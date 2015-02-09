package io.ssc.angles.pipeline.explorers

import java.net.URI

/**
 * Created by xolor on 11.02.15.
 */
class ClusterableTweet (val explorerId : String, val uris : List[URI]) {

  def mapURIs(f : (URI) => String) : List[String] = uris.map(f)
  
}
