package nachos.threads;

import nachos.machine.*;


/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock)
    {
    	// # Q2
    	// Allocate a new ThreadQueue, do not transfer priority
    	// to the ThreadQueue
    	rr = new RoundRobinScheduler();
    	waitQ = rr.newThreadQueue(false);
    	//waitQ = ThreadedKernel.scheduler.newThreadQueue(false);
    	this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep()
    {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		
		// release the lock which was acquired by the currentThread before calling sleep 
		conditionLock.release();
		// disable the interrupts to provide atomicity
		boolean oldInterrupStatus = Machine.interrupt().disable();
		// place the current thread into the waitQ, and go to sleep
		// another thread will call teh queue to wake up one or all
		// sleeping thread
		waitQ.waitForAccess(KThread.currentThread());
		KThread currentThread = KThread.currentThread();
		currentThread.sleep();
		// at this point this thread has been wakened
		conditionLock.acquire();
		// don't forget to enable interrupts with the old interrupt status
		Machine.interrupt().restore(oldInterrupStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake()
    {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    	// disable the interrupts to provide atomicity
		boolean oldInterrupStatus = Machine.interrupt().disable();
    	// Wake up the "oldest" thread on the queue
		KThread oldestThread = waitQ.nextThread();
		if(oldestThread == null)
		{
			Lib.debug(	dbgThread, "Condition2::wake() thread " + KThread.currentThread().getName() + 
						" waking an empty Q!");
		}
		else
		{
			// wake up!
			oldestThread.ready();
		}
		// don't forget to enable interrupts with the old interrupt status
		Machine.interrupt().restore(oldInterrupStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll()
    {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    	// disable the interrupts to provide atomicity
		boolean oldInterrupStatus = Machine.interrupt().disable();
    	// Wake up the all the threads one after the other (FIFO order)
		KThread oldestThread = null;
		while( (oldestThread = waitQ.nextThread()) != null)
		{
			// wake up!
			oldestThread.ready();
		}
		// don't forget to enable interrupts with the old interrupt status
		Machine.interrupt().restore(oldInterrupStatus);
    }
    
    // # Q2
    private static class MyInt
    {
 		public MyInt()
 		{
 			intVal = 1;
 		}
 		public int intVal;
    }
    
    private static class ConditionWaiter implements Runnable
    {
 	   ConditionWaiter(Condition2 cond, Lock condLock, MyInt counter, int which)
            {
                    this.which = which;
                    this.cond = cond;
                    this.condLock = condLock;
                    this.counter = counter;
            }
            public void run()
            {
                    condLock.acquire();
                    Lib.debug(dbgThread, "### thread " + which + " (ConditionWaiter) - aquiring, counter = " + this.counter.intVal);
                    // we want counter to be even, it is initialized as 1
                    if(this.counter.intVal != 2)
                    {
                 	   // go to sleep until thread B increments the counter
                 	   Lib.debug(dbgThread, "### thread " + which + " (ConditionWaiter) - going to sleep");
                        cond.sleep();
                        // being here means B signaled A and the lock has been acquired by A
                        Lib.debug(dbgThread, "### thread " + which + " (ConditionWaiter) - condition has met");
                        // ok now I have been woken up, I will print some sexy log and release the lock
                        Lib.debug(dbgThread, "### thread " + which + " (ConditionWaiter) - waking up, counter = " + this.counter.intVal);
                    }
                    condLock.release();
            }
            private int which;
            private MyInt counter;
            private Condition2 cond;
            private Lock condLock;
    }
    
    private static class ConditionAwakaner implements Runnable
    {
 	   ConditionAwakaner(Condition2 cond, Lock condLock, MyInt counter, int which)
        {
                this.which = which;
                this.condLock = condLock;
                this.counter = counter;
                this.cond = cond;
        }
        public void run()
        {
                condLock.acquire();
                Lib.debug(dbgThread, "### thread " + which + " (ConditionAwakaner) - incrementing counter");
                this.counter.intVal = 2;
                Lib.debug(dbgThread, "### thread " + which + " (ConditionAwakaner) - counter is " + this.counter.intVal);
                cond.wake();
                condLock.release();
        }
        private int which;
        private MyInt counter;
        private Lock condLock;
        private Condition2 cond;
    }

    
    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
    	// # Q2
        // run a test for Condition2
        Lib.debug(dbgThread, "# Starting Coondition2 test");
        Lock lok = new Lock();
        Condition2 cond = new Condition2(lok);
        MyInt counter = new MyInt();
       
        KThread t1 = new KThread(new ConditionWaiter(cond, lok, counter, 3)).setName("ConditionWaiter");
        KThread t2 = new KThread(new ConditionAwakaner(cond, lok, counter, 4)).setName("ConditionAwakaner");
        t1.fork();
        t2.fork();
        // wait
        t1.join();
        t2.join();
    }
    
    // # Q2
    // We will use a ThreadQueue to provide a mechanism for threads to wait
    // for the condition variable to be released
    // For Q2 we use roundrobin scheduler to provide a ThreadQueue which is a FIFO queue implemented
    // with a linked list. This means that threads waiting on our Condition will be wakened
    // in the FIFO order
    private RoundRobinScheduler rr;
    private ThreadQueue waitQ;
    private Lock conditionLock;
    private static final char dbgThread = 't';
}
