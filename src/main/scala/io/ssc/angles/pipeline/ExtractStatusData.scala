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