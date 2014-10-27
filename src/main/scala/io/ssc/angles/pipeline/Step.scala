package io.ssc.angles.pipeline

import org.joda.time.DateTime

trait Step {

  def execute(since: DateTime): Unit
}
