package org.learningconcurrency
package ch2

import scala.collection._

object SynchronizedPool extends App {
  private val tasks = mutable.Queue[() => Unit]()

  object Worker extends Thread {
    setDaemon(false)

    def poll(): () => Unit = tasks.synchronized {
      while (tasks.isEmpty) tasks.wait()
      tasks.dequeue()
    }

    override def run(): Unit = while (true) {
      val task = poll()
      task()
    }
  }

  Worker.start()
  def asynchronous(body: => Unit): Unit = tasks.synchronized {
    tasks.enqueue(() => body)
    tasks.notify()
  }

  asynchronous { log("Hello") }
  asynchronous { log(" world!") }
  Thread.sleep(5000)
}
