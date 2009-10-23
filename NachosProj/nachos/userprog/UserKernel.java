package nachos.userprog;

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

    	int numPhysPages = Machine.processor().getNumPhysPages();
    	pageList = new PageList(numPhysPages);//TODO: not quite right    

    	console = new SynchConsole(Machine.console());

    	Machine.processor().setExceptionHandler(new Runnable() {
    		public void run() { exceptionHandler(); }
    	});
    }
    
    public static PageList getPageList()
    {
    	return pageList;
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
    

    private static PageList pageList;
    
    public class Page
    {
    	public Page(Page nextPage, int aValue) 
    	{
    		value = aValue;
    		next = nextPage;
    		previous = null;
    		
    		nextPage.previous = this;
    	}
    	
    	public Page(Page nextPage) 
    	{
    		next = nextPage;
    		nextPage.previous = this;
    	}
    	
    	public Page() 
    	{
    		next = null;
    		previous = null;
    	}
    	
    	public void setValue(int aValue)
    	{
    		value = aValue;
    	}
    	
    	public int getValue()
    	{
    		return value;
    	}
    	public void setNext(Page aPage)
    	{
    		next = aPage;
    		aPage.previous = this;
    	}
    	
    	public Page getNext()
    	{
    		return next;
    	}
    	
       	public Page getPrevious()
    	{
    		return next;
    	}
    	
    	private int value;
    	private Page next;
    	private Page previous;
    	
    }

    public class PageList
    {
    	public PageList(int aLength) 
    	{
    		lock = new Lock();
    		length = aLength;
    		root = new Page();
    		root.setValue(0);
    		firstFreePage = root;
    		initialize();
    	}
    	public void initialize()
    	{
    		Page currentPage = root;
    		for (int i =1; i<length; i++)
    		{
    			Page nextPage = new Page();
    			currentPage.setNext(nextPage);
    			currentPage = nextPage;
    		}
    		currentPage = null;
    	}
    	
    	public Page getPages(int pagesWanted)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		
    		Page currentPage = firstFreePage;
    		int index = 0;
    		while ((index < pagesWanted) && (currentPage !=null))
    		{
    			currentPage = currentPage.getNext();
    			index++;
    		}
    		if (index != pagesWanted)
    		{
    			//could not get all the pages that were desired
    			//maybe an error, not sure
    		}

    		Page returnPage = firstFreePage;
    		firstFreePage = currentPage;
    		currentPage.previous.next = null;

			lock.release();
    		return returnPage;	
    	}
    	
    	public void freePages(Page usedPage)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}


    		Page currentPage = firstFreePage;

    		if (currentPage == null)
    		{
    			//all pages are used, so the first free page is the used page
    			firstFreePage = usedPage;//maybe firstFreePage.next?
    		}
    		else
    		{
    			while (currentPage.next !=null)
    			{
    				currentPage = currentPage.getNext();
    			}
    			currentPage.next = usedPage;
    			usedPage.previous = currentPage;
    		}
    		lock.release();
    		return;	
    	}
    	
    	
    	private int length;
    	private Page root;
    	private Page firstFreePage;
    	private Lock lock;
    	
    }
    
}
