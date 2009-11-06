package nachos.vm;

import java.util.Enumeration;

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
    	VMKernel.tlb.setUsed(processId, true);
	}
	
    /**
     * Sets any required write flags  
     *
     * @param	the virtual page number.
     */
    @Override
	protected void setWriteFlags(int vpn)
	{
    	VMKernel.tlb.setDirty(processId, true);
    	VMKernel.tlb.setUsed(processId, true);
	}
    
    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
//	super.saveState();//may need to implement this
//	Enumeration<Integer> e = VMKernel.tlb.getKeys();
//	while (e.hasMoreElements())
//	{
//		Integer integerValue = e.nextElement();
//		int intValue = integerValue.intValue();
//		TranslationEntry translationEntry =  VMKernel.tlb.getTranslationEntry(intValue);
//		if (translationEntry.dirty)
//		{
//			//TODO:writeToDisk(intValue, translationEntry)
//		}
//		VMKernel.tlb.setValid(intValue, false);
//	}
    	if ( VMKernel.tlb.isDirty(processId))
    	{
    		//TODO:writeToDisk(intValue, translationEntry)
    	}
    	VMKernel.tlb.setValid(processId, false);
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
//	super.restoreState();
//	TODO: readFromDisk(intValue, translationEntry)
    	
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
	return super.loadSections();//TODO: change
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	super.unloadSections();
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
