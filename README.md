[![Build Status](https://app.travis-ci.com/mtumilowicz/elliptic-curve-workshop.svg?branch=master)](https://app.travis-ci.com/mtumilowicz/elliptic-curve-workshop)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

# zio2-fiber-workshop

* references
    * [How To Successfully Manage A ZIO Fiber's Lifecycle by Natan Silnitsky](https://www.youtube.com/watch?v=zUPtEbPsOqE)
    * https://blog.rockthejvm.com/zio-fibers/
    * https://www.zionomicon.com/
    * https://blog.knoldus.com/everything-you-need-to-know-about-zio-fiber-and-its-operations-explained/
    * [2022 - Tomasz Nurkiewicz - Loom: rewolucja czy szczegół implementacyjny?](https://www.youtube.com/watch?v=FVEpsgquheo)
    * https://gist.github.com/mtsokol/0d6ab5473c04583899e3ffdcb7812959
    * https://zio.dev/reference/
    * https://www.quora.com/Whats-the-difference-between-green-threads-coroutines-lightweight-threads-and-fibers
    * https://graphitemaster.github.io/fibers/
    * https://www.developer.com/design/an-introduction-to-jvm-threading-implementation/
    * https://www.developer.com/design/understanding-the-java-thread-model/
    * https://medium.com/@ja.m.arunkumar/java-threads-part-1-7855b11ddb6
    * https://nisal-pubudu.medium.com/understanding-threads-multi-threading-in-java-6e8c988d26af
    * https://medium.com/platform-engineer/understanding-jvm-architecture-22c0ddf09722
    * https://stackoverflow.com/questions/4967885/jvm-option-xss-what-does-it-do-exactly
    * https://www.quora.com/Why-a-context-switch-is-considered-an-overhead-task
    * [The Great Concurrency Smackdown: ZIO versus JDK by John A. De Goes](https://www.youtube.com/watch?v=9I2xoQVzrhs)
    * [The Rise Of Loom And The Evolution Of Reactive Programming](https://www.youtube.com/watch?v=SJeAb-XEIe8)
    * [Threading lightly with Kotlin by Vasco Veloso](https://www.youtube.com/watch?v=UdCxii0xihw)
    * [Zymposium - FiberRefs](https://www.youtube.com/watch?v=Lq_EI7l9rnA)
    * [Zymposium - Implementing a New ZIO Feature](https://www.youtube.com/watch?v=RA24E-674MY)
    * [[VDBUH22] Riccardo Lippolis - Concurrency made easy with Kotlin Coroutines](https://www.youtube.com/watch?v=k7JUObP6PeI)
    * [Game of Loom: implementation patterns [...] playing with virtual threads by Mario Fusco](https://www.youtube.com/watch?v=70aNTos4Lrc)

## preface
* goals of this workshop
    * introduction to ZIO fiber model
    * understanding standard operations on fibers: interrupt, join
    * understanding forking a fiber
* workshop plan
    * task: implement app that has background process that prints to console some message, but if you type something
    in console - background process should not print anything for 5 sec

## general
* linux thread ~ linux process
    * from the kernel point of view, only processes exist
    * so-called thread is just a different kind of process
    * difference: flag (1 bit) - to share memory with parent
        * yes => thread; no => process
* green threads, coroutines, lightweight threads, and fibers are all different names for the same basic idea
    * multiple threads of execution in a single address space that cooperate with no or minimal kernel support
* type of workloads
    * CPU Work
        * pure computational firepower without involving any interaction and communication with the outside world
    * Blocking I/O
        * anything that involves reading from and writing to an external resource
            * file or a socket or web API
    * Asynchronous I/O
        * is code that whenever it runs into something that it needs to wait on, instead of blocking
        and parking the thread, it registers a callback, and returns immediately
            * when the result is available then our callback will be invoked
        * example: callbacks are the fundamental way by which all async code on the JVM works
            * no mechanism to support async code natively
            * drawback: not pretty and fun to work with
* scheduling
    * cooperative scheduling
        * fibers yield to each other as required to preserve some level of fairness
    * preemptive scheduling
        * cooperative multitasking (also known as non-preemptive multitasking) is a style of computer multitasking in which the operating system never initiates a context switch from a running process to another process
           * instead, in order to run multuple applications concurrently, processes coluntarily yield control periodically or when idle or logically blocked
        * you don’t have to yield to other threads to allow them to run
        * most operating systems (OS) schedule threads preemptively
        * when OS may decide to preempt a thread include?
            * IO, sleep, interrupt

## jvm thread model
* every program have at least one thread (main thread)
    * is created by the JVM whenever you run a java program
    * thread is simply a flow of execution
* there are two types of threads
    * daemon threads
    * non-daemon threads
        * JVM always waits for non-daemon threads to finish their work
        * never exits until the last non-daemon thread finishes its work
* order of threads’ executions cannot be predicted
* before Java 1.3 there was a thing called Green thread model
    * simplest threading library of JVM scheduled threads
    * each thread is an abstraction within the VM
    * JVM is completely responsible for its creation and manages the process of context switching
    * underlying operating system is unaware of the existence of any thread within the process
        * OS sees JVM as a single process and a single thread
* current Java Releases use something called Native thread model
    * JVM creates and manages Java threads
        * uses thread API library of the underlying operating system
* memory model refresher
    * heap
        * for storing objects created by the Java application
        * shared resource (only 1 heap area per JVM)
            * data is not thread safe
    * stack
        * worth reviewing: https://github.com/mtumilowicz/java-stack
        * not shared
        * every JVM thread has its own
        * each running thread creates its own stack in the stack area
        * contains all the information specific or local to the thread
            * example: declared primitive variables and method calls
        * `-Xss` flag is used to "set thread stack size"
            * default size: 1024 KB for 64-bit JVMs
* limitations of threads
    * scarce
        * mapping from JVM threads to operating system threads is one-to-one
            * imposes an upper bound on the number of threads
            * mapping of fibers to threads is many-to-one
    * resource consuming
        * stack space
        * jvm meta
        * os descriptors
        * new gc root
    * overhead on context switching
        * in practice, the way the OS switches between threads and processes used to be
            1. interrupt the running thread
            1. store the context of the interrupted thread
            1. run the OS scheduler to decide what to do next
            1. flush the cache, if necessary
            1. load the context of the next thread/process
            1. begin executing that thread/process
    * lack of composability
        * are not typed
        * don't have a meaningful return type
        * no type parameter for error
            * expected to throw any exception of type Throwable to signal errors
    * no guaranteed way to stop a thread
        * thread can be interrupted via `Thread#interrupt` from another thread, but it may refuse the interruption request and continue processing
    * not typed
        * powered by `Runnable` through constructor `Thread(Runnable r)`, and `Runnable` is not typed

## zio fibers
* other names
    * green threads
    * user-space threads
    * coroutines
* virtual threads
    * scalability way beyond threads
        * each JVM thread will end up executing anywhere from hundreds to thousands or even tens
        of thousands of fibers concurrently
            * by hopping back and forth between them as necessary   
    * may have relevant impact on the GC
        * uses heap to store stacks
        * but: if you run 1000 virtual threads by 8 native threads you have only 8 additional gc roots (not 1000)
    * massive context switches (in user space) of a huge number of virtual threads can potentially cause a larger amount of cache misses
    * is a concept that is beyond the ZIO library
    * it’s a concurrency model
    * we’re measuring threads versus CPU cores and fibers versus GB of heap
    * comparison
      |                 |native            |virtual                             |
      |---              |---               |---                                 |
      |metadata         |2kB               |200-300B                            |
      |stack            |preallocated 1MB  |allocated on heap (pay-as-you-go)   |
      |context switch   |1-10 µs           | some ns                            |
* in the ZIO model, all code runs on fibers
    * analogy: there is no code in Java that does not execute on a thread
    * example: main function in ZIO that returns an effect
        * we don't explicitly fork a fiber, the effect will be executed on what is called the main fiber
            * it's a top-level fiber
            * analogy: main function in Java will execute on the main thread
* fiber models a running computation and instructions on a single fiber are executed sequentially
    * fibers give no guarantees as to which thread they execute on at any time
* single JVM thread will execute many fibers
* Cats Effect and ZIO both rely on fibers
* it’s only a data structure
    * it’s up to the ZIO runtime to schedule these fibers for execution (on the internal JVM thread pool)
    * we can create millions of fibers and switch between them without the overheads associated with threads
* fibers are cheap to start and stop
* represented using the type `Fiber[E, A]`
    * computation that will produce a result of type A or will fail with the type E
* closest analogy to Future
    * if we see fiber it is probably doing something or already evaluated
        * returned fiber is wrapped in another IO instance
    * two core methods are: join and interrupt
        * no start method, as soon as fiber is created it is started as well
* ZIO executes fibers using an Executor, which is a sort of abstraction over a thread pool
    * one primary built-in fixed thread pool
        * designed to be used for the majority of our application requirements
        * has a certain number of threads in it and that stays constant over the lifetime of our application
            * it does not actually help things to create more threads than the number of CPU cores
                * example: if we have eight cores, it does not accelerate any sort of processing to create more than eight threads
                    * hardware is only capable of running eight things at the same time.
            * ZIO's default thread pool is fixed with a number of threads equal to the number of CPU cores
        * problem: pure CPU Work operation that takes a really long time to run
            * if that overall CPU Work composes many ZIO operations, it has a chance to yield quickly to other
            fibers and doesn't monopolize a thread.
                * if not => ZIO Runtime doesn't have any chance of yielding quickly to other fibers
                    * so this fiber is going to monopolize the underlying thread
                    * example: we lift some function from a legacy library using `ZIO#attempt`
            * solution: when we have a huge CPU Work that is not chunked with built-in ZIO operations we should run that on a dedicated thread pool
                * designed to perform CPU-driven tasks
                * ZIO has a special thread pool that can be used to do these computations
                    * blocking thread pool
                    * operator: `ZIO#blocking` and its variants
* non blocking operations
    * most of the ZIO operations do not block the underlying thread
        * they offer blocking semantics managed by ZIO
            * just blocking semantically without actually blocking an underlying thread
        * example: ZIO.sleep, queue.take, queue.offer, semaphore.withPermit
    * vs Java counterparts block a thread
        * example: Thread.sleep or any of its lock machinery
    * ZIO is 100% non-blocking, while Java threads are not
* we can attach finalizers to a fiber
    * unlike threads
    * finalizer will close all the resources used by the effect
    * ZIO library guarantees that if an effect begins execution, its finalizers will always be run
        * irrespective of whether the effect succeeds with a value, fails with an error, or is interrupted
* fairness
    * example: we have five fibers
        * first four: performing long running computations with one million instructions each
        * fifth: performing a short computation with only one thousand instructions
        * executor: fixed thread pool with four threads
            * assuming no yielding
                * we would not get to start running the fifth fiber until all the other fibers had completed
                * work of the fifth fiber was not really performed concurrently
            * assuming yielding
                * if each fiber yields back to the runtime after every thousand instructions
                    * first four fibers do a small amount of work
                    * then the fifth fiber gets to do its work
                    * then the other four fibers get to continue their work
                * creates the result of all five fibers running concurrently
                    * even though only four are ever actually running at any given moment.
* how fibers differ to virtual threads from loom project?
   * libraries like ZIO allocate everything on heap (ZIO does not have a way to save stack and restore)
   * virtual threads in loom allow you to save and restore a stack
      * stack is significantly faster to use than heap

### fork
* forking creates a new fiber that executes the effect being forked concurrently with the current fiber
    ```
    trait ZIO[-R, +E, +A] {
      def fork: URIO[R, Fiber[E, A]]
    }
    ```
    * example
        ```
         lazy val example2 = for {
             _ <- doSomething.fork
             _ <- doSomethingElse
         } yield ()
         ```
         * disclaimer: there is no guarantee about the order of execution of `doSomething` and `doSomethingElse`
            * to order the execution use `join`
* sometimes we want a child fiber to outlive the scope of the parent
    * operator called `forkDaemon` which forks the fiber as a daemon fiber
        ```
        trait ZIO[-R, +E, +A] {
            def forkDaemon: URIO[R, Fiber[E, A]]
        }
        ```
    * fork into a new fiber attached to the global scope
    * they run in the background doing their work until they end with failure or success
    * example: background jobs that should just keep on going
* supervision
    1. every fiber has a scope
    1. every fiber is forked in a scope
    1. fibers are forked in the scope of the current fiber unless otherwise specified
    1. the scope of a fiber is closed when the fiber terminates, either through success, failure, or interruption
    1. when a scope is closed all fibers forked in that scope are interrupted
    * summary: fibers can’t outlive the fiber that forked them

### interrupting
* by interrupting a fiber says that we do not need this fiber to do its work anymore
    * it can immediately stop execution without returning a result
* tells the Executor that the fiber must not be scheduled anymore
* if the fiber already succeeded with its value when interrupted => `Exit.Success[A]`, an `Exit.Failure[Cause.Interrupt]` otherwise
    ```
    trait Fiber[+E, +A] {
      def interrupt: UIO[Exit[E, A]]
    }
    ```
* several cases that we need to interrupt the execution of other fibers
    1. parent fiber started child fibers and later decide that it doesn't need the result of some
    1. two fibers start race with each other
        * the loser of a race, if still running, is canceled
    1. user may want to stop some already running tasks
        * example: clicking on the "stop" button to prevent downloading more files
    1. timeouts: computations run longer than expected
    1. one effect fails during the execution of many effects in parallel => the others will be canceled
    1. ZIO performs automatic interruption for this reasons
* interrupting running fiber causes any finalizers associated with that fiber to be run
    * when we interrupt a thread we have no guarantee that any finalizers will be executed
        * we could leave the system in an inconsistent state
            * example: opened a file but not closed it
    * any finalizers will immediately begin execution
* all fibers are interruptible by default
    * when a fiber interrupts another fiber, we know that the interruption occurs, and it always works
    * we can declare a fiber as uninterruptible
        * fiber will execute till the end even if it receives an interrupt signal
        * example: `Fiber#uninterruptible`
* ZIO runtime checks for interruption before executing each instruction
    * ZIO runtime does not know how to interrupt single blocks of side effecting code imported into ZIO
        * constructors that import arbitrary code into a ZIO execute that code as a single statement
        * example: effect will not be interruptible during execution
            ```
            val effect: UIO[Unit] = UIO.succeed {
                var i = 0
                while (i < 100000) {
                    println(i)
                    i += 1
                }
            }
            ```
    * is not a problem because in most cases ZIO programs consist of large numbers of smaller statements
        * glued together with operators like flatMap
        * normally: plenty of opportunities for interruption.
    * we may really want to perform an operation a hundred thousand times in a tight loop in a single effect for efficiency
        * how to interrupt it?
            * use specialized constructors like `attemptBlockingCancelable`, etc to provide our own logic for
            how ZIO should interrupt the code we are importing
    * `attemptBlocking` doesn't translate the ZIO interruption into thread interruption (`Thread.interrupt`)
        * use `attemptBlockingInterrupt` for that purpose
            * adds significant overhead
* implementation
    * naive way: provide a mechanism that one fiber can kill/terminate another fiber
        * drawback: doesn't guarantee to leave the shared mutable state in an internally consistent state.
            * if the target fiber is in the middle of changing a shared state
    * polling
        * used by: imperative languages
            * example: java
        * target fiber keep polling the interrupt status
            * based on the interrupt status will find out that whether there is an interruption request or not
        * fiber itself takes care of critical sections
            * should ignore the interruption and postpone the delivery of interruption during the critical section
        * drawback
            * if the programmer forget to poll regularly enough, then the target fiber become unresponsive
            and cause deadlocks
            * what is more: polling a global flag is not a functional operation (doesn't fit with ZIO's paradigm)
    * asynchronous
        * in critical sections the target fiber disable the interruptibility of these regions
        * purely-functional solution and doesn't require to poll a global state
            * ZIO uses this solution for its interruption model
        * fully asynchronous signalling mechanism
* ZIO provides structured concurrency
    * child fibers are scoped to their parents
        * parent fiber is interrupted => all its children interrupted
        * parent has completed its job => child fiber will be interrupted
            * solution: join its parent
    * it is almost impossible to leak fibers because child fibers are guaranteed to complete before their parents
    * gives us a way to reason about fiber lifespans
        * we can statically reason about the lifetimes of children fibers just by looking at our code

### join
* method to wait for the termination of a fiber
      ```
      trait Fiber[+E, +A] {
        def join: IO[E, A]
      }
      ```
* waits for the result of a computation being performed concurrently and makes it available to the current computation
* fork means run in the background; join means wait for a result
* execution in the current fiber can’t continue until the joined fiber completes execution
* fibers don’t block any thread during the waiting associated with the call of the join method
    * ZIO runtime registers a callback to be invoked when the forked fiber completes execution and
    then the current fiber suspends execution
* translates the result of joined fiber back to the current fiber
    * joining a fiber that has failed will result in a failure
* await
    ```
    trait Fiber[+E, +A] {
        def await: UIO[Exit[E, A]]
    }
    ```
    * used to inspect whether our fiber succeeded or failed, we can call await on that fiber
    * will wait for that fiber to terminate
        * gives us back the fiber's value as an Exit
    * similar to join, but they react differently to errors and interruption
        * await always succeeds with Exit information, even if the fiber fails or is interrupted
        * join on a fiber that fails will itself fail with the same error as the fiber
            * join on a fiber that is interrupted will itself become interrupted

### high level methods
* usually you don't work with fork & join but with higher level operators:
    * `ZIO#foreachPar`
    * `ZIO#zipPar`
        * execute actions in parallel
        * has resource-safe semantics
            * if one computation fails, the other computation will be interrupted, to prevent wasting resources
    * `ZIO#race`
        * value of the first action that completes successfully will be returned
        * has resource-safe semantics
            * if one of the two actions returns a value, the other one will be interrupted, to prevent wasting resources

### FiberRef
* analogy: different threads have different ThreadLocal, different fibers have different FiberRefs
    * FiberRef is the fiber version of ThreadLocal with significant improvements in its semantics
* use whenever you want "context" that is not reflected in the environment type
    * context = all the information workflow needs to have that isn't reflected in the arguments to the function
    that creates it
* design difference to ThreadLocal
    * ThreadLocal only has a mutable state in which each thread accesses its own copy, but threads 
    don't propagate their state to their children's
* different fibers who hold the same FiberRef[A] can independently set and retrieve values of the reference, 
without collisions
* whenever a child's fiber is created from its parent, the FiberRef value of parent fiber propagated to its child fibers
    * vs ThreadLocal
        * ThreadLocals do not propagate their values across the sort of graph of threads
        * ThreadLocal value is not propagated from parent to child
    * when the child set a new value of FiberRef, the change is visible only to the child itself
        * if we join a fiber then its FiberRef is merged back into the parent fiber
            * by default: the last fiber which is going to join will override the parent's FiberRef value
                * patch theory
                    * GIT analogy
                        * main: parent fiber
                        * branch1: left fiber
                        * branch2: right fiber
                    * example
                        ```
                        trait FiberRef[A] {
                          def diff(oldValue: Value, newValue: Value): Patch // oldValue = branch1, newValue = branch2, we can diff to create data structure describing update
                          def combine(first: Patch, second: Patch): Patch // git squash
                          def patch(patch: Patch)(oldValue: Value): Value // oldValue = code, patch - MR
                        }
                        ```
            * customization: FiberRef.make(initial = A, join = (A, A) => A)
                * example
                    ```
                    val run = for {
                        _ <- withRetries(5).zip(withRetryInterval(200))
                        _ <- retryConfig.get.debug("retryConfig")
                        // zip - both branches are included: withRetries and withRetryInterval
                        // in case of zipPar, only right part will be included
                    } yield ()
                    ```
            * join has higher-level semantics that `await` because it will fail if the child fiber failed
                * and it will also merge back its value to its parent
* FiberRef is automatically garbage collected once the Fiber owning it is finished
    ```
    def make[A](
      initial: => A,
      fork: A => A = (a: A) => a,
      join: (A, A) => A = ((_: A, a: A) => a)
    )(implicit trace: Trace): ZIO[Scope, Nothing, FiberRef[A]] =
      makeWith(unsafe.make(initial, fork, join)(Unsafe.unsafe))

    private def makeWith[Value, Patch](
      ref: => FiberRef.WithPatch[Value, Patch]
    )(implicit trace: Trace): ZIO[Scope, Nothing, FiberRef.WithPatch[Value, Patch]] =
      ZIO.acquireRelease(ZIO.succeed(ref).tap(_.update(identity)))(_.delete)
    ```
* vs Ref
    * Ref = one variable shared between different fibers
        * used for communicating between fibers
    * FiberRef = each fiber gets its own copy of the FiberRef
        * used for maintaining some scoped state or context
            * example: LoggingContext
    * with FiberRef we are free to do as many separate get and set as we want
        ```
        val retryConfig: FiberRef[Map[String, Int]] =
            Unsafe.unsafe { implicit unsafe =>
                FiberRef.unsafe.make(Map("retries" -> 3, "retryInterval" -> 100))
            }

        def withRetries(n: Int): Task[Unit] =
            for {
                map <- retryConfig.get
                _   <- retryConfig.set(map.updated("retries", n))
            } yield ()
        ```
        * with Ref it would not be a good thing
            * no guarantee to be done atomically
            * vs FiberRef: each FiberRef is always updating its own copy
    * example
        * Ref
            ```
            val refExample = for {
                ref <- Ref.make("V1")
                v2 = ref.set("V2") *> ref.get.debug("V2?") *> ref.set("V1")
                v3 = ref.set("V3") *> ref.get.debug("V3?") *> ref.set("V1")
                _ <- v2.zipPar(v3)
            } yield ()
            ```
            result
            ```
            V2?: V2 // or V3
            V3?: V2 // or V3
            ```
        * FiberRef
            ```
            val fiberRefExample = for {
                ref <- FiberRef.make("V1")
                v2 = ref.set("V2") *> ref.get.debug("V2?") *> ref.set("V1")
                v3 = ref.set("V3") *> ref.get.debug("V3?") *> ref.set("V1")
                _ <- v2.zipPar(v3) // here we join a fiber
            } yield ()
            ```
            result
            ```
            V2?: V2 // always
            V3?: V3 // always
            ```
* use `locally` to scope FiberRef value only for a given effect
    * example
        ```
        ref <- FiberRef.make("a")
        fiber <- ref.locally("b")(ref.get.flatMap(Console.printLine)).fork // prints b; try replace it with set
        _ <- fiber.join
        _ <- ref.get.flatMap(Console.printLine) // prints a
        ```
    * implementation
        ```
        def locally[R, E, B](newValue: A)(zio: ZIO[R, E, B])(implicit trace: Trace): ZIO[R, E, B] =
            ZIO.acquireReleaseWith(get <* set(newValue))(set)(_ => zio)
        ```
        * `<*`: sequences the specified effect after this effect, but ignores the value produced by this effect
            ```
            final def <*[R1 <: R, E1 >: E, B](that: => ZIO[R1, E1, B])(implicit trace: Trace): ZIO[R1, E1, A] =
              self.flatMap(a => that.as(a))
            ```
