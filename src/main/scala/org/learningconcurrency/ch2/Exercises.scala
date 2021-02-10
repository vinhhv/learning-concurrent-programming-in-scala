package org.learningconcurrency
package ch2

import scala.annotation.tailrec

object Exercises extends App {
  def parallel[A, B](a: => A, b: => B): (A, B) = {
    @volatile var resultA: Option[A] = None
    @volatile var resultB: Option[B] = None

    thread {
      resultA = Some(a)
    }

    thread {
      resultB = Some(b)
    }

    while (resultA.isEmpty || resultB.isEmpty) ()

    val result = (resultA.get, resultB.get)
    log(s"result $result")
    result
  }

  parallel({ Thread.sleep(3); 1 }, { Thread.sleep(5); "Hello" })

  @tailrec
  def periodically(duration: Long)(b: => Unit): Unit = {
    b
    Thread.sleep(duration)
    periodically(duration)(b)
  }

  thread({
    periodically(2000)(println("Hello!"))
  }, true)

  class SyncVar[T] {
    @volatile var value: Option[T] = None

    def get(): T = this.synchronized {
      value match {
        case None => throw new Exception("Value is empty!")
        case Some(t) =>
          value = None
          t
      }
    }
    def put(x: T): Unit = this.synchronized {
      value match {
        case None => value = Some(x)
        case Some(t) => throw new Exception("Value is non empty!")
      }
    }

    def getWait(): T = this.synchronized {
      while (this.isEmpty) this.wait()
      val result = value.get
      value = None
      this.notify()
      result
    }

    def putWait(x: T): Unit = this.synchronized {
      while (this.nonEmpty) this.wait()
      value = Some(x)
      this.notify()
    }

    def isEmpty: Boolean = synchronized { value.isEmpty }

    def nonEmpty: Boolean = !isEmpty
  }

  val syncVar = new SyncVar[Int]

  thread {
    @tailrec
    def printValue: Unit = {
      syncVar.synchronized {
        while (syncVar.isEmpty) syncVar.wait()
        val value = syncVar.get()
        syncVar.notify()
        log(s"value is $value")
      }
      printValue
    }
    printValue
  }

  thread {
    for (i <- 0 until 15) {
      syncVar.synchronized {
        while (syncVar.nonEmpty) syncVar.wait()
        syncVar.put(i)
        syncVar.notify()
      }
    }
  }

  val syncVarWait = new SyncVar[Int]
  thread {
    @tailrec
    def printValue: Unit = {
      val value = syncVarWait.getWait()
      log(s"wait value is $value")
      printValue
    }
    printValue
  }

  thread {
    for (i <- 0 until 15) {
      syncVarWait.putWait(i)
    }
  }
}
