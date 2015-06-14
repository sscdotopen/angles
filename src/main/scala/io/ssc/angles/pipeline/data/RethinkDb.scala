/**
 * Angles
 * Copyright (C) 2015 Jakob Hendeß, Niklas Wolber
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*package io.ssc.angles.pipeline.data

import com.dkhenry.RethinkDB.{RqlConnection, RqlCursor}
import io.ssc.angles.pipeline.explorers.ExplorerUriPair
import org.apache.commons.lang3.StringUtils


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
