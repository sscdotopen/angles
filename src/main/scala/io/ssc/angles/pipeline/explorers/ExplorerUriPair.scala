package io.ssc.angles.pipeline.explorers

import java.net.URI

/**
 * Created by xolor on 11.02.15.
 */
class ExplorerUriPair (val explorerId : String, val uri : URI) {

  def mapURI(f : (URI) => String) : String = f(uri)
  
  def this(explorerId : String, uri : String) = {
    this(explorerId, URI.create(uri))
  }
  
}
