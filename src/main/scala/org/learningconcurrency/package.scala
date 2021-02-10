package org

package object learningconcurrency {
  def log(msg: String): Unit = println(s"${Thread.currentThread.getName}: $msg")

  def thread(body: => Unit, isDaemon: Boolean = false): Thread = {
    val t = new Thread {
      setDaemon(isDaemon)
      override def run() = body
    }
    t.start()
    t
  }
}
