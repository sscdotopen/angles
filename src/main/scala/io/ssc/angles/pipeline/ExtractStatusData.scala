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

package io.ssc.angles.pipeline


import org.joda.time.DateTime
import twitter4j.Status
object ExtractStatusData {


  def retweetsPerHoursInDayOne(now: DateTime, status: Status) = {

    val diffInHours = math.ceil((now.getMillis - new DateTime(status.getCreatedAt).getMillis) / (3600 * 1000))

    val hoursToLookAt = math.min(diffInHours, 24)

    if (status.getRetweetCount > 0) {
      status.getRetweetCount / hoursToLookAt.toDouble
    } else {
      0d
    }
  }

}