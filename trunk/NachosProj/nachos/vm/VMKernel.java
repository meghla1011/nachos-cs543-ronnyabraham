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
    
    /**
     * The Translation Look-aside Buffer (TLB)
     *  is represented as a global Inverted Page Table.
     *  
     *  The InvertedPageTable class is represented as follows:
     *  
     *  Hashtable <processId, Hashtable <virtualPageNumber, TranslationEntry>>
     *  Nested Hashtables are required to represent the 1 to many relationship
     *     between processId and the mapping to virtualpageNumbers to 
     *     TranslationEntries (as a process can have more than 1 page).
     *     The virtualPageNumber will also be contained within the TranslationEntry
     *  
     *  All methods within the InvertedPageTable class performs locking,
     *  so the methods are thread safe.
     */
    public class InvertedPageTable
    {

    	private InvertedPageTable() 
    	{
    		lock = new Lock();
    		invertedPageTable = new Hashtable<Integer, 
    		Hashtable <Integer, TranslationEntry>>
    		(Machine.processor().getTLBSize());
    	}
        /**
         * Gets the physical Page number given a process id and virtual page number 
         */
    	public int getPhysicalPageNumber(int processId, int virtualPageNumber)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		
    		int returnValue = -1;
    		TranslationEntry translationEntry = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		returnValue = translationEntry.ppn;
    		lock.release();
    		
    		return returnValue;	
    	}
    	
        /**
         * Adds an entry to the TLB 
         */
    	public void addToInvertedPageTable(int processId, int virtualPageNumber, int physicalPageNumber)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		TranslationEntry translationEntry = 
    			new TranslationEntry(virtualPageNumber, 
    					physicalPageNumber, true,false,false,false);
    		
    		Integer processIdInt = new Integer(processId);
    		Integer virtualPageNumberInt = new Integer(virtualPageNumber);
    		Hashtable<Integer, TranslationEntry> innerHashtable =
    			new Hashtable<Integer, TranslationEntry>();
    		innerHashtable.put(virtualPageNumberInt, translationEntry);
    		
    		invertedPageTable.put(processIdInt, innerHashtable);
    		
    		lock.release();
    		return;	
    	}
    	
        /**
         * Returns the translation entry, given a process Id and virtual page number 
         */
    	public TranslationEntry getTranslationEntry(int processId, int virtualPageNumber)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		
    		TranslationEntry returnValue = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		lock.release();
    		
    		return returnValue;	
    	}
    	
        /**
         * Adds an entry to the TLB 
         */
    	public void addToInvertedPageTable(int processId, TranslationEntry translationEntry)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		
    		Integer processIdInt = new Integer(processId);
    		int virtualPageNumber = translationEntry.vpn;
    		Integer virtualPageNumberInt = new Integer(virtualPageNumber);
    		Hashtable<Integer, TranslationEntry> innerHashtable =
    			new Hashtable<Integer, TranslationEntry>();
    		innerHashtable.put(virtualPageNumberInt, translationEntry);
    		
    		invertedPageTable.put(processIdInt, innerHashtable);
    		
    		lock.release();
    		return;	
    	}
        /**
         * Sets the dirty flag for the given process id and virtualPageNumber
         *  within an entry of the TLB 
         */
    	public void setDirty(int processId, int virtualPageNumber, boolean value)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}

    		TranslationEntry currentValue = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		currentValue.dirty = value;
    		lock.release();

    	}
        /**
         * Checks the dirty flag for the given process id and virtualPageNumber
         *  within an entry of the TLB 
         */    	
    	public boolean isDirty(int processId, int virtualPageNumber)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}

    		TranslationEntry currentValue = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		
    		boolean returnValue =  currentValue.dirty;
    		lock.release();
    		return returnValue;

    	}
    	
        /**
         * Sets the used flag for the given process id and virtualPageNumber
         *  within an entry of the TLB 
         */
    	public void setUsed(int processId, int virtualPageNumber, boolean value)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}

    		TranslationEntry currentValue = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		currentValue.used = value;
    		lock.release();
    	}
    	
        /**
         * Sets the valid flag for the given process id and virtualPageNumber
         *  within an entry of the TLB 
         */
    	public void setValid(int processId, int virtualPageNumber, boolean value)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}

    		TranslationEntry currentValue = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		currentValue.valid = value;
    		lock.release();
    	}
    	
        /**
         * Returns an enumeration of all the processIds within the TLB 
         */
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
    	
        /**
         * Returns all the virtualPageNumber to TranslationEntry for a given processId
         */
    	public Hashtable<Integer, TranslationEntry> getTranslationEntryMappingForProcessId(int processId)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		Integer processIdInteger = new Integer(processId);
    		Hashtable<Integer, TranslationEntry> returnValue = 
    			invertedPageTable.get(processIdInteger);

    		lock.release();
    		return returnValue;

    	}
    	
    	private Hashtable<Integer, Hashtable<Integer,TranslationEntry>> invertedPageTable;

    	private Lock lock;	
    }
}
