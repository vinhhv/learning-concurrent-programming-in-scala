package org.learningconcurrency
package ch2

object ThreadsSleep extends App {
  val t = thread {
    Thread.sleep(1000)
    log("New thread running")
    Thread.sleep(1000)
    log("Still running.")
    Thread.sleep(1000)
    log("Completed.")
  }
  t.join()
  log("New thread joined.")
}
