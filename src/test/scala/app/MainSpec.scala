package app

import zio.test.TestAspect.repeats
import zio.test.{Spec, TestClock, TestConsole, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Queue, Scope, durationInt}

object MainSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("Program")(
    test("non empty queue and less than 5s -> no ticker message") {
      for {
        queue <- Queue.bounded[String](100)
        _ <- queue.offer("a")
        _ <- Program(queue).run.fork
        _ <- TestClock.adjust(4.seconds)
        output <- TestConsole.output
      } yield assertTrue (output == Vector("say sth plz\n", "wrote: a\n"))
    },
    test("non empty queue and more than 5s -> one ticker message") {
      for {
        queue <- Queue.bounded[String](100)
        _ <- queue.offer("a")
        _ <- Program(queue).run.fork
        _ <- TestClock.adjust(6.seconds)
        output <- TestConsole.output
      } yield assertTrue(output == Vector("say sth plz\n", "wrote: a\n", "say sth plz\n"))
    },
    test("empty queue => ticker message multiplied by number of seconds + 1") {
      for {
        queue <- Queue.bounded[String](100)
        _ <- Program(queue).run.fork
        _ <- TestClock.adjust(9.seconds)
        output <- TestConsole.output
      } yield assertTrue(output.count(_ == "say sth plz\n") == 10)
    }
  ) @@ repeats(50)
}
