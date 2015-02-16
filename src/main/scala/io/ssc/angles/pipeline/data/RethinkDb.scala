package io.ssc.angles.pipeline.data

import com.rethinkscala.Document
import com.rethinkscala.Implicits._
import com.rethinkscala.Implicits.Async
import com.rethinkscala.japi.r
import com.rethinkscala.net.Version2
import com.rethinkscala.Async._
import org.codehaus.jackson.annotate.JsonProperty

case class Pair(explorer: Long, realUri: String)

/**
 * Created by niklas on 15.02.15.
 */
class RethinkDb {
  implicit val asyncConnection = Async(Version2("localhost"))

  def allTweetURIPairs() = {
    r.db("angles").table("tweetedUris").as[Pair]
  }
}
