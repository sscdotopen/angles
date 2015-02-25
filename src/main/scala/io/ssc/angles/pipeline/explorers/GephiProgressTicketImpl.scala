package io.ssc.angles.pipeline.explorers

;

import org.gephi.utils.progress.ProgressTicket;

/**
 * Created by xolor on 23.02.15.
 */
class GephiProgressTicketImpl extends ProgressTicket {

  /**
   * Finish the progress task.
   */
  @Override
  def finish(): Unit = {

  }

  /**
   * Finish the progress task and show and wrap-up message
   *
   * @param finishMessage
     * a message about the finished task
   */
  @Override
  def finish(finishMessage: String): Unit = {

  }

  /**
   * Notify the user about a new completed unit. Equivalent to incrementing workunits by one.
   */
  @Override
  def progress(): Unit = {

  }

  /**
   * Notify the user about completed workunits.
   *
   * @param workunit
     * a cumulative number of workunits completed so far
   */
  @Override
  def progress(workunit: Int): Unit = {

  }

  /**
   * Notify the user about progress by showing message with details.
   *
   * @param message
     * about the status of the task
   */
  @Override
  def progress(message: String): Unit = {

  }

  /**
   * Notify the user about completed workunits and show additional detailed message.
   *
   * @param message
     * details about the status of the task
   * @param workunit
     * a cumulative number of workunits completed so far
   */
  @Override
  def progress(message: String, workunit: Int): Unit = {

  }

  /**
   * Change the display name of the progress task. Use with care, please make sure the changed name is not completely
   * different, or otherwise it might appear to the user as a different task.
   *
   * @param newDisplayName
     * the new display name
   */
  @Override
  def setDisplayName(newDisplayName: String): Unit = {

  }

  /**
   * Returns the current display name
   *
   * @return the current display name
   */
  @Override
  def getDisplayName(): String = {
    return "";
  }

  /**
   * Start the progress indication for indeterminate task.
   */
  @Override
  def start() {

  }

  /**
   * Start the progress indication for a task with known number of steps.
   *
   * @param workunits
     * total number of workunits that will be processed
   */
  @Override
  def start(workunits: Int) {

  }

  /**
   * Currently indeterminate task can be switched to show percentage completed.
   *
   * @param workunits
     * workunits total number of workunits that will be processed
   */
  @Override
  def switchToDeterminate(workunits: Int) {

  }

  /**
   * Currently determinate task can be switched to indeterminate mode.
   */
  @Override
  def switchToIndeterminate(): Unit = {

  }
}
