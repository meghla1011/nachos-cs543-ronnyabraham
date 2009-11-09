package nachos.vm;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.text.html.HTMLDocument.Iterator;

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
    	int numPhysPages = Machine.processor().getNumPhysPages();
    	
    	//Create a new memory Manager with the desired number of pages
    	memoryManager = new MemoryManager(numPhysPages);
		
    	ipt = new InvertedPageTable();
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
    	super.run();
//kikyfv
//    	UserProcess process = UserProcess.newUserProcess();
//    	
//    	String shellProgram = Machine.getShellProgramName();	
//    	System.out.println("shell program name is "+shellProgram);
//    	Lib.assertTrue(process.execute(shellProgram, new String[] { }));
//
//    	KThread.currentThread().finish();
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
    
    public static InvertedPageTable ipt;
    
    /**
     * The global Inverted Page Table.
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
    		(Machine.processor().getNumPhysPages());
    		swapF = new SwapFile();
    	}
        /**
         * Gets the physical Page number given a process id and virtual page number 
         */
    	public int getPhysicalPageNumber(int processId, int virtualPageNumber)
    	{
  //  		Lib.debug(dbgVM, "Looking for processId: " + processId + " VPN: "+virtualPageNumber);
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		
    		int returnValue = -1;

    		Integer processIdInteger = new Integer(processId);
    		Integer vpnInteger = new Integer(virtualPageNumber);
    		
    		Hashtable<Integer,TranslationEntry> innerTable = 
    			invertedPageTable.get(processIdInteger);
    		
    		TranslationEntry translationEntry = 
    			innerTable.get(vpnInteger);
    		
    		
    		returnValue = translationEntry.ppn;
    		lock.release();
    		
    		return returnValue;	
    	}
    	
        /**
         * Adds an entry to the TLB 
         */
    	public void addToInvertedPageTable(int processId, int virtualPageNumber, int physicalPageNumber)
    	{
  //  		Lib.debug(dbgVM, "addToInvertedPageTable: PID "+ processId + " VPN "+ virtualPageNumber + " PPN " + physicalPageNumber);
    		
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
        		invertedPageTable.get(processIdInt);
    		if (innerHashtable == null)
    		{
    			innerHashtable = new Hashtable<Integer, TranslationEntry>();
        		innerHashtable.put(virtualPageNumberInt, translationEntry);
        		invertedPageTable.put(processId, innerHashtable);//may/may not be needed
    		}
    		else
    		{
    			innerHashtable.put(virtualPageNumberInt, translationEntry);
    		}
    		
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
         * Removes the translation entry, given a process Id and virtual page number
         * and returns that entry 
         */
    	public TranslationEntry removeTranslationEntry(int processId, int virtualPageNumber)
    	{
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		

    		Integer processIdInteger = new Integer(processId);
    		Integer vpnInteger = new Integer(virtualPageNumber);
    		
    		Hashtable<Integer,TranslationEntry> innerTable = 
    			invertedPageTable.get(processIdInteger);
    		
    		TranslationEntry translationEntry = 
    			innerTable.get(vpnInteger);
    		
    		lock.release();
    		
    		return translationEntry;	
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
        		invertedPageTable.get(processIdInt);
    		
    		if (innerHashtable == null)
    		{
    			innerHashtable = new Hashtable<Integer, TranslationEntry>();
        		innerHashtable.put(virtualPageNumberInt, translationEntry);
        		invertedPageTable.put(processId, innerHashtable);//may/may not be needed
    		}
    		else
    		{
    			innerHashtable.put(virtualPageNumberInt, translationEntry);
    		}
    		
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
    	
    	public TranslationEntry handlePageFault(int processId, int virtualPageNum)
    	{
    		
    		TranslationEntry iptTranslationEntry = getTranslationEntry(processId, virtualPageNum);
    		if(iptTranslationEntry != null)
    			return iptTranslationEntry;
    	
    		//now we will have to handle the condition of page fault
    		if (! lock.isHeldByCurrentThread())
    		{
    			lock.acquire();
    		}
    		
    		//Find the oldpage using clock algorithm
    		TranslationEntry toBeSwapped = runClockAlgorithm(processId);
    		
    		boolean oldStatus = Machine.interrupt().setStatus(false);
    		
    		TranslationEntry newTLB = new TranslationEntry();
		    newTLB.valid = false;
		    //remove toBeSwapped tlb from processor TLB list
			for (int i = 0; i < Machine.processor().getTLBSize(); i++)
			{
				TranslationEntry temp2 = Machine.processor().readTLBEntry(i);
				if (temp2.vpn == toBeSwapped.vpn)
				{
					if (temp2.dirty)
					{	
						addToInvertedPageTable(processId,temp2);
					}
                    //This will clear the victim tlb
					Machine.processor().writeTLBEntry(i, newTLB);
				}
			}
			Machine.interrupt().setStatus(oldStatus);
    		if(toBeSwapped != null)
    		{
    			swapF.writeToFile(processId, toBeSwapped.vpn, toBeSwapped);
				
				// Take old page and remove it from page table
    			removeTranslationEntry(processId, toBeSwapped.vpn);
    		}
    		
    		
    		TranslationEntry newTE = swapF.readFromFile(processId, virtualPageNum, toBeSwapped.ppn);

			// It wasn't in swap file, then we create it
			if ( newTE == null )
			{
				addToInvertedPageTable(processId,virtualPageNum,toBeSwapped.ppn);
			}
			iptTranslationEntry = getTranslationEntry(processId, virtualPageNum);
    		
    		return iptTranslationEntry;
    	}
    	
    	//In the clock algorithm we use the used bit as the reference bit
    	//if the translation entry is used than we give it a second chance
    	//the unused translation entry becomes the victim for swapping
    	private TranslationEntry runClockAlgorithm(int processId)
    	{
    		Hashtable<Integer,TranslationEntry> listOfPages = invertedPageTable.get(processId);
    		Enumeration<Integer> set = listOfPages.keys();

    		for ( ; set.hasMoreElements() ;) {
    			Integer key = set.nextElement();
    			TranslationEntry te = listOfPages.get(key);
    			if(te.used == false)
      	    	  return te;
    			else
    			{    				
    			    //second chance algorithm resets the used flag, so next time te will become
    				//the victim to be swapped with the page in the swap file system. 
    				te.used = true;
    			}
    	    }
    		//should not reach here 
    	    return null;
    	}
    	
    	
    	
    	private Hashtable<Integer, Hashtable<Integer,TranslationEntry>> invertedPageTable;

    	private Lock lock;	
    	
    	private SwapFile swapF;
    }
}
