package app

import zio._

object Main extends ZIOAppDefault {
  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = for {
    queue <- Queue.bounded[String](100)
    _ <- offerReadLine(queue).forever.fork
    _ <- Program(queue).run
  } yield ()

  private def offerReadLine(queue: Queue[String]) = for {
    read <- Console.readLine
    _ <- queue.offer(read)
  } yield ()

}