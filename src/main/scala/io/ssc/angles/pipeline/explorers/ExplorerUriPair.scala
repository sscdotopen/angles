package io.ssc.angles.pipeline.explorers

import java.net.URI

/**
 * Created by xolor on 11.02.15.
 */
class ExplorerUriPair (val explorerId : String, val uris : List[URI]) {

  def mapURIs(f : (URI) => String) : List[String] = uris.map(f)
  
  def this(explorerId : String, uri : String) = {
    this(explorerId, List(URI.create(uri)))
  }
  
}
