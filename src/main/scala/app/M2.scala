package app

import zio._
import zio.Random._

object M2 extends ZIOAppDefault {

  override def run = program

  sealed trait Diagnostic

  case object HipDiagnostic extends Diagnostic

  case object KneeDiagnostic extends Diagnostic

  case class Request[A](topic: Diagnostic, XRayImage: A)

  trait RequestGenerator[A] {
    def generate(topic: Diagnostic): UIO[Request[A]]
  }

  case class IntRequestGenerator() extends RequestGenerator[Int] {
    override def generate(topic: Diagnostic): UIO[Request[Int]] =
      nextIntBounded(1000) flatMap (n => ZIO.succeed(Request(topic, n)))
  }

  case class Consumer[A](title: String) {
    def run = for {
      queue <- Queue.bounded[A](10)
      loop = for {
        img  <- queue.take
        _    <- Console.printLine(s"[$title] worker: Starting analyzing task $img")
        rand <- nextIntBounded(4)
        _    <- ZIO.sleep(rand.seconds)
        _    <- Console.printLine(s"[$title] worker: Finished task $img")
      } yield ()
      fiber <- loop.forever.fork
    } yield (queue, fiber)
  }

  object Consumer {
    def create[A](title: String) = ZIO.succeed(Consumer[A](title))
  }

  case class TopicQueue[A](queue: Queue[A], subscribers: Ref[Map[Int, List[Queue[A]]]]) {
    def subscribe(sub: Queue[A], consumerGroup: Int): UIO[Unit] =
      subscribers.update { map =>
        map.get(consumerGroup) match {
          case Some(value) =>
            map + (consumerGroup -> (value :+ sub))
          case None =>
            map + (consumerGroup -> List(sub))
        }
      }

    private val loop =
      for {
        elem <- queue.take
        subs <- subscribers.get
        _    <- ZIO.foreachDiscard(subs.values) {
          group =>
            for {
              idx <- nextIntBounded(group.length)
              _ <- group(idx).offer(elem)
            } yield ()
        }
      } yield ()

    def run = loop.forever.fork
  }

  object TopicQueue {
    def create[A](queue: Queue[A]): UIO[TopicQueue[A]] =
      Ref.make(Map.empty[Int, List[Queue[A]]]) flatMap (map => ZIO.succeed(TopicQueue(queue, map)))
  }

  case class Exchange[A]() {
    def run = for {
      jobQueue       <- Queue.bounded[Request[A]](10)
      queueHip       <- Queue.bounded[A](10)
      queueKnee      <- Queue.bounded[A](10)
      hipTopicQueue  <- TopicQueue.create(queueHip)
      kneeTopicQueue <- TopicQueue.create(queueKnee)
      loop = for {
        job <- jobQueue.take
        _   <- job.topic match {
          case HipDiagnostic =>
            queueHip.offer(job.XRayImage)
          case KneeDiagnostic =>
            queueKnee.offer(job.XRayImage)
        }
      } yield ()
      fiber <- loop.forever.fork
    } yield (jobQueue, hipTopicQueue, kneeTopicQueue, fiber)
  }

  object Exchange {
    def create[A] = ZIO.succeed(Exchange[A]())
  }

  case class Producer[A](queue: Queue[Request[A]], generator: RequestGenerator[A]) {
    def run = {
      val loop = for {
        _    <- Console.printLine("[XRayRoom] generating hip and knee request")
        hip  <- generator.generate(HipDiagnostic)
        _    <- queue.offer(hip)
        knee <- generator.generate(KneeDiagnostic)
        _    <- queue.offer(knee)
        _    <- ZIO.sleep(2.seconds)
      } yield ()
      loop.forever.fork
    }
  }

  object Producer {
    def create[A](queue: Queue[Request[A]], generator: RequestGenerator[A]) = ZIO.succeed(Producer(queue, generator))
  }

  val program = for {

    physicianHip             <- Consumer.create[Int]("Hip")
    ctxPhHip                 <- physicianHip.run
    (phHipQueue, phHipFiber) = ctxPhHip

    loggerHip           <- Consumer.create[Int]("HIP_LOGGER")
    ctxLoggerHip        <- loggerHip.run
    (loggerHipQueue, _) = ctxLoggerHip

    physicianKnee    <- Consumer.create[Int]("Knee1")
    ctxPhKnee        <- physicianKnee.run
    (phKneeQueue, _) = ctxPhKnee

    physicianKnee2    <- Consumer.create[Int]("Knee2")
    ctxPhKnee2        <- physicianKnee2.run
    (phKneeQueue2, _) = ctxPhKnee2


    exchange                                         <- Exchange.create[Int]
    ctxExchange                                      <- exchange.run
    (inputQueue, outputQueueHip, outputQueueKnee, _) = ctxExchange


    generator = IntRequestGenerator()
    xRayRoom  <- Producer.create(inputQueue, generator)
    _         <- xRayRoom.run


    _ <- outputQueueHip.subscribe(phHipQueue, consumerGroup = 1)
    _ <- outputQueueHip.subscribe(loggerHipQueue, consumerGroup = 2)

    _ <- outputQueueKnee.subscribe(phKneeQueue, consumerGroup = 1)
    _ <- outputQueueKnee.subscribe(phKneeQueue2, consumerGroup = 1)

    _ <- outputQueueHip.run
    _ <- outputQueueKnee.run

    _ <- phHipFiber.join

  } yield ()

}