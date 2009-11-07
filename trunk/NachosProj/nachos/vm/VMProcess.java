package nachos.vm;

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
    }

    /**
     * Initializes Memory as required for a VM Process
     */
    @Override
	protected void initializeMemory()
	{
		//no memory initialization required for tlb
	}
	
    /**
     * Destroys the memory when exiting from a VM Process
     */
    @Override
	protected void destoryMemory()
	{
		//no memory destruction required for tlb
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
    	VMKernel.tlb.setUsed(processId, vpn, true);
	}
	
    /**
     * Sets any required write flags  
     *
     * @param	the virtual page number.
     */
    @Override
	protected void setWriteFlags(int vpn)
	{
    	VMKernel.tlb.setDirty(processId, vpn, true);
    	VMKernel.tlb.setUsed(processId, vpn, true);
	}
    
    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    	
    	Hashtable<Integer, TranslationEntry> translationEntryMapping =
    		VMKernel.tlb.getTranslationEntryMappingForProcessId(processId);
    	
    	Enumeration<Integer> e =translationEntryMapping.keys();
    	
    	while (e.hasMoreElements())
    	{
    		Integer integerValue = e.nextElement();
    		int virtualPageNumber = integerValue.intValue();
    		if (VMKernel.tlb.isDirty(processId, virtualPageNumber))
    		{
    			//TODO:writeToDisk(intValue, translationEntry)
    		}
    		VMKernel.tlb.setValid(processId, virtualPageNumber, false);
    	}
    	
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
//	TODO: readFromDisk(intValue, translationEntry)
    	Lib.assertTrue(false, "Group 10 - What needs to be done on restoreState");
    	
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
 	if (numPages > Machine.processor().getNumPhysPages() ) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;

		int physicalAddress = VMKernel.tlb.getPhysicalPageNumber(processId, vpn);
		
		// map virtual addresses to physical addresses
		section.loadPage(i, physicalAddress);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	/*call saveState, which write everything to disk,
    	 *  thus everything is unloaded*/
    	saveState();
    }    

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
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
    	int vaddress = Machine.processor().readRegister(Machine.processor().regBadVAddr);
    	
    	//TODO:part 2 of assignment, maybe?  Possibly read value from disk
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
    	int realPageNumber =  VMKernel.tlb.getPhysicalPageNumber(processId, virtualPageNumber);
    	if (realPageNumber == -1)
    	{
    		int vaddress = (virtualPageNumber * Processor.pageSize) + offset;
    		Lib.debug(dbgProcess, "\t\tTLB miss");
    		Machine.processor().writeRegister(Machine.processor().regBadVAddr, vaddress);
    		handleException(Processor.exceptionTLBMiss);
    	}
    	return realPageNumber;
    	
    }
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
}
