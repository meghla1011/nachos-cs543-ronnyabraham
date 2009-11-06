package nachos.userprog;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args)
    {
    	super.initialize(args);

    	console = new SynchConsole(Machine.console());
    	
    	initializeGlobalMemory();

    	Machine.processor().setExceptionHandler(new Runnable() {
    		public void run() { exceptionHandler(); }
    	});
    }
    
    /**
     * Initializes the global memory.
     */  
    protected void initializeGlobalMemory()
    {
    	int numPhysPages = Machine.processor().getNumPhysPages();
    	
    	//Create a new memory Manager with the desired number of pages
    	memoryManager = new MemoryManager(numPhysPages);
		System.out.println("In UserKernel, initializeGlobalMemory.");
    }
    
    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	/*
	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');
  
	//System.out.println("");
	 
	 */
	
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	System.out.println("shell program name is "+shellProgram);
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;

    public static MemoryManager memoryManager;
     
    /*This page represents the Page, the value is the page number*/
    public class Page
    {
    	/*The constructor requires the page number*/
    	public Page(int aValue) 
    	{
    		value = aValue;
    	}
    	/*Set Value sets the page number, it requires an int*/
    	public void setValue(int aValue)
    	{
    		value = aValue;
    	}

    	/*Get Value gets the page number, it returns an int*/
    	public int getValue()
    	{
    		return value;
    	}
     	
    	//The value is the page number
    	private int value;
    }

    /*The Memory Manger class manages the List of Pages,
    the Linked List supports the ability to use non-consectuative pages.
    The methods are initialize, get pages, and free pages.*/
    public class MemoryManager
    {
    	/*The constructor requires the number of pages,
    	 *  or the inital length of the linkedlist*/
    	private MemoryManager(int aLength) 
    	{
    		lock = new Lock();
    		initialize(aLength);
    	}
    	/*Initalizes the linked list of the memory manager*/
    	private void initialize(int length)
    	{
    		memoryManager = new LinkedList<Page>();
        	
        	for (int i=0; i<length; i++)
        	{
        		memoryManager.add(new Page (i));
        	}
        	
    	}
    	/*Returns a linkedlist with the length of pages as virtual memory.  
    	 * Performs locking to ensure only one proecss accesses the memory manager at a time*/
    	public LinkedList<Page> getPages(int pagesWanted) throws NoSuchElementException
    	{
    		LinkedList<Page> returnList = null;
    		if (pagesWanted <= memoryManager.size())
    		{

    			if (! lock.isHeldByCurrentThread())
    			{
    				lock.acquire();
    			}
    			returnList = new LinkedList<Page>();

    			for (int i=0; i< pagesWanted; i++)
    			{
    				returnList.add(memoryManager.removeFirst());    			
    			}

    			lock.release();
    		}
    		return returnList;	
    	}
    	
    	/*Frees the virtual memory and adds it to the overall memory available.  
    	 * Performs locking to ensure only one proecss accesses the memory manager at a time*/
    	public void freePages(LinkedList<Page> usedPages)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		for (int i = usedPages.size(); i>0; i--)
    		{
    			memoryManager.add(usedPages.removeFirst());
    		}
    		lock.release();
    		return;	
    	}
    	/*The linked list of Pages that represents the available system memory*/
    	private LinkedList<Page> memoryManager;
    	/*Lock provides a way to ensure only one process is accessing the memory manager*/
    	private Lock lock;	
    }
    
}
