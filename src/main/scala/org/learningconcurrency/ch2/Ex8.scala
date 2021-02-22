package org.learningconcurrency
package ch2

import scala.collection.mutable

object Ex8 extends App {

  final case class Task(runnable: () => Unit, priority: Int)
  implicit object TaskOrdering extends Ordering[Task] {
    override def compare(x: Task, y: Task): Int = x.priority compare y.priority
  }

  class PriorityTaskPool(nThreads: Int, important: Int) {
    private val tasks = mutable.PriorityQueue[Task]()

    @volatile private var terminated = false

    def asynchronous(priority: Int)(task: => Unit): Unit = tasks synchronized {
      tasks.enqueue(Task(() => task, priority))
      tasks.notify()
    }

    def shutdown(): Unit = tasks.synchronized {
      terminated = true
      tasks.notify()
      log("Shutdown!")
    }

    class Worker extends Thread {
      setDaemon(true)

      def poll(): Task = tasks.synchronized {
        while (tasks.isEmpty) tasks.wait()
        tasks.dequeue()
      }

      override def run(): Unit = while (true) {
        poll() match {
          case Task(runnable, priority) if priority > important || !terminated => runnable()
          case _ =>
        }
      }
    }

    (0 until nThreads) foreach(_ => new Worker().start())
  }

  val tasks = new PriorityTaskPool(10, 900)

  (1 to 1000) foreach(_ => {
    val a = (Math.random * 1000).toInt
    tasks.asynchronous(a)({log(s"<- $a")})
  })
  tasks.shutdown()


  Thread.sleep(10000)
  log("Hello")
  Thread.sleep(10000)
}
