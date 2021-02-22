package org.learningconcurrency
package ch2

import org.learningconcurrency.ch2.SynchronizedNesting.Account

import scala.annotation.tailrec
import scala.collection.mutable

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

  class SyncQueue[T](n: Int) {
    private var queue: mutable.Queue[T] = new mutable.Queue[T]()

    def get(): T = this.synchronized {
      while (queue.isEmpty) this.wait()
      val result = queue.dequeue()
      this.notify()
      result
    }

    def put(t: T): Unit = this.synchronized {
      while (queue.size == n) this.wait()
      queue.enqueue(t)
      this.notify()
    }
  }

  val syncQueue = new SyncQueue[Int](10)

  thread {
    @tailrec
    def printValue: Unit = {
      val value = syncQueue.get()
      log(s"queue value = $value")
      printValue
    }
    printValue
  }

  thread {
    for (i <- 0 until 10) {
      syncQueue.put(i)
    }
  }

  // Exercise 7
  def send(a1: Account, a2: Account, n: Int): Unit = {
    def adjust: Unit = {
      a1.money -= n
      a2.money += n
    }
    if (a1.name < a2.name) a1.synchronized { a2.synchronized { adjust }}
    else a2.synchronized { a1.synchronized { adjust }}
  }
  def sendAll(accounts: Set[Account], target: Account): Unit = {
//    accounts.foreach(account => send(account, target, account.money))
    def adjust: Unit = {
      target.money = accounts.foldLeft(0)((s, a) => {
        val money = a.money
        a.money = 0
        s + money
      })
    }

    def sendAllSync(accounts: List[Account]): Unit = accounts match {
      case h :: t => h synchronized {
        sendAllSync(t)
      }
      case _ => adjust
    }

    sendAllSync((target :: accounts.toList).sortBy(_.name))
  }

  val accounts = (1 to 100).map(i => new Account(s"Account: $i", i * 10)).toSet
  val target = new Account("Target account", 0)

  sendAll(accounts, target)

  accounts.foreach(a => log(s"${a.name}, money = ${a.money}"))
  log(s"${target.name} - money = ${target.money}")
}
