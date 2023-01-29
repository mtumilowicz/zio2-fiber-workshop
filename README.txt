* references
    * [How To Successfully Manage A ZIO Fiber's Lifecycle by Natan Silnitsky](https://www.youtube.com/watch?v=zUPtEbPsOqE)
    * https://blog.rockthejvm.com/zio-fibers/
    * https://www.zionomicon.com/
    * https://scalac.io/blog/build-your-own-kafka-in-zio-queues-fibers/
    * https://blog.knoldus.com/everything-you-need-to-know-about-zio-fiber-and-its-operations-explained/
    * [2022 - Tomasz Nurkiewicz - Loom: rewolucja czy szczegół implementacyjny?](https://www.youtube.com/watch?v=FVEpsgquheo)
    * https://gist.github.com/mtsokol/0d6ab5473c04583899e3ffdcb7812959


* linux thread ~ linux process
  * from the kernel point of view, only processes exist
  * so-called thread is just a different kind of process
  * difference: flag (1 bit) - to share memory with parent
    * yes => thread; no => process
* A fiber is a concept that is beyond the ZIO library. In fact, it’s a concurrency model
* Often, we refer to fibers as green threads.
* A fiber is a schedulable computation, much like a thread. However, it’s only a data structure, which means it’s up to the ZIO runtime to schedule these fibers for execution (on the internal JVM thread pool)
* Unlike a system/JVM thread which is expensive to start and stop, fibers are cheap to allocate and remove.
* Hence, we can create millions of fibers and switch between them without the overheads associated with threads.
* The ZIO library represents fibers using the type Fiber[E, A], which means a computation that will produce a result of type A or will fail with the type E
    * Moreover, ZIO executes fibers using an Executor, which is a sort of abstraction over a thread pool.
* to create a new fiber in ZIO, we must fork it from an instance of the ZIO effect
   ```
   trait ZIO[-R, +E, +A] {
     def fork: URIO[R, Fiber[E, A]]
   }
   ```
* ZIO fibers provide the join method to wait for the termination of a fiber:
    ```
    trait Fiber[+E, +A] {
      def join: IO[E, A]
    }
    ```
    * Through the join method, we can wait for the result of concurrent computation and eventually use it
* Interrupting a FiberPermalink
    * Why should we interrupt a fiber? The main reason is that some action external to the fiber execution turns the fiber useless.
    * So, to not waste system resources, it’s better to interrupt the fiber.
    ```
    trait Fiber[+E, +A] {
      def interrupt: UIO[Exit[E, A]]
    }
    ```
    * If the fiber already succeeded with its value when interrupted, then ZIO returns an instance of Exit.Success[A], an Exit.Failure[Cause.Interrupt] otherwise
    * Unlike interrupting a thread, interrupting a fiber is an easy operation. In fact, the creation of a new Fiber is very lightweight. It doesn’t require the creation of complex structures in memory, as for threads. Interrupting a fiber simply tells the Executor that the fiber must not be scheduled anymore.
* Finally, unlike threads, we can attach finalizers to a fiber.
    * A finalizer will close all the resources used by the effect.
    * The ZIO library guarantees that if an effect begins execution, its finalizers will always be run, whether the effect succeeds with a value, fails with an error, or is interrupted.
* Last but not least, we can declare a fiber as uninterruptible. As the name suggests, an uninterruptible fiber will execute till the end even if it receives an interrupt signal.
* So Fibers are data types for expressing concurrent computations. Fibers are loosely related to threads – a single Fiber can be executed on multiple threads by shifting between them – all with full resource safety!
* Cats Effect and ZIO both rely on fibers
* in ZIO world, Fiber is the closest analogy to Future
  * if we see fiber it is probably doing something or already evaluated
  * two core methods are: join and interrupt
    * no start method, as soon as fiber is created it is started as well
* in ZIO: def fork: ZIO[R, Nothing, Fiber[E, A]]
* in Fiber: def join: ZIO[Any, E, A]
  * fork means run in the background; join means wait for a result
* semantically block but never block underlying threads
* ZIO.foreachPar(ids)(getUserById)
  * automatically interrupt others if one fails
* getDataFromEastCoast.race(getDataFromWestCoast)
  * returns first
  * automatically interrupt the loser
* provided primitives
  * Ref - functional equivalent of atomic ref
  * Promise - single value communication
  * Queue - multiple value communication
  * Semaphore - control level of concurrency
  * Schedule - manage repeats and retries

* fibers
  * if it is not doing active work and can't do active work - will be garbage collected
  * you don't have to take care of explicitly shutting them down
  * it’s up to the ZIO runtime to schedule these fibers for execution (on the internal JVM thread pool)
  * Moreover, ZIO executes fibers using an Executor, which is a sort of abstraction over a thread pool
  * ZIO fibers don’t block any thread during the waiting associated with the call of the join method
  * If the fiber already succeeded with its value when interrupted, then ZIO returns an instance of Exit.Success[A], an Exit.Failure[Cause.Interrupt] otherwise
    * Unlike interrupting a thread, interrupting a fiber is an easy operation
    * Interrupting a fiber simply tells the Executor that the fiber must not be scheduled anymore
    * As the name suggests, an uninterruptible fiber will execute till the end even if it receives an interrupt signal.
  * Notice that we’re measuring threads versus CPU cores and fibers versus GB of heap
  * But since creating the fiber itself — and running the IO on a separate thread — is an effect, the returned fiber is wrapped in another IO instance
  * usually you don't work with fork & join but with higher level operators:
    * ZIO#foreachPar
    * ZIO#zipPar
    * ZIO#race

* queues
    * The effectful, back-pressured ZIO Queue makes it easy to avoid blocked threads on Queues core operations such as offer and take.