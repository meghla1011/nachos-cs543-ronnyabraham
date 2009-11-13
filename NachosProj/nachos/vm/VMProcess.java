package nachos.vm;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.userprog.UserKernel.MemoryManager;
import nachos.userprog.UserKernel.Page;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
	super();
	tlbIndexCounter = 0;
	tlbInitialized = false;
	tempFrameNumber = 0;
	tlbImage = new int[Machine.processor().getTLBSize()];
	
	for (int i=0; i <tlbImage.length; i++)
	{
		tlbImage[i] = -1;
	}
	
    }

    /**
     * Initializes Memory as required for a VM Process
     */
    @Override
	protected void initializeMemory()
	{
   		int numPages = 15;
		MemoryManager memoryManager = UserKernel.memoryManager; 
		
		if (memoryManager == null)
		{
			Lib.debug(dbgProcess, "memory manager is null");
		}
			
		virtualMemory = memoryManager.getPages(numPages);
		
		int ppn = 0;
		Page currentPage;
		for (int i=0; i<numPages; i++)
		{
			currentPage = virtualMemory.get(i);
			ppn = currentPage.getValue();
//			Lib.debug(dbgVM, "PID: "+ processId + " VPN: " + i + " PPN: " + ppn);
			VMKernel.ipt.addToInvertedPageTable(processId, i,ppn);
		}
	}
	
    /**
     * Destroys the memory when exiting from a VM Process
     */
    @Override
	protected void destoryMemory()
	{
		super.destoryMemory();
	}
	
    /**
     * Returns whether an address is valid  
     *
     * @return	the validity of the address.
     */
    @Override
	protected boolean isValidMemoryAddress(int possibleAddress)
	{
		 return true;
	}
    
    /**
     * Sets any required read flags  
     *
     * @param	the virtual page number.
     */
    @Override
	protected void setReadFlags(int vpn)
	{
    	Processor processor = Machine.processor();
	    for (int i=0; (i<processor.getTLBSize()); i++) {
	    	TranslationEntry translationEntry = processor.readTLBEntry(i);
	    	if (translationEntry.valid && translationEntry.vpn == vpn)
	    	{
	    		translationEntry.used = true;
	    	}
	    }
	}
	
    /**
     * Sets any required write flags  
     *
     * @param	the virtual page number.
     */
    @Override
	protected void setWriteFlags(int vpn)
	{
    	Processor processor = Machine.processor();
	    for (int i=0; (i<processor.getTLBSize()); i++) {
	    	TranslationEntry translationEntry = processor.readTLBEntry(i);
	    	if (translationEntry.valid && translationEntry.vpn == vpn)
	    	{
	    		translationEntry.used = true;
	    		translationEntry.dirty = true;
	    		
	    	}
	    }
	}
    
    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    	
    	Processor processor = Machine.processor();
    	for (int i=0; (i<processor.getTLBSize()); i++) {
	    	TranslationEntry translationEntry = processor.readTLBEntry(i);
    		if (translationEntry.valid)
    		{
    			VMKernel.ipt.addToInvertedPageTable(
    					processId, translationEntry);
    			tlbImage[i] = translationEntry.vpn;
    		}
    		else
    		{
    			tlbImage[i] = -1;
    		}
	    }
    	
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
    	Processor processor = Machine.processor();
    	for (int i =0; i< processor.getTLBSize(); i++)
    	{
    		int virtualPageNumber = tlbImage[i];
    		if (virtualPageNumber == -1)
    		{
        		processor.writeTLBEntry(i, 
				new TranslationEntry(0, 0, false, false, false, false));
    		}
    		else
    		{
    			TranslationEntry translationEntry = 
    				VMKernel.ipt.removeTranslationEntry(processId, virtualPageNumber);
    			processor.writeTLBEntry(i, translationEntry);
    		}
    	}
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections()
    {
    	// calculate how many pages we will need for our coff section
    	int numSections = coff.getNumSections();
		for(int i = 0; i < numSections; ++i)
		{
			CoffSection sec = coff.getSection(i);
			int secLength = sec.getLength();
			requiredPagesForCoff += secLength;
		}
		// create our vpn to coff and offset arrays
		vpnToCoff = new int[requiredPagesForCoff];
		vpnToOffset = new int[requiredPagesForCoff];

		for(int j = 0; j < numSections; j++) 
		{
			CoffSection sec = coff.getSection(j);
			int vpn = sec.getFirstVPN();
			int secLength = sec.getLength();
			for(int k = 0; k < secLength; ++k)
			{
				vpn = sec.getFirstVPN() + k;
				vpnToCoff[vpn]		= j;
				vpnToOffset[vpn]	= k;
			}
		}
		return true;
    }

    public void lazyLoadPage(int virtualPageNumber)
    {
    	// load sections
    	for (int s=0; s<coff.getNumSections(); s++) 
    	{
    		CoffSection section = coff.getSection(s);

    		for (int i=0; i<section.getLength(); i++) 
    		{
    			int vpn = section.getFirstVPN()+i;

    			//find the vpn that we should load
    			if (vpn == virtualPageNumber )
    			{
    				int physicalAddress = 
    					VMKernel.ipt.getPhysicalPageNumber(processId, vpn);
    				section.loadPage(i, physicalAddress);
    			}
    		}
    	}
    }
    
    
    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {

    }    

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exception</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause)
    {
    	Processor processor = Machine.processor();
      	switch (cause) 
    	{
    		case Processor.exceptionTLBMiss:
    		{
    			handleTLBMissException();
    			break;
    		}
    		default:
    		{
    			super.handleException(cause);
    			break;
    		}
    	}
    }
    
    void handleTLBMissException()
    {
    	Processor processor = Machine.processor();
    	int vaddr = processor.readRegister(Machine.processor().regBadVAddr);
    	
    	int virtualPageNumber = vaddr / pageSize;
    	int realMemOffset = vaddr % pageSize;
    	
    	//Search for the TLB in the inverted page table. 
    	
    	TranslationEntry iptTranslationEntry = VMKernel.ipt.getTranslationEntry(processId, virtualPageNumber);
		if(iptTranslationEntry != null)
		{
			//write the translation entry to the TLB buffer
		    writeTranslationEntryToTLB(iptTranslationEntry,processor);
		    return;
		}
    	//The inverted table does not have tlb entry 
		int physicalPageSize = Machine.processor().getNumPhysPages();
		int tlbSize = processor.getTLBSize();
		//Inverted page table size is physical page size - tlbsize. 
		if( VMKernel.ipt.iptSize(processId) < (physicalPageSize - tlbSize) )
		{
			//Add a new page to the inverted page table. 
			//we will have to get a new page. 
			int newPpn = VMKernel.getFreePage();
			iptTranslationEntry = VMKernel.ipt.addToInvertedPageTable(processId,virtualPageNumber,newPpn);
			writeTranslationEntryToTLB(iptTranslationEntry,processor);
			return;
		}
		
		// handle page fault
    	 iptTranslationEntry = VMKernel.ipt.handlePageFault(processId,virtualPageNumber);
    	
		//lazyLoadPage(iptTranslationEntry.vpn);
		
		
    }
    
    public void writeTranslationEntryToTLB(TranslationEntry iptTranslationEntry,Processor processor)
    {
    	if (!tlbInitialized)
    	{
    		processor.writeTLBEntry(tlbIndexCounter, iptTranslationEntry);
    		if (tlbIndexCounter == processor.getTLBSize() -1)
    		{
    			tlbInitialized = true;
    		}
    	}
    	else
    	{
    		TranslationEntry tlbTranslationEntry = 
    			processor.readTLBEntry(tlbIndexCounter);
    		if (tlbTranslationEntry.valid)
    		{
    			VMKernel.ipt.addToInvertedPageTable(
    					processId, tlbTranslationEntry);
    		}
    		processor.writeTLBEntry(tlbIndexCounter, iptTranslationEntry);
    	}
		tlbIndexCounter++;
		tlbIndexCounter = tlbIndexCounter % processor.getTLBSize();
		tempFrameNumber = iptTranslationEntry.ppn;
    }
    
    /**
     * returns the Frame Number using the implementation
     *  required for a VM Process
     *
     * @return	the frame number associated with that page
     *  number.
     */
    @Override
    protected int returnFrameNumber(int virtualPageNumber, int offset)//kludge center
    {
//    	int realPageNumber =  VMKernel.tlb.getPhysicalPageNumber(processId, virtualPageNumber);
    	Processor processor = Machine.processor();
    	
    	int frameNumber = -1;
	    for (int i=0; (i<processor.getTLBSize()) && (frameNumber == -1); i++) {
	    	TranslationEntry translationEntry = processor.readTLBEntry(i);
	    	if (translationEntry.valid && translationEntry.vpn == virtualPageNumber)
	    	{
	    		frameNumber = translationEntry.ppn;
	    	}
	    }
	    if (frameNumber == -1)
    	{
    		int vaddress = (virtualPageNumber * Processor.pageSize) + offset;
    		Lib.debug(dbgProcess, "\t\tTLB miss");
    		Machine.processor().writeRegister(Machine.processor().regBadVAddr, vaddress);
    		handleException(Processor.exceptionTLBMiss);
    		frameNumber = tempFrameNumber;
    	}
    	return frameNumber;	
    } 
    public int requiredPagesForCoff;
    // we maintain 2 arrays keeping track of the mapping from vurtual pages to coff sections and offsets
    public int [] vpnToCoff;
    public int [] vpnToOffset;
    
    private int tempFrameNumber;
    private int tlbIndexCounter;
    private boolean tlbInitialized;
    private static final int pageSize = Processor.pageSize;
    private int[] tlbImage;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
    
}
