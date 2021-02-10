package org.learningconcurrency.ch2

object ThreadsMain extends App {
  val t: Thread = Thread.currentThread
  val name = t.getName
  println(s"I am the thread $name")

  class MyThread extends Thread {
    override def run(): Unit = println("New thread running.")
  }

  val newThread = new MyThread
  newThread.start()
  newThread.join()
  println("New thread joined")
}
