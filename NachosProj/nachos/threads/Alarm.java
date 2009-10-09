package nachos.threads;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import nachos.machine.*;


/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm()
    {
    	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });

    	 sleepers  = new HashMap<Long, LinkedList<KThread>>();
    	 multimapProtector = new Lock();
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt()
    {
    	KThread.currentThread().yield();

    	boolean oldInterrupStatus = Machine.interrupt().disable();


    	int mapsz = sleepers.size();
    	Iterator iter = sleepers.entrySet().iterator();
    	for(int i = 0; i < mapsz; ++i)
    	{
    		Map.Entry entry = (Map.Entry) iter.next();
    		Long time = (Long)entry.getKey();
    		LinkedList<KThread> lst = (LinkedList<KThread>)entry.getValue();
    		if( Machine.timer().getTime() >= time)
    		{
    			int lstsize = lst.size();
    			for(int j = 0; j < lstsize ; j++)
    			{
    				KThread thread = lst.get(j);
    				thread.ready();
    				lst.remove(j);
    			}
    		}
    	}
		// enable the interrupt
		Machine.interrupt().restore(oldInterrupStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x)
    {
		Long wakeTime = Machine.timer().getTime() + x;
		// inserting in the multimap should be mutually exclusive to protect the multimap
		multimapProtector.acquire();
		LinkedList<KThread> lst = (LinkedList<KThread>) sleepers.get(wakeTime);
		if(lst == null)
		{
			lst = new LinkedList<KThread>();
			sleepers.put(wakeTime, lst);
		}
		lst.add(KThread.currentThread());
		// release the lock
		multimapProtector.release();
		// put me to sleep
		boolean oldInterrupStatus = Machine.interrupt().disable();
		KThread.currentThread().sleep();
		// enable the interrupt
		Machine.interrupt().restore(oldInterrupStatus);
    }

    // # Q3
    private static class Sleepy implements Runnable
    {
 	   Sleepy(String name, Alarm alrm, long sleepTime)
 	   {
 		   this.name = name;
 		   this.alrm = alrm;
 		   this.sleepTime = sleepTime;
 	   }
 	   public void run()
        {
 		   Lib.debug(dbgThread, "@@@ " + name +" says: it is now " + Machine.timer().getTime() +
 				   ", going to sleep for " + sleepTime);
 		   alrm.waitUntil(sleepTime);
 		   Lib.debug(dbgThread, "@@@ " + name + " says: woke up at: " + Machine.timer().getTime());

        }
 	   private String name;
 	   private Alarm alrm;
 	   private long sleepTime;
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
    	// # Q3
        Alarm alrm = new Alarm();
        KThread sleepy1 = new KThread (new Sleepy("sleepy1", alrm, 1200)).setName("sleepy1");
        KThread sleepy2 = new KThread (new Sleepy("sleepy2", alrm, 1300)).setName("sleepy2");
        KThread sleepy3 = new KThread (new Sleepy("sleepy3", alrm, 6600)).setName("sleepy3");
        KThread sleepy4 = new KThread (new Sleepy("sleepy4", alrm, 100)).setName("sleepy4");

        sleepy1.fork();
        sleepy2.fork();
        sleepy3.fork();
        sleepy4.fork();

        sleepy3.join();
    }

    // # Q3
    // This is a multimap of KThreads (a TreeMap of Longs to Lists)
    // We use a multimap because it is more efficient - since a multimap is
    // sorted we can stop iterating when we reach a value greater thn the current time
    // (look in timerInterrupt for more details)
    private Map sleepers;
    // Lock for protecting the multimap access
    Lock multimapProtector;
    private static final char dbgThread = 't';
}
