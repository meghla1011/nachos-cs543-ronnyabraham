package nachos.vm;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.userprog.UserKernel.MemoryManager;
import nachos.userprog.UserKernel.Page;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
	super();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
	super.initialize(args);
    }

    /**
     * Initializes the global memory.
     */  
    protected void initializeGlobalMemory()
    {
    	tlb = new InvertedPageTable();
//		System.out.println("In VMKernel, initializeGlobalMemory.  IT WORKS!!!");
    }
    
    
    /**
     * Test this kernel.
     */	
    public void selfTest() {
	super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
	super.run();//TODO:change
    }
    
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';
    
    public static InvertedPageTable tlb;
    
    public class InvertedPageTable
    {

    	private InvertedPageTable() 
    	{
    		lock = new Lock();
    		invertedPageTable = new Hashtable<Integer, TranslationEntry>(Machine.processor().getTLBSize());
    	}

    	public int getPhysicalPageNumber(int processId, int virtualPageNumber)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		
    		int returnValue = -1;
    		TranslationEntry translationEntry = invertedPageTable.get(processId);
    		if (translationEntry.vpn == virtualPageNumber)
    		{
    			returnValue = translationEntry.ppn;
    		}
    		lock.release();
    		
    		return returnValue;	
    	}
    	
    	public void addToInvertedPageTable(int processId, int virtualPageNumber, int physicalPageNumber)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		
    		invertedPageTable.put(new Integer(processId), 
    				new TranslationEntry(virtualPageNumber, physicalPageNumber, true,false,false,false));
    		
    		lock.release();
    		return;	
    	}
    	

    	public TranslationEntry getTranslationEntry(int processId)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		
    		TranslationEntry returnValue = invertedPageTable.get(processId);
    		lock.release();
    		
    		return returnValue;	
    	}
    	
    	public void addToInvertedPageTable(int processId, TranslationEntry translationEntry)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		
    		invertedPageTable.put(new Integer(processId), translationEntry);
    		
    		lock.release();
    		return;	
    	}
    	
    	public void setDirty(int processId, boolean value)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}

    		TranslationEntry currentValue = invertedPageTable.get(processId);
    		currentValue.dirty = value;
    		lock.release();

    	}
    	
    	public boolean isDirty(int processId)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}

    		TranslationEntry currentValue = invertedPageTable.get(processId);
    		boolean returnValue =  currentValue.dirty;
    		lock.release();
    		return returnValue;

    	}
    	
    	
    	public void setUsed(int processId, boolean value)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}

    		TranslationEntry currentValue = invertedPageTable.get(processId);
    		currentValue.used = value;
    		lock.release();
    	}
    	
    	public void setValid(int processId, boolean value)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}

    		TranslationEntry currentValue = invertedPageTable.get(processId);
    		currentValue.valid = value;
    		lock.release();
    	}
    	
    	public Enumeration<Integer> getKeys()
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}

    		Enumeration<Integer> returnValue = invertedPageTable.keys();
    		lock.release();
    		return returnValue;

    	}
    	
    	private Hashtable<Integer, TranslationEntry> invertedPageTable;

    	private Lock lock;	
    }
}
