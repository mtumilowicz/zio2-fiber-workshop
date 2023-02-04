package app

import zio.test.TestAspect.repeats
import zio.test.{Spec, TestClock, TestConsole, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Queue, Scope, durationInt}

object MainSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("a")(
    test("b") {
      assertTrue("a" == "a")
    },
    test("c") {
      for {
        queue <- Queue.bounded[String](100)
        _ <- queue.offer("a")
        _ <- Main.program(queue).fork
        _ <- TestClock.adjust(4.seconds)
        output <- TestConsole.output
      } yield assertTrue (output == Vector("say sth plz\n", "wrote: a\n"))
    },
    test("d") {
      for {
        queue <- Queue.bounded[String](100)
        _ <- queue.offer("a")
        _ <- Main.program(queue).fork
        _ <- TestClock.adjust(6.seconds)
        output <- TestConsole.output
      } yield assertTrue(output == Vector("say sth plz\n", "wrote: a\n", "say sth plz\n"))
    },
    test("d") {
      for {
        queue <- Queue.bounded[String](100)
        _ <- Main.program(queue).fork
        _ <- TestClock.adjust(9.seconds)
        output <- TestConsole.output
      } yield assertTrue(output.count(_ == "say sth plz\n") == 10)
    }
  ) @@ repeats(50)
}
