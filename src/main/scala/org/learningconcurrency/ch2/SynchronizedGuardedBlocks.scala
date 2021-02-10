package org.learningconcurrency
package ch2

object SynchronizedGuardedBlocks extends App {
  val lock = new AnyRef
  var message: Option[String] = None
  val greeter = thread {
    lock.synchronized {
      while (message.isEmpty) lock.wait()
      log(message.get)
    }
  }

  lock.synchronized {
    message = Some("Hello!")
    lock.notify()
  }
  greeter.join()
}
