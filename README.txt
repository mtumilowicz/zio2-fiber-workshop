* references
    * [How To Successfully Manage A ZIO Fiber's Lifecycle by Natan Silnitsky](https://www.youtube.com/watch?v=zUPtEbPsOqE)
    * https://blog.rockthejvm.com/zio-fibers/
    * https://www.zionomicon.com/
    * https://scalac.io/blog/build-your-own-kafka-in-zio-queues-fibers/
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


* Fibers are lightweight equivalents of operating system threads
    * Like a thread, a fiber models a running computation and instructions on a single fiber are executed sequentially
    * However, fibers are much less costly to create than operating system threads, so we can have hundreds of thousands of fibers in our program at any given time whereas maintaining this number of threads would have a severe performance impact
    * Unlike threads, fibers are also safely interruptible and can be joined without blocking.
* When we call unsafeRun the ZIO runtime creates a new fiber to execute our program
    * At this point our program hasn’t started yet, but the Fiber represents a running program that we happen to not have started yet but will be running at
    some point
    * You can think of it as a Thread we have created to run our program logic but have not started yet.
        * The ZIO runtime will then submit that Fiber for execution on some Executor.
        * An Executor is typically backed by a thread pool in multithreaded environments.
        * The Executor will then begin executing the ZIO program one instruction at a time on an underlying operating system thread, assuming one is available.
        * However, the ZIO program will not necessarily run to completion. Instead after a certain number of instructions have been executed, the maximumYieldOpCount, the ZIO program will suspend execution and any remaining program logic will be submitted to the Executor.
            * This ensure fairness.
                * Say we have five fibers.
                    * The first four fibers are performing long running computations with one million instructions each
                    * The fifth fiber is performing a short computation with only one thousand instructions
                    * If the Executor was backed by a fixed thread pool with four threads and we did not yield we would not get to start running the fifth fiber until all the other fibers had completed execution, resulting in a situation where the work of the fifth fiber was not really performed concurrently with the other fibers, leading to potentially unexpected and undesirable results.
                    * If instead each fiber yields back to the runtime after every thousand instructions then the first four fibers do a small amount of work, then the fifth fiber gets to do its work, and then the other four fibers get to continue their work.
                    * This creates the result of all five fibers running concurrently even though only four are ever actually running at any given moment.
            * In essence, fibers represent units of “work” and the Executor switches between running each fiber for a brief period, just like the operating system switches between running each Thread for a small slice of time.
                * The difference is that fibers are just data types we create that do not have the same operating system overhead as threads, and since we create the runtime we can build in logic for how we ensure fairness, how we handle interruption, and so on.
* 2.2) Heap Area (Shared among Threads)
  This is also a shared resource (only 1 heap area per JVM). Information of all objects and their corresponding instance variables and arrays are stored in the Heap area. Since the Method and Heap areas share memory for multiple threads, the data stored in Method & Heap areas are not thread safe.
* https://github.com/mtumilowicz/java-stack
    * 2.3) Stack Area (per Thread)
      This is not a shared resource. For every JVM thread, when the thread starts, a separate runtime stack gets created in order to store method calls. For every such method call, one entry will be created and added (pushed) into the top of runtime stack and such entryit is called a Stack Frame.
* A thread is simply a flow of execution, and every Java program have at least one thread known as a main thread.
    * The main thread is created by the JVM whenever you run a java program.
* In Java, there are two types of threads, Daemon threads and Non-Daemon threads
    * In fact, JVM always waits until non-daemon threads to finish their work. It never exits until the last non-daemon thread finishes its work.
* Multitasking can be divided into 2 categories based on its behavior.

  Process-Based multitasking: When the operation system runs multiple processes at the same time.
  Thread-Based multitasking: When two or more threads run concurrently, that belongs to a same process.
* In java the order of threads’ executions cannot be predicted.
* Before Java 1.3 there was a thing called Green thread model. The green thread is the simplest threading library of JVM scheduled threads. In this model, each thread is an abstraction within the VM. The JVM is completely responsible for its creation and manages the process of context switching within a single process of the operating system. The underlying operating system has no part to play and can be unaware of the existence of any thread within the process. The OS sees JVM as a single process and a single thread. Therefore, any thread created by JVM is supposed to be maintained by itself.
* Current Java Releases use something called Native thread model. In the native thread model, the JVM creates and manages Java threads. But it does so use the thread API library of the underlying operating system.

* JVM is a system that runs on top of the actual operating system. It has its own memory management scheme that works in sync with the underlying platform’s memory architecture.
* Java memory manager specifies how a thread works with the synchronization process while accessing the shared variables. It segments the memory into two parts: a Stack area and the Heap area.
    * Each running thread creates its own stack in the stack area; this stack contains all the information specific or local to the thread, such as all declared primitive variables and method calls. This area is not sharable between threads and the stack size changes dynamically according to the running condition of the thread.
    * The heap area is for storing objects created by the Java application. These objects are sharable by all threads and can access the object’s method if it has a reference to it.

* Green threads, coroutines, lightweight threads, and fibers are all different names for the same basic idea: multiple threads of execution in a single address space that cooperate with no or minimal kernel support.
* Fibers are a lightweight thread of execution similar to OS threads. However, unlike OS threads, they’re cooperatively scheduled as opposed to preemptively scheduled. What this means in plain English is that fibers yield themselves to allow another fiber to run.
    * You may have used something similar to this in your programming language of choice where it’s typically called a coroutine, there’s no real distinction between coroutines and fibers other than that coroutines are usually a language-level construct, while fibers tend to be a systems-level concept.
* Other names for fibers you may have heard before include:
  * green threads
  * user-space threads
  * coroutines
  * tasklets
  * microthreads
* Scheduling
    * Cooperative scheduling
        * This idea of fibers yielding to each other is what is known as cooperative scheduling.
        * Fibers effectively move the idea of context switching from kernel-space to user-space and then make those switches a fundamental part of computation, that is, they’re a deliberate and explicitly done thing, by the fibers themselves.
        *
    * Preemptive scheduling
        * Most people familiar with threads know that you don’t have to yield to other threads to allow them to run. This is because most operating systems (OS) schedule threads preemptively.
        * The points at which the OS may decide to preempt a thread include:

          IO
          sleeps
          waits (seen in locking primitives)
          interrupts (hardware events mostly)

* While developing concurrent applications, there are several cases that we need to interrupt the execution of other fibers, for example:
    * A parent fiber might start some child fibers to perform a task, and later the parent might decide that, it doesn't need the result of some or all of the child fibers.
    * Two or more fibers start race with each other. The fiber whose result is computed first wins, and all other fibers are no longer needed, and should be interrupted.
    * In interactive applications, a user may want to stop some already running tasks, such as clicking on the "stop" button to prevent downloading more files.
    * Computations that run longer than expected should be aborted by using timeout operations.
    * When we have an application that perform compute-intensive tasks based on the user inputs, if the user changes the input we should cancel the current task and perform another one.
* Polling vs. Asynchronous Interruption
    A simple and naive way to implement fiber interruption is to provide a mechanism that one fiber can kill/terminate another fiber. This is not a correct solution, because if the target fiber is in the middle of changing a shared state, it leads to an inconsistent state. So this solution doesn't guarantee to leave the shared mutable state in an internally consistent state.

    Other than the very simple kill solution, there are two popular valid solutions to this problem:

    Semi-asynchronous Interruption (Polling for Interruption)— Imperative languages often use polling to implement a semi-asynchronous signaling mechanism, such as Java. In this model, a fiber sends a request for interruption of other fiber. The target fiber keep polling the interrupt status, and based on the interrupt status will find out that weather there is an interruption request from other fibers received or not. Then it should terminate itself as soon as possible.

    Using this solution, the fiber itself takes care of critical sections. So while a fiber is in the middle of a critical section, if it receives an interruption request, it should ignore the interruption and postpone the delivery of interruption during the critical section.

    The drawback of this solution is that, if the programmer forget to poll regularly enough, then the target fiber become unresponsive and cause deadlocks. Another problem with this solution is that polling a global flag is not a functional operation, that doesn't fit with ZIO's paradigm.

    Asynchronous Interruption— In asynchronous interruption a fiber allows to terminate another fiber. So the target fiber is not responsible for polling the status, instead in critical sections the target fiber disable the interruptibility of these regions. This is a purely-functional solution and doesn't require to poll a global state. ZIO uses this solution for its interruption model. It is a fully asynchronous signalling mechanism.

    This mechanism doesn't have the drawback of forgetting to poll regularly. And also its fully compatible with functional paradigm because in a purely-functional computation, at any point we can abort the computation, except for critical sections.
* Child Fibers Are Scoped to Their Parents
    * If a child fiber does not complete its job or does not join its parent before the parent has completed its job, the child fiber will be interrupted
    * If a parent fiber is interrupted, all its children will be interrupted
* By default, when we convert a blocking operation into the ZIO effects using attemptBlocking, there is no guarantee that if that effect is interrupted the underlying effect will be interrupted.
    * So the attemptBlocking doesn't translate the ZIO interruption into thread interruption (Thread.interrupt).
    * Instead, we should use attemptBlockingInterrupt to create interruptible blocking effects:
* If we are converting a blocking I/O to the ZIO effect, it would be better to use attemptBlockingIO which refines the error type to the java.io.IOException.
* The attemptBlockingInterrupt method adds significant overhead. So for performance-sensitive applications, it is better to handle interruptions manually using attemptBlockingCancelable
* The danger with such an interruption is that:

  If the interruption occurs during the execution of an operation that must be finalized, the finalization will not be executed.

  If this interruption occurs in the middle of a critical section, it will cause an application state to become inconsistent.

  It is also a threat to resource safety. If the fiber is in the middle of acquiring a resource and is interrupted, the application will leak resources.

  ZIO introduces the uninterruptible and uninterruptibleMask operations for this purpose.
  * The former creates a region of code uninterruptible and the latter has the same functionality but gives us a restore function that can be applied to any region of code, to restore the interruptibility of that region.
  * If you find yourself using these operators, think again to refactor your code using high-level operators like ZIO#onInterrupt, ZIO#onDone, ZIO#ensuring, ZIO.requireRelease* and many other concurrent operators like race, foreachPar, etc.

* A Fiber can be thought of as a virtual thread. A Fiber is the analog of a Java thread (java.lang.Thread), but it performs much better. Fibers are implemented in such a fashion that a single JVM thread will execute many fibers. We can think of fibers as unbounded JVM threads.
* There are some limitations with JVM threads:

  Threads are scarce — Threads on the JVM map to the operating system level threads which imposes an upper bound on the number of threads that we can have inside our application.

  Expensive on creation — The creation of threads is expensive in terms of time and memory complexity.

  Much Overhead on Context Switching — Switching between the execution of one thread to another thread is not cheap, it takes a lot of time.

  Lack of Composability — Threads are not typed. They don't have a meaningful return type. In Java, when we create a thread, we have to provide a run function that returns void. So threads cannot finish with any specific value. Due to this limitation, we cannot compose threads. Also, a thread has no type parameter for error. It is expected to throw any exception of type Throwable to signal errors.
* So whereas the mapping from JVM threads to operating system threads is one-to-one, the mapping of fibers to threads is many-to-one.
    * Each JVM thread will end up executing anywhere from hundreds to thousands or even tens of thousands of fibers concurrently, by hopping back and forth between them as necessary.
    * This gives us virtual threads that have the benefits of threads, but the scalability way beyond threads. In other words, fibers offer us massive concurrent lightweight green threading on the JVM.
* JVM threads are expensive to create in terms of time and memory complexity. Also it takes a lot of time to switch between one thread of execution to another. In contrast to that, fibers are virtual, and as they use green threading, they are considered to be lightweight cooperative threads. This means that fibers always yield their executions to each other without the overhead of preemptive scheduling.
* Threads in Java can be terminated via the stop method, but this is not a safe operation. The stop operation has been deprecated. So this is not a safe way to force kill a thread. Instead, we should try to request an interruption of the thread, but in this case, the thread may not respond to our request, and it may just go forever.
* Fiber has a safe version of this functionality that works very well. Just like we can interrupt a thread, we can interrupt a fiber too, but interruption of fibers is much more reliable. It will always work, and it probably works very fast. We don't need to wait around, we can just try to interrupt them, and they will be gone very soon.
* It's worth mentioning that in the ZIO model, all code runs on fibers. There is no such thing as code that is executed outside of fibers. When we create a main function in ZIO that returns an effect, then even if we don't explicitly fork a fiber, the effect will be executed on what is called the main fiber. It's a top-level fiber.
    * It's just like if we have a main function in Java then that main function will execute on the main thread. There is no code in Java that does not execute on a thread. All code executes on a thread even if you didn't create a thread.
* ZIO provides structured concurrency. The way ZIO's structured concurrency works is that the child fibers are scoped to their parent fibers which means that when the parent effect finishes execution, then all childs' effects will be automatically interrupted. So when we fork, and we get back a fiber, the fiber's lifetime is bound to the parent fiber that forked it. It is almost impossible to leak fibers because child fibers are guaranteed to complete before their parents.
    * The structured concurrency gives us a way to reason about fiber lifespans. We can statically reason about the lifetimes of children fibers just by looking at our code. We don't need to insert complicated logic to keep track of all the child fibers and manually shut them down.
* Sometimes we want a child fiber to outlive the scope of the parent. What can we do in that case? Well, ZIO offers an operator called forkDaemon which forks the fiber as a daemon fiber. Daemon fibers can outlive their parents. They can live forever. They run in the background doing their work until they end with failure or success. This gives us a way to spawn background jobs that should just keep on going regardless of what happens to the parent.
* To perform an effect without blocking the current process, we can use fibers, which are a lightweight concurrency mechanism.
* Lifetime of Child Fibers
    Fork With Automatic Supervision— If we use the ordinary ZIO#fork operation, the child fiber will be automatically supervised by the parent fiber. The lifetime child fibers are tied to the lifetime of their parent fiber. This means that these fibers will be terminated either when they end naturally, or when their parent fiber is terminated.
        * Forking with automatic supervision is the default strategy. When we use the ZIO#fork method, the lifetime of child fibers is tied to their parent fiber. However, sometimes we don't want this behavior. Instead, we use three other alternatives.
    Fork in Global Scope (Daemon)— Sometimes we want to run long-running background fibers that aren't tied to their parent fiber, and also we want to fork them in a global scope. Any fiber that is forked in global scope will become daemon fiber. This can be achieved by using the ZIO#forkDaemon operator. As these fibers have no parent, they are not supervised, and they will be terminated when they end naturally, or when our application is terminated.
    Fork in Local Scope— Sometimes, we want to run a background fiber that isn't tied to its parent fiber, but we want to live that fiber in the local scope. We can fork fibers in the local scope by using ZIO#forkScoped. Such fibers can outlive their parent fiber (so they are not supervised by their parents), and they will be terminated when their life end or their local scope is closed.
    Fork in Specific Scope— This is similar to the previous strategy, but we can have more fine-grained control over the lifetime of the child fiber by forking it in a specific scope. We can do this by using the ZIO#forkIn operator.
    * The second and third strategies are required to work with the Scope data type. A contextual data type that describes a resource's lifetime, in this case, the fiber's lifetime. To learn more about Scope we have a separate section on it.
* Whenever we need to start a fiber, we have to fork an effect to get a new fiber. This is similar to the start method on Java thread or submitting a new thread to the thread pool in Java, it is the same idea. Also, joining is a way of waiting for that fiber to compute its value. We are going to wait until it's done and receive its result.
* await
  To inspect whether our fiber succeeded or failed, we can call await on that fiber. This call will wait for that fiber to terminate, and it will give us back the fiber's value as an Exit. That exit value could be failure or success:
  * await is similar to join, but they react differently to errors and interruption: await always succeeds with Exit information, even if the fiber fails or is interrupted. In contrast to that, join on a fiber that fails will itself fail with the same error as the fiber, and join on a fiber that is interrupted will itself become interrupted.
* To execute actions in parallel, the zipPar method can be used:
    * The zipPar combinator has resource-safe semantics. If one computation fails, the other computation will be interrupted, to prevent wasting resources.
* Two actions can be raced, which means they will be executed in parallel, and the value of the first action that completes successfully will be returned.
    * The race combinator is resource-safe, which means that if one of the two actions returns a value, the other one will be interrupted, to prevent wasting resources.
* In Java, a thread can be interrupted via Thread#interrupt from another thread, but it may refuse the interruption request and continue processing. Unlike Java, in ZIO when a fiber interrupts another fiber, we know that the interruption occurs, and it always works.
    * All fibers are interruptible by default. To make an effect uninterruptible we can use Fiber#uninterruptible, ZIO#uninterruptible or ZIO.uninterruptible. ZIO provides also the reverse direction of these methods to make an uninterruptible effect interruptible.
* When a fiber has done its work or has been interrupted, the finalizer of that fiber is guaranteed to be executed
    * As we saw in the previous section, the ZIO runtime gets stuck on interruption until the fiber's finalizer finishes its job. We can prevent this behavior by using ZIO#disconnect or Fiber#interruptFork which perform fiber's interruption in the background or in separate daemon fiber
* The ZIO#attemptBlocking is interruptible by default, but its interruption will not translate to JVM thread interruption. Instead, we can use ZIO#attemptBlockingInterrupt to translate the ZIO interruption of that effect into JVM thread interruption.
* If we never cancel a running effect explicitly, ZIO performs automatic interruption for several reasons:

  Structured Concurrency — If a parent fiber terminates, then by default, all child fibers are interrupted, and they cannot outlive their parent. We can prevent this behavior by using ZIO#forkDaemon or ZIO#forkIn instead of ZIO#fork.

  Parallelism — If one effect fails during the execution of many effects in parallel, the others will be canceled. Examples include foreachPar, zipPar, and all other parallel operators.

  Timeouts — If a running effect with a timeout has not been completed in the specified amount of time, then the execution is canceled.

  Racing — The loser of a race, if still running, is canceled.
* By default, fibers give no guarantees as to which thread they execute on. They may shift between threads, especially as they execute for long periods of time.
* What if though, we have a CPU Work operation that takes a really long time to run? Let's say 30 seconds it does pure CPU Work very computationally intensive? What happens if we take that single gigantic function and put that into a ZIO#attempt? In that case there is no way for the ZIO Runtime to force that fiber to yield to other fibers.
    * In this situation, the ZIO Runtime cannot preserve some level of fairness, and that single big CPU operation monopolizes the underlying thread. It is not a good practice to monopolize the underlying thread.
    * o how can we determine that our CPU Work can yield quickly or not?

      If that overall CPU Work composes many ZIO operations, then due to the composition of ZIO operations, it has a chance to yield quickly to other fibers and doesn't monopolize a thread.
      If that CPU work doesn't compose any ZIO operations, or we lift that from a legacy library, then the ZIO Runtime doesn't have any chance of yielding quickly to other fibers. So this fiber is going to monopolize the underlying thread.
    * So as a rule of thumb, when we have a huge CPU Work that is not chunked with built-in ZIO operations, and thus going to monopolize the underlying thread, we should run that on a dedicated thread pool that is designed to perform CPU-driven tasks.
* ZIO has a special thread pool that can be used to do these computations. That's the blocking thread pool. The ZIO#blocking operator and its variants (see here) can be used to run a big CPU Work on a dedicated thread. So, it doesn't interfere with all the other work that is going on simultaneously in the ZIO Runtime system.

* Blocking I/O
  Inside Java, there are many methods that will put our thread to sleep. For example, if we call read on a socket and there is nothing to read right now because not enough bytes have been read from the other side over the TCP/IP protocol, then that will put our thread to sleep.
    * What we refer to as blocking I/O is not necessarily just an I/O operation. Remember every time we use a lock we are also parking a thread. It goes to sleep, and it has to be woken up again. We refer to this entire class of operations as blocking I/O.
* ZIO has one primary built-in fixed thread pool. This sort of workhorse thread pool is designed to be used for the majority of our application requirements. It has a certain number of threads in it and that stays constant over the lifetime of our application.
    * Why is that the case? Well because for the majority of workloads in our applications, it does not actually help things to create more threads than the number of CPU cores. If we have eight cores, it does not accelerate any sort of processing to create more than eight threads. Because at the end of the day our hardware is only capable of running eight things at the same time.
    * So for that reason, ZIO's default thread pool is fixed with a number of threads equal to the number of CPU cores. That is the best practice. That means that no matter how much work we create if we create a hundred thousand fibers, they will still run on a fixed number of threads.
* Asynchronous I/O
  The third category is asynchronous I/O, and we refer to it as Async Work. Async Work is code that whenever it runs into something that it needs to wait on, instead of blocking and parking the thread, it registers a callback, and returns immediately.
  * It allows us to register a callback and when that result is available then our callback will be invoked. Callbacks are the fundamental way by which all async code on the JVM works. There is no mechanism in the JVM right now to support async code natively, but once that would happen in the future, probably in the Loom project, it will simplify a lot of things.
  * The drawback of callbacks is they are not so pretty and fun to work with.
* Most of the ZIO operations that one would expect to be blocking do actually not block the underlying thread, but they offer blocking semantics managed by ZIO. For example, every time we see something like ZIO.sleep or when we take something from a queue (queue.take) or offer something to a queue (queue.offer) or if we acquire a permit from a semaphore (semaphore.withPermit) and so forth, we are just blocking semantically without actually blocking an underlying thread
    * If we use the corresponding methods in Java, like Thread.sleep or any of its lock machinery, then those methods are going to block a thread. So this is why we say that ZIO is 100% non-blocking, while Java threads are not.

* linux thread ~ linux process
  * from the kernel point of view, only processes exist
  * so-called thread is just a different kind of process
  * difference: flag (1 bit) - to share memory with parent
    * yes => thread; no => process
* Using ZIO#forkDaemon, the effect is fork into a new fiber attached to the global scope. This means that the forked fiber will continue to run when the previous fiber that executed the returned effect terminates.
* To perform an effect without blocking the current process, we can use fibers in ZIO Library, which are a lightweight concurrency mechanism.
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
   * Forking creates a new fiber that executes the effect being forked concurrently with the current fiber.
   * example
        lazy val example2 = for { _ <- doSomething.fork
        _ <- doSomethingElse
        } yield ()
        Here there is no guarantee about the order of execution of doSomething and doSomethingElse
* ZIO fibers provide the join method to wait for the termination of a fiber:
    ```
    trait Fiber[+E, +A] {
      def join: IO[E, A]
    }
    ```
    * Through the join method, we can wait for the result of concurrent computation and eventually use it
    * join waits for the result of a computation being performed concurrently and makes it available to the current computation.
    * An important characteristic of join is that it does not block any underlying operating system threads
        * When we join a fiber, execution in the current fiber can’t continue until the joined fiber completes execution
        * But no actual thread will be blocked waiting for that to happen
        * Instead, internally the ZIO runtime registers a callback to be invoked when the forked fiber completes execution and then the current fiber suspends execution
    * Joining a fiber translates the result of that fiber back to the current fiber, so joining a fiber that has failed will result in a failure
    * if we instead want to wait for the fiber but be able to handle its result whether it is a success or a failure we can use await
        trait Fiber[+E, +A] {
        def await: UIO[Exit[E, A]]
        }
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
    * Interrupting a fiber says that we do not need this fiber to do its work anymore and it can immediately stop execution without returning a result
    * If the fiber has already completed execution by the time it is interrupted the returned value will be the result of the fiber. Otherwise it will be a failure with Cause.Interrupt.
    * We can interrupt a Thread by calling Thread.interrupt but this is a very “hard” way to stop a thread’s execution. When we interrupt a thread we have no guarantee that any finalizers associated with logic currently being executed by that thread will be run, so we could leave the system in an inconsistent state where we have opened a file but not closed it, or we have debited one bank
      account without crediting another.
        * In contrast, interrupting a fiber in ZIO causes any finalizers associated with that fiber to be run.
    * Second, interruption in ZIO is much more efficient than interruption in thread based models because fibers themselves are so much more lightweight.
        * A thread is quite expensive to create, relatively speaking, so even if we can interrupt a thread we have to be quite careful in doing so because we are destroying this valuable resource
        * In contrast fibers are very cheap to create, so we can create many fibers to do our work and interrupt them when they aren’t needed anymore.
        * Interruption just tells the ZIO runtime that there is no more work to do on this fiber.
    * This is true in the sense that ZIO effects describe rather than do things so we can run the same effect multiple times.
        * But it is also true in the sense that a ZIO effect is literally a list of instructions.
            val greet: UIO[Unit] = for {
                name <- ZIO.succeed(StdIn.readLine("What's your name?"))
            _ <- ZIO.succeed(println(s"Hello, $name!")) } yield ()
        * First, every ZIO effect translates into one or more statements in this plan. Each statement describes one effect and operators like flatMap connect each of these statements togethers.
          Second, constructors that import arbitrary code into a ZIO execute that code as a single statement.
            * When we import Scala code into a ZIO using a constructor like succeed the ZIO runtime just sees a single block of Scala code, essentially a thunk () => A.
            * The ZIO runtime has no way to know whether that Scala code is performing a simple statement, like reading a single line from the console, or a much more complex program like reading 10,000 lines from a file.
            * This is important because the ZIO runtime checks for interruption before exe- cuting each instruction.
        * Another implication is that in the absence of special logic the ZIO runtime does not know how to interrupt single blocks of side effecting code imported into ZIO.
            val effect: UIO[Unit] = UIO.succeed {
            var i = 0
            while (i < 100000) {
            println(i)
            i += 1 }
            }
            * Here effect will not be interruptible during execution
                * Interruption happens only “between” statements in our program and here effect is a single statement because it is just wrapping one block of Scala code.
                * Normally this is not a problem because in most cases ZIO programs consist of large numbers of smaller statements glued together with operators like flatMap, so normally there are plenty of opportunities for interruption.
            * Sometimes we do need to be able to interrupt an effect like this. For example, we may really want to perform an operation a hundred thousand times in a tight loop in a single effect for efficiency.
              In this case we can use specialized constructors like attemptBlockingCancelable, attemptBlockingInterrupt, and asyncInterrupt to provide our own logic for how ZIO should interrupt the code we are importing.
    * The basic rule is that interruption does not return until all logic associated with the interrupted effect has completed execution.
        * When fiber is interrupted any finalizers associated with it will immediately begin execution
        * If we want to interrupt an effect without waiting for the interruption to complete we can simply fork it.

* Finally, unlike threads, we can attach finalizers to a fiber.
    * A finalizer will close all the resources used by the effect.
    * The ZIO library guarantees that if an effect begins execution, its finalizers will always be run, whether the effect succeeds with a value, fails with an error, or is interrupted.
* Last but not least, we can declare a fiber as uninterruptible. As the name suggests, an uninterruptible fiber will execute till the end even if it receives an interrupt signal.
    * Any part of a ZIO effect is either interruptible or uninterruptible. By default all ZIO effects are interruptible but some operators may make parts of a ZIO effect uninterruptible.
    * The basic operators for controlling interruptibility are interruptible and uninterruptible.
    * If the effect has been interrupted and is not interruptible it continues executing instructions as normal, checking each time whether the interruptibility status has changed.
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


* Fiber Supervision
    * In the main fiber we fork parent and parent in turn forks child. Then we interrupt parent when child is still doing work. What should happen to child here?
    * ZIO implements a fiber supervision model
        1. Every fiber has a scope
        2. Every fiber is forked in a scope
        3. Fibers are forked in the scope of the current fiber unless otherwise specified
        4. The scope of a fiber is closed when the fiber terminates, either through
        success, failure, or interruption
        5. When a scope is closed all fibers forked in that scope are interrupted
    * The implication of this is that by default fibers can’t outlive the fiber that forked them
    * If you do need to create a fiber that outlives its parent (e.g. to create some background process) you can fork a fiber on the global scope using forkDaemon.
      trait ZIO[-R, +E, +A] {
      def forkDaemon: URIO[R, Fiber[E, A]]
      }

* Normally, fibers will be executed by ZIO’s default Executor. However, sometimes we may want to execute some or all of an effect with a particular Executor. We already saw an example of this with our discussion of the Blocking service.
