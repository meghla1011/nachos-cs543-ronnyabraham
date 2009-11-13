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
    	
    	VMKernel.ipt.swapF.deleteFile();
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
    		invertedPageTable = new Hashtable<Integer, 
    		Hashtable <Integer, TranslationEntry>>
    		(Machine.processor().getNumPhysPages() - Machine.processor().getTLBSize());
    		swapF = new SwappingFile();
    	}
        /**
         * Gets the physical Page number given a process id and virtual page number 
         */
    	public int getPhysicalPageNumber(int processId, int virtualPageNumber)
    	{
  //  		Lib.debug(dbgVM, "Looking for processId: " + processId + " VPN: "+virtualPageNumber);
    		
    		
    		int returnValue = -1;

    		Integer processIdInteger = new Integer(processId);
    		Integer vpnInteger = new Integer(virtualPageNumber);
    		
    		Hashtable<Integer,TranslationEntry> innerTable = 
    			invertedPageTable.get(processIdInteger);
    		
    		TranslationEntry translationEntry = 
    			innerTable.get(vpnInteger);
    		
    		
    		returnValue = translationEntry.ppn;
    		
    		
    		return returnValue;	
    	}
    	
        /**
         * Adds an entry to the TLB 
         */
    	public TranslationEntry addToInvertedPageTable(int processId, int virtualPageNumber, int physicalPageNumber)
    	{
  //  		Lib.debug(dbgVM, "addToInvertedPageTable: PID "+ processId + " VPN "+ virtualPageNumber + " PPN " + physicalPageNumber);
    		
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
    		
    		return translationEntry;	
    	}
    	
    	public void cleanupProcessEntries(int pid)
    	{
    		invertedPageTable.remove(pid);
    	}
    	
        /**
         * Returns the translation entry, given a process Id and virtual page number 
         */
    	public TranslationEntry getTranslationEntry(int processId, int virtualPageNumber)
    	{
    		
   		
    		TranslationEntry returnValue = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		
    		
    		return returnValue;	
    	}
    	
        /**
         * Removes the translation entry, given a process Id and virtual page number
         * and returns that entry 
         */
    	public TranslationEntry removeTranslationEntry(int processId, int virtualPageNumber)
    	{
    		
    		Integer processIdInteger = new Integer(processId);
    		Integer vpnInteger = new Integer(virtualPageNumber);
    		
    		Hashtable<Integer,TranslationEntry> innerTable = 
    			invertedPageTable.get(processIdInteger);
    		
    		TranslationEntry translationEntry = 
    			innerTable.get(vpnInteger);
    		
    		
    		
    		return translationEntry;	
    	}
    	
    	public int iptSize(int processId)
    	{
    		Hashtable<Integer,TranslationEntry> innerTable = 
    			invertedPageTable.get(processId);
    		
    		return innerTable.size();
    	}
    	
    	
        /**
         * Adds an entry to the TLB 
         */
    	public void addToInvertedPageTable(int processId, TranslationEntry translationEntry)
    	{
    		
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
    		
    		
    		return;	
    	}
        /**
         * Sets the dirty flag for the given process id and virtualPageNumber
         *  within an entry of the TLB 
         */
    	public void setDirty(int processId, int virtualPageNumber, boolean value)
    	{
    		

    		TranslationEntry currentValue = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		currentValue.dirty = value;
    		

    	}
        /**
         * Checks the dirty flag for the given process id and virtualPageNumber
         *  within an entry of the TLB 
         */    	
    	public boolean isDirty(int processId, int virtualPageNumber)
    	{
    		

    		TranslationEntry currentValue = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		
    		boolean returnValue =  currentValue.dirty;
    		
    		return returnValue;

    	}
    	
        /**
         * Sets the used flag for the given process id and virtualPageNumber
         *  within an entry of the TLB 
         */
    	public void setUsed(int processId, int virtualPageNumber, boolean value)
    	{
    		

    		TranslationEntry currentValue = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		currentValue.used = value;
    		
    	}
    	
        /**
         * Sets the valid flag for the given process id and virtualPageNumber
         *  within an entry of the TLB 
         */
    	public void setValid(int processId, int virtualPageNumber, boolean value)
    	{
    		

    		TranslationEntry currentValue = 
    			invertedPageTable.get(processId).get(virtualPageNumber);
    		currentValue.valid = value;
    		
    	}
    	
        /**
         * Returns an enumeration of all the processIds within the TLB 
         */
    	public Enumeration<Integer> getKeys()
    	{
    		

    		Enumeration<Integer> returnValue = invertedPageTable.keys();
    		
    		return returnValue;

    	}
    	
        /**
         * Returns all the virtualPageNumber to TranslationEntry for a given processId
         */
    	public Hashtable<Integer, TranslationEntry> getTranslationEntryMappingForProcessId(int processId)
    	{
    		
    		Integer processIdInteger = new Integer(processId);
    		Hashtable<Integer, TranslationEntry> returnValue = 
    			invertedPageTable.get(processIdInteger);

    		
    		return returnValue;

    	}
    	
    	public TranslationEntry handlePageFault(int pid, int vpn, int numProcessPages, Coff cof, int[] vpn2Coff, int[] vpn2Offset)
    	{    		    	
    		//Find the old page using clock algorithm
    		TranslationEntry toBeSwapped = runClockAlgorithm(pid);
    		
    		if(toBeSwapped != null)
    		{
    			swapF.writePage(pid, toBeSwapped.vpn, toBeSwapped);
				// Take old page and remove it from page table
    			removeTranslationEntry(pid, toBeSwapped.vpn);
    		}
    		
    		TranslationEntry newTE = swapF.readPage(pid, vpn, toBeSwapped.ppn);

			// It wasn't in swap file, then we create it
			if ( newTE == null )
			{
				newTE = addToInvertedPageTable(pid,vpn,toBeSwapped.ppn);
				// being here means this is the physical page was not found in the tlb, inverted table, or swap file
				// if this is the case, we need to load the relevant section of the coff to the new page 
				
				if (vpn >= 0 && vpn < numProcessPages)
				{
					CoffSection sec = cof.getSection(vpn2Coff[vpn]);
					sec.loadPage(vpn2Offset[vpn], toBeSwapped.ppn);
					newTE.readOnly = sec.isReadOnly();
				} 
				else
				{
					// clear all the memory
					byte[] allMem = Machine.processor().getMemory();
					for(int i = Processor.makeAddress(toBeSwapped.ppn, 0); 
						i < (Processor.makeAddress(toBeSwapped.ppn, 0) + Processor.pageSize);
						++i)
					{
						allMem[i] = 0;
					}
				}
				
			}
			return newTE;
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
    				te.used = false;
    			}
    	    }
    		//should not reach here 
    	    return null;
    	}
    	
    	
    	
    	private Hashtable<Integer, Hashtable<Integer,TranslationEntry>> invertedPageTable;


    	
    	public SwappingFile swapF;
    }
}
