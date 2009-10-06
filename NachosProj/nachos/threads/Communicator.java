package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator
{
    /**
     * Allocate a new communicator.
     */
    public Communicator()
    {
    	lock = new Lock();
    	condition = new Condition(lock);
    	message = 0;
    	flag = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word)
    {
		Lib.debug(debug, "Beginning of speak method");

		if (!flag)
		{
			flag = true;
			Lib.debug(debug, "Speak ready, waiting for listener");

			lock.acquire();
			Lib.debug(debug, "speaker has acquired the lock and is going to sleep");
			condition.sleep();

			Lib.debug(debug, "Listener ready, speaker is continuing and going to send the word");
			message = word;
		}
		else
		{
			Lib.debug(debug, "Listener ready, speaker is sending word");
			message = word;
		}

		Lib.debug(debug, "Word sent, speaker attempting to notify the listener");

		lock.acquire();
		condition.wake();
		lock.release();

		Lib.debug(debug, "Speaker FINISHED");
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen()
    {
		Lib.debug(debug, "Beginning of listen method");

		if (flag)
		{
			Lib.debug(debug, "Listener ready, proceeding to try to listen to message from speak");
			lock.acquire();
			condition.wake();

			Lib.debug(debug, "listener has called to wake speaker, now going to sleep");
			lock.acquire();
			condition.sleep();

			Lib.debug(debug, "listener has waken up after speaker finished");
			//Listener should have now received the word from speaker
		}
		else
		{
			flag = true;
			Lib.debug(debug, "Listener ready, waiting for speaker");
			lock.acquire();
			condition.sleep();
		}

		flag = false;
		Lib.debug(debug, "Listener received word and is FINISHED");
		return message;
	}


    private static class CommunicatorTest implements Runnable {
	CommunicatorTest(Communicator communicator, int type) {
	    this.communicator = communicator;
	    this.type = type;
	}

	public void run()
	{
		int receivedNumber = 0;

		if (1 == type) //speaker
		{		
			//communicator.speak(1);
			//communicator.speak(2);
			communicator.speak(3);
			//communicator.speak(4);
			//communicator.speak(5);


		}
		else //listener
		{
			receivedNumber = communicator.listen();
			System.out.printf("1st listen call returned %d\n", receivedNumber);

			//communicator.listen();
			//System.out.printf("1st listen call returned %d\n", receivedNumber);

			//communicator.listen();
			//System.out.printf("1st listen call returned %d\n", receivedNumber);

			//communicator.listen();
			//System.out.printf("1st listen call returned %d\n", receivedNumber);

			//communicator.listen();
			//System.out.printf("1st listen call returned %d\n", receivedNumber);
		}
	}

	private Communicator communicator;
	private int type;
    }

    /**
     * Test if this module is working.
     */
    public static void selfTest() {
    	Communicator commObject = new Communicator();

	//Seems to work:
	// 1) Listener ready before speaker, speaker comes up and sends and listener receives without issue

	//Known Problems:
	// 1) If speaker is ready before listener, threads crash, not quite sure yet what is going on
	// 2) Sparatic crashes on initialization between threads (probably from coming up and calling the lock)
	//    Only seems to happen in this speaker ready without listener scenario.
	//    Should we come up and call that lock is not held before calling?
	// 3) Can't run back to back on anything.  Even runing listener ready before speaker back to back
	//    fails.  I think we need some sort of logic to make sure that after the speaker sends, he
	//    waits until listener is finished before starting over (sleep until receive wakes and tells him
	//    he finished?)


	//Test Works - listener before speaker works
	new KThread(new CommunicatorTest(commObject, 2)).setName("listener").fork();
	new KThread(new CommunicatorTest(commObject, 1)).setName("speaker").fork();

	//BROKEN - This case not working yet
	//new KThread(new CommunicatorTest(commObject, 1)).setName("speaker").fork();
	//new KThread(new CommunicatorTest(commObject, 2)).setName("listener").fork();


    }

    private Lock lock;
    private static final char debug = 't';
    int message;
    Condition condition;
    boolean flag;

}
