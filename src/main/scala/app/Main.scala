package app

import zio._

import java.io.IOException

object Main extends ZIOAppDefault {
  def program(queue: Queue[String]) = {
    def loop(sayingBackgroundFiber: Ref[Fiber.Runtime[IOException, Long]]): Task[Unit] = for {
      read <- queue.take
      _ <- sayingBackgroundFiber.get.flatMap(_.interrupt)
      fork <- sayPlease.delay(Duration.fromSeconds(5)).fork
      _ <- sayingBackgroundFiber.set(fork)
      _ <- Console.printLine(s"wrote: $read")
      _ <- loop(sayingBackgroundFiber)
    } yield ()

    for {
      fiber2 <- sayPlease.fork
      _ <- ZIO.sleep(100.millis)
      ref <- Ref.make(fiber2)
      _ <- loop(ref)
    } yield ()
  }

  val sayPlease = Console.printLine("say sth plz").repeat(Schedule.spaced(Duration.fromSeconds(1)))
  def say(queue: Queue[String]) = for {
    read <- Console.readLine
    _ <- queue.offer(read)
  } yield ()


  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = for {
    queue <- Queue.bounded[String](100)
    _ <- say(queue).forever.fork
    _ <- program(queue)
  } yield ()
}