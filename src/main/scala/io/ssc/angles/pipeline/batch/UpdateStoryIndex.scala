/**
 * Angles
 * Copyright (C) 2014  Sebastian Schelter
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
package io.ssc.angles.pipeline.batch

import io.ssc.angles.Config
import io.ssc.angles.pipeline._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import twitter4j.TwitterException

import scala.collection.mutable.Queue

object UpdateStoryIndex extends App {
  val since = new DateTime().minusDays(Config.property("angles.updateStoryIndex.sinceDays").toInt)
  new IndexArticles().execute(since)
}
