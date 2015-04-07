/*
package io.ssc.angles.pipeline.data

import com.dkhenry.RethinkDB.{RqlConnection, RqlCursor}
import io.ssc.angles.pipeline.explorers.ExplorerUriPair
import org.apache.commons.lang3.StringUtils


/**
 * Created by xolor on 16.02.15.
 */
object RethinkDb extends App {              
  
  getPairList

  def getPairList : List[ExplorerUriPair] = {
    val r = RqlConnection.connect("localhost", 28015)

    val dbList = r.run(r.db("angles").table("tweetedUris").eq_join("explorer", r.db("angles").table("explorers"))).asInstanceOf[RqlCursor]

    var pairList: collection.mutable.MutableList[ExplorerUriPair] = collection.mutable.MutableList.empty[ExplorerUriPair]
    val it = dbList.iterator()

    while (it.hasNext) {
      try {
        val map: java.util.Map[String, java.util.Map[String, AnyRef]] = it.next().get().asInstanceOf[java.util.Map[String, java.util.Map[String, AnyRef]]]
        val leftMap : java.util.Map[String, AnyRef] = map.get("left")
        val rightMap : java.util.Map[String, AnyRef] = map.get("right")
        var uri = leftMap.getOrDefault("realUri", leftMap.get("uri")).asInstanceOf[String]
        //val explorer = new java.math.BigDecimal(rightMap.get("screen_name").asInstanceOf[Double]).toPlainString
        val explorer = rightMap.get("screen_name").asInstanceOf[String]
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
*/
