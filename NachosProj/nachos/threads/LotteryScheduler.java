package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler()
    {
    	
    }
    
    public static void selfTest()
    {
    	System.out.println("LotteryScheduler.selfTest1() - testing scheduler");
    	//selfTest1();
    	System.out.println("LotteryScheduler.selfTest1() - testing scheduler - priority donation");
    	selfTest2();
    }
    
    /*
     * selfTest1
     * Just run 4 threads with different priorities and observe their scheduling
     * Since we are in lottery scheduling we expect the probability of a thread to
     * be scheduled to be:
     * P(t1) = 2/20 = 0.1
     * P(t2) = 5/20 = 0.25
     * P(t3) = 10/20 = 0.5
     * P(t4) = 3/20 = 0.15
     * 
     * 10/13/2009 - I ran this test with RR scheduler and each thread gets a slice
     * as expected. When turning on Lottery Scheduling we get the expected distribution.
     * -- Ronny A.
     */
    private static void selfTest1()
    {
    	// create 4 SimpleRunner object threads
    	KThread t1 	= new KThread(new SimpleRunner(1)).setName("Runner1");
    	KThread t2 	= new KThread(new SimpleRunner(2)).setName("Runner2");
    	KThread t3 	= new KThread(new SimpleRunner(3)).setName("Runner3");
    	KThread t4 	= new KThread(new SimpleRunner(4)).setName("Runner4");
    	// set priorities
    	boolean oldInterrupStatus = Machine.interrupt().disable();
        ThreadedKernel.scheduler.setPriority(t1, 2);
        ThreadedKernel.scheduler.setPriority(t2, 5);
        ThreadedKernel.scheduler.setPriority(t3, 10);
        ThreadedKernel.scheduler.setPriority(t4, 3);
		Machine.interrupt().restore(oldInterrupStatus);
		// fork all
		t1.fork();
        t2.fork();
        t3.fork();
        t4.fork();
        
        t1.join();
        t2.join();
        t3.join();
        t4.join();
    }
    private static class SimpleRunner implements Runnable
    {
    	SimpleRunner(int id)
    	{
			this.id = id; 
    	}
    	public void run()
    	{
    		for(int i = 0; i < 100; ++i)
    		{
    			Lib.debug(dbgThread, "##@@ SimpleRunner" + id + " says:   " + i);
    			KThread.currentThread().yield();
    		}
    	}
    	int id;
    }
    
    /*
     * selfTest2
     * Test priority donation with a lottery scheduler
     */
    private static void selfTest2()
    {
    	Semaphore sem = new Semaphore(0);
    	Lock lk = new Lock();
    	
    	// create T1 - T4
    	KThread t1 	= new KThread(new T1(sem, lk)).setName("T1");
    	KThread t2 	= new KThread(new T2(lk, 2)).setName("T2");
    	KThread t3 	= new KThread(new T2(lk, 3)).setName("T3");
    	KThread t4 	= new KThread(new T4()).setName("T4");
    	// set priorities
    	boolean oldInterrupStatus = Machine.interrupt().disable();
        ThreadedKernel.scheduler.setPriority(t1, 2);
        ThreadedKernel.scheduler.setPriority(t2, 5);
        ThreadedKernel.scheduler.setPriority(t3, 10);
        ThreadedKernel.scheduler.setPriority(t4, 3);
		Machine.interrupt().restore(oldInterrupStatus);
		// run t1
    	t1.fork();
    	// wait for t1 to release the sema4, we do this to make sure t1 runs 
    	// for a while before all other threads
    	sem.P();
    	// now run t2, t3 and t4
        t2.fork();
        t3.fork();
        t4.fork();
        
        t1.join();
        t2.join();
        t3.join();
        t4.join();
        System.out.println("LotteryScheduler.selfTest1() - done priority donation test");
    }
    
    private static class T1 implements Runnable
    {
    	T1(Semaphore s, Lock l)
    	{
    		this.sema4 = s;
    		this.lk = l;
    	}
    	public void run()
    	{
    		// acquire the lock, once this is done t2 and t3 will be
    		// waiting on this lock and donating their priority to t1
    		lk.acquire();
    		for(int i = 0; i < 100; ++i)
    		{
    			// log the priority of this thread
    			// getting the priority must be done with disabled interrupts
    			//boolean oldInterrupStatus = Machine.interrupt().disable();
    			//int p = ThreadedKernel.scheduler.getEffectivePriority();
    			//Machine.interrupt().restore(oldInterrupStatus);
    			//Lib.debug(dbgThread, "=##= T1 says: tick " + i + " --->> my effective priority is " + p);
    			
    			if(i == 19)
    			{
    				// signal the main thread that it may start forking t2, t3 and t4
    	    		sema4.V();
    			}
    			
    			if(i == 49)
    			{
    				// release the lock after 50 ticks
    				// this will show the change in effective priority
    				lk.release();
    			}
    			KThread.currentThread().yield();
    		}
    	}
    	Semaphore sema4;
    	Lock lk;
    }
    
    /*
     * T2 - this implements t2 and t3 for the test (same functionality, so I put t2 and t3 in 1 class)
     */
    private static class T2 implements Runnable
    {
    	T2(Lock l, int id)
    	{
    		this.id = id;
    		this.lk = l;
    	}
    	public void run()
    	{
    		// acquire the lock, since we made sure that t1 has the lock
    		// before this point of time, threads of this class will wait for
    		// t1 to release the lock, and in the meanwhile we expect them to
    		// donate their tickets to t1
    		lk.acquire();
    		for(int i = 0; i < 100; ++i)
    		{
    			Lib.debug(dbgThread, "=##= T" + id +" says: tick " + i);
    			KThread.currentThread().yield();
    		}
    	}
    	int id;
    	Lock lk;
    }
    
    private static class T4 implements Runnable
    {
    	T4()
    	{
    	}
    	public void run()
    	{
    		for(int i = 0; i < 100; ++i)
    		{
    			Lib.debug(dbgThread, "=##= T4 says: tick " + i);
    			KThread.currentThread().yield();
    		}
    	}
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority)
    {
    	return new LotteryPriorityQueue(transferPriority);
    }

    /*
     * getThreadState
     * # HW2 Q4
     * creates a new LotteryThreadState
     */
    protected ThreadState getThreadState(KThread thread)
    {
    	if (thread.schedulingState == null)
    	    thread.schedulingState = new LotteryThreadState(thread);

    	return (ThreadState) thread.schedulingState;
    }
    
    
    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int lotPriorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int lotPriorityMinimum = 1;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int lotPriorityMaximum = Integer.MAX_VALUE; 
    
    private static final char dbgThread = 't';
    
    protected int getPriorityDefault()
    {
    	return lotPriorityDefault;
    }
    protected int getPriorityMin()
    {
    	return lotPriorityMinimum;
    }
    protected int getPriorityMax()
    {
    	return lotPriorityMaximum;
    }
    
    protected class LotteryPriorityQueue extends PriorityQueue
    {

		LotteryPriorityQueue(boolean transferPriority)
		{
			super(transferPriority);
		}
		
		// This method is different from the pickNextThread
		// since it will modify the state of the threads waiting
		// in the data structure iQueue. 
		public KThread nextThread()
		{
			//runningThread is the thread that is hold the current lock 
			//since it has finished the nextThread will get the
			//lock and will start to run
			if(runningThread != null)
			{
				LotteryThreadState state = (LotteryThreadState) runningThread;
				state.listOfWaitingThreads.clear();
				
				if(runningThread.inheritedPriority)
				{
					runningThread.setPriority(runningThread.oldPriority);
					runningThread.inheritedPriority = false;
				}
				runningThread = null;
			}
		    Lib.assertTrue(Machine.interrupt().disabled());
		    
		    ThreadState nThread = pickNextThread();
		    if(nThread != null)
		    {
		    	iQueue.remove(nThread);
		    	nThread.acquire(this);
		    	return nThread.thread;
		    }
		    
		    return null;
		}
		
		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread()
		{
			if(iQueue.isEmpty())
			{
				return null;
			}
			return draw();
		}
	
		/*
		 * # HW2 Q4
		 * draw
		 * Draw a ticket from all the waiting threads and return the winner
		 * Here is how the draw works:
		 * A number is drawn  between 1 and sum(i) where i is all the priorities
		 * (this is the total number of tickets held by all threads)
		 * After the number was drawn, we return the winning thread holding the 
		 * winner ticket
		 */
		
		private LotteryThreadState draw()
		{
			int sumPriorities = 0;
			
			ListIterator<LotteryThreadState> iter = iQueue.listIterator();
			while(iter.hasNext())
			{
				LotteryThreadState state = iter.next();
				sumPriorities += state.oldPriority;
			}
			// generate a random number between 1 and sumPriorities
			Random generator = new Random();
			int winner = generator.nextInt(sumPriorities); // 0 based
			winner++;
			
			// get the participants of the lottery, donating threads have 0 tickets
			// so they get pulled out of the lottery
			LinkedList<LotteryThreadState> zeroTicketThreads = new LinkedList<LotteryThreadState>();
			
			iter = iQueue.listIterator();
			while(iter.hasNext())
			{
				LotteryThreadState state = iter.next();
				ListIterator<LotteryThreadState> iter2 = state.listOfWaitingThreads.listIterator();
				while(iter2.hasNext())
				{
					LotteryThreadState nonParticipatingThread = iter2.next();
					zeroTicketThreads.add(nonParticipatingThread);
				}
			}
			
			int lowerBound = 1;
			
			// draw  ticket, remember that waiting threads have donated all their tickets
			iter = iQueue.listIterator();
			while(iter.hasNext())
			{
				LotteryThreadState state = iter.next();
				if(zeroTicketThreads.contains(state))
				{
					// threads with no tickets do not participate in the lottery
					continue;
				}
				int upperBound = lowerBound + state.priority;
				if(winner >= lowerBound && winner < upperBound)
				{
					// thread with winning ticket jumps ecstatically
					return state;
				}
				lowerBound = upperBound;
			}
			return null;
		}
		
		LinkedList<LotteryThreadState> iQueue = new LinkedList<LotteryThreadState>();
    }
    
    
    
    /*
     * LotteryThreadState
     */
    protected class LotteryThreadState extends ThreadState
    {

		public LotteryThreadState(KThread thread)
		{
			super(thread);
			oldPriority = priority;
		}
		
		public void setPriority(int priority)
		{
			super.setPriority(priority);
			oldPriority = priority;
		}
		
		public int getEffectivePriority()
		{
			// # HW2 Q4
			// The effective priority of a thread is how many
			// tickets it holds + any tickets from other waiting threads
			if(listOfWaitingThreads.size() > 0)
			{
				// take of my hat, iterate over all threads waiting 
				// on the priority Q and collect all the donations
				int donation = 0;
				ListIterator<LotteryThreadState> iter = listOfWaitingThreads.listIterator();
				while(iter.hasNext())
				{
					LotteryThreadState state = iter.next();
					donation += state.priority;
				}
				// track effectiveP only for the first time
				oldPriority = priority;
				priority = oldPriority + donation;
				inheritedPriority = true;
			}
		    return priority;
		}
		
		public void waitForAccess(PriorityQueue waitQueue)
		{
			getEffectivePriority();
			LotteryPriorityQueue lotteryQ = (LotteryPriorityQueue) waitQueue;
			// # HW2 Q4 add myself to the LotteryPriorityQueue 
			lotteryQ.iQueue.add(this);
			
			// # HW2 Q4
	    	// donate my priority to the thread currently holding the queue
	    	// - done by adding myself to the running thread listOfHPThreads
		    if(waitQueue.transferPriority)
		    {
		    	LotteryThreadState state = (LotteryThreadState) waitQueue.runningThread;
		    	state.listOfWaitingThreads.add(this);
		    }
		}
		
		public LinkedList<LotteryThreadState> listOfWaitingThreads = new LinkedList<LotteryThreadState>();
    }
    
}
