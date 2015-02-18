package io.ssc.angles.pipeline.data

import com.dkhenry.RethinkDB.{RqlConnection, RqlCursor}
import io.ssc.angles.pipeline.explorers.ExplorerUriPair
import org.apache.commons.lang3.StringUtils


/**
 * Created by xolor on 16.02.15.
 */
object RethinkDb extends App {                     

  def getPairList : List[ExplorerUriPair] = {
    val r = RqlConnection.connect("localhost", 28015)

    val dbList = r.run(r.db("angles").table("tweetedUris")).asInstanceOf[RqlCursor]


    var pairList: collection.mutable.MutableList[ExplorerUriPair] = collection.mutable.MutableList.empty[ExplorerUriPair]
    val it = dbList.iterator()

    while (it.hasNext) {
      try {
        val map: java.util.Map[String, AnyRef] = it.next().get().asInstanceOf[java.util.Map[String, AnyRef]]
        var uri = map.getOrDefault("realUri", map.get("uri")).asInstanceOf[String]
        val explorer = new java.math.BigDecimal(map.get("explorer").asInstanceOf[Double]).toPlainString
        uri = StringUtils.replace(uri, " ", "%20")
        uri = StringUtils.replace(uri, "|", "%7C")
        pairList += new ExplorerUriPair(explorer, uri)
      } catch {
        case e: IllegalArgumentException => println(e.getMessage)
      }
    }
   pairList.toList
  }

}
