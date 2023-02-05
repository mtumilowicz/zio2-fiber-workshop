package app

import zio.{Console, Queue, _}

import java.io.IOException

case class Program(queue: Queue[String]) {

  private val ticker = Console.printLine("say sth plz")
    .repeat(Schedule.spaced(Duration.fromSeconds(1)))

  def run: ZIO[Any, Throwable, Unit] = {
    def loop(sayingBackgroundFiber: Ref[Fiber.Runtime[IOException, Long]]): Task[Unit] = for {
      read <- queue.take
      _ <- sayingBackgroundFiber.get.flatMap(_.interrupt)
      fork <- ticker.delay(Duration.fromSeconds(5)).fork
      _ <- sayingBackgroundFiber.set(fork)
      _ <- Console.printLine(s"wrote: $read")
      _ <- loop(sayingBackgroundFiber)
    } yield ()

    for {
      fiber2 <- ticker.fork
      _ <- ZIO.sleep(100.millis)
      ref <- Ref.make(fiber2)
      _ <- loop(ref)
    } yield ()
  }

}
