package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
	public UserProcess()
	{
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i=0; i<numPhysPages; i++)
		{
			pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
		}
		fileDescriptorOpenFileManager = new FileDescriptorOpenFileManager();
	}
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
	public static UserProcess newUserProcess()
	{
		return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
	}

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(memory, vaddr, data, offset, amount);

	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(data, offset, memory, vaddr, amount);

	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
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

		// for now, just assume virtual addresses=physical addresses
		section.loadPage(i, vpn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
    
	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

    /**
     * Handle the creat() system call. 
     */
    private int handleCreate()
    {
    	boolean createFileIfDoesNotExist = true;
    	int returnValue = 0;

    	Processor processor = Machine.processor();
    	int vaddr = processor.readRegister(4);

    	byte[] data = new byte[256];
    	int fileSize = readVirtualMemory(vaddr,data);
    	String filename = readVirtualMemoryString(vaddr, fileSize);

    	FileSystem fileSystem = ThreadedKernel.fileSystem;

    	//Lib.assertTrue(fileSystem == null, "UserProcess::handleCreate: No FileSystem");
    	if (fileSystem == null)
    	{
			String errorMsg = "UserProcess::handleCreate: No FileSystem, returning -1";
			Lib.debug(dbgProcess, errorMsg);
    		returnValue = -1;
    	}
    	else
    	{
    		OpenFile openedFile = 
    			fileSystem.open(filename, createFileIfDoesNotExist);
    		
    		if (openedFile == null)
    		{
    			String errorMsg = "UserProcess::handleCreate: File could not be opened, returning -1";
    			Lib.debug(dbgProcess, errorMsg);
        		returnValue = -1;
    		}
    		else
    		{
    			try
    			{
    				returnValue = fileDescriptorOpenFileManager.insertFileDescriptorMapping(filename, openedFile);
        		
    				String outputMsg = "handleCreate created file: " + filename + " with file descriptor: " 
    				+ returnValue;
        			Lib.debug(dbgProcess, outputMsg);
        			
    			}
    			catch(EmptyStackException e)
    			{
    				String errorMsg = e.toString() + "(returning -1)";
    				Lib.debug(dbgProcess, errorMsg);
    				returnValue = -1;
    			}
    			finally
    			{
    			}
    		}
    	}
    	
    	return returnValue;
    }

    /**
     * Handle the open() system call. 
     */
    private int handleOpen()
    {
    	boolean createFileIfDoesNotExist = false;
    	int returnValue = 0;

    	Processor processor = Machine.processor();
    	int vaddr = processor.readRegister(4);
    	//System.out.printf("address read = %d\n", vaddr);

    	byte[] data = new byte[256];
    	int fileSize = readVirtualMemory(vaddr,data);
    	String filename = readVirtualMemoryString(vaddr, fileSize);

    	FileSystem fileSystem = ThreadedKernel.fileSystem;

    	//Lib.assertTrue(fileSystem == null, "UserProcess::handleOpen: No FileSystem");

    	if (fileSystem == null)
    	{
    		String errorMsg = "UserProcess::handleOpen: No FileSystem, returning -1";
			Lib.debug(dbgProcess, errorMsg);
    		returnValue = -1;
    	}
    	else
    	{
    		//Try to read it without creating
    		OpenFile openedFile = 
    			fileSystem.open(filename, createFileIfDoesNotExist);
    		
    		if (openedFile == null)
    		{
    			//The file does not exist, so create it
    			createFileIfDoesNotExist = true;
    			openedFile = 
        			fileSystem.open(filename, createFileIfDoesNotExist);
    		}
    		if (openedFile == null)
    		{
    			//the file could not be created.
    			String errorMsg = "UserProcess::handleOpen: File could not be opened, returning -1";
    			Lib.debug(dbgProcess, errorMsg);
        		returnValue = -1;
    		}
    		else
    		{
    			try
    			{
    				returnValue = fileDescriptorOpenFileManager.insertFileDescriptorMapping(filename,openedFile);
        		
    				String outputMsg = "handleOpen opened file: " + filename + " with file descriptor: " 
    				+ returnValue;
        			Lib.debug(dbgProcess, outputMsg);
        			
    			}
    			catch(EmptyStackException e)
    			{
    				String errorMsg = e.toString() + "(returning -1)";
    				Lib.debug(dbgProcess, errorMsg);
    				returnValue = -1;
    			}
    			finally
    			{
    			}
    		}
    	}

    	return returnValue;
    }
    
    /**
     * Handle the read() system call. 
     */
    private int handleRead()
    {
    	Processor processor = Machine.processor();
    	int fileDescriptor = processor.readRegister(4);
    	int bufferAddress = processor.readRegister(5);
    	int bytesToRead= processor.readRegister(6);    	
    	int returnValue = 0;

    	byte[] buffer = new byte[bytesToRead];
    	int transferedBytes = readVirtualMemory(bufferAddress,buffer);
    	if (transferedBytes != bytesToRead)
    	{
    		//I think there are cases when this is okay, and cases where it is an error, see description in syscall
    		//if this shouldn't happen, the try below should be in an else
    	}

    	try
    	{
    		OpenFile openedFile = fileDescriptorOpenFileManager.getOpenFile(fileDescriptor);

    		if (openedFile == null)
    		{
    			String errorMsg = "UserProcess::handleRead: File could not be opened, returning -1";
    			Lib.debug(dbgProcess, errorMsg);
    			returnValue = -1;
    		}
    		else
    		{
    			int offset =  fileDescriptorOpenFileManager.getPositionIndex(fileDescriptor);
    			int numberOfBytesRead = openedFile.read(buffer, offset, bytesToRead);
    			if (numberOfBytesRead == -1)
    			{
    	    		String errorMsg = "handleRead error:  Number of bytes read = -1, returning -1";
    				Lib.debug(dbgProcess, errorMsg);
    				returnValue = -1;
    			}
    			else if (numberOfBytesRead != bytesToRead)
    			{
//    				if (Stream)
//    				returnValue = numberOfBytesRead
//    				or
//    				else {
    				String errorMsg = "handleRead error:  Number of bytes read not equal to bytes requested";
    				Lib.debug(dbgProcess, errorMsg);
    				returnValue = -1;
    			}
    			else
    			{   				
    				StringBuffer outputMsg = new StringBuffer("handleRead read " + numberOfBytesRead + 
    						" bytes from file descriptor: " + fileDescriptor + ": ");
    				for (int i=0; i<buffer.length; i++)
    				{
    					char c = (char)buffer[i];
    					outputMsg.append(c );
    				}
    				fileDescriptorOpenFileManager.incrementPositionIndex(fileDescriptor, numberOfBytesRead);
    				
    				returnValue = numberOfBytesRead;
    				Lib.debug(dbgProcess, outputMsg.toString());
    			}
    		}
    	}
    	catch(Exception e)
    	{
    		String errorMsg = e.toString() + "(returning -1)";
			Lib.debug(dbgProcess, errorMsg);
    		returnValue = -1;
    	}
    	finally
    	{
    	}
    	return returnValue;
    }
    
    /**
     * Handle the write() system call. 
     */
    private int handleWrite()
    {
    	Processor processor = Machine.processor();
    	int fileDescriptor = processor.readRegister(4);
    	int bufferAddress = processor.readRegister(5);
    	int bytesToWrite= processor.readRegister(6);    	
    	int returnValue = 0;

    	byte[] buffer = new byte[bytesToWrite];
    	int transferedBytes = readVirtualMemory(bufferAddress,buffer);
    	
    	if (transferedBytes != bytesToWrite)
    	{
    		//I think there are cases when this is okay, and cases where it is an error, see description in syscall
    		//if this shouldn't happen, the try below should be in an else
    	}

    	try
    	{
    		OpenFile openedFile = fileDescriptorOpenFileManager.getOpenFile(fileDescriptor);

    		if (openedFile == null)
    		{
    			String errorMsg = "UserProcess::handleWrite: File could not be opened, returning -1";
    			Lib.debug(dbgProcess, errorMsg);
    			returnValue = -1;
    		}
    		else
    		{
    			int offset =  fileDescriptorOpenFileManager.getPositionIndex(fileDescriptor);
    			int numberOfBytesWrote = openedFile.write(buffer, offset, bytesToWrite);
    			if (numberOfBytesWrote == -1)
    			{
    	    		String errorMsg = "handleWrite error:  Number of bytes written = -1, returning -1";
    				Lib.debug(dbgProcess, errorMsg);
    				returnValue = -1;
    			}
    			else if (numberOfBytesWrote < bytesToWrite)
    			{
    				String errorMsg = "handleWrite error:  Number of bytes written not equal to bytes requested";
    				Lib.debug(dbgProcess, errorMsg);
    				returnValue = -1;
    			}
    			else
    			{   				
    				StringBuffer outputMsg = new StringBuffer("handleWritten done writting " +
    						numberOfBytesWrote + " bytes from file descriptor: " + fileDescriptor + ": ");
    				for (int i=0; i<buffer.length; i++)
    				{
    					char c = (char)buffer[i];
    					outputMsg.append(c );
    				}
    				fileDescriptorOpenFileManager.incrementPositionIndex(fileDescriptor, numberOfBytesWrote);
    				
    				returnValue = numberOfBytesWrote;
    				Lib.debug(dbgProcess, outputMsg.toString());
    			}
    		}
    	}
    	catch(Exception e)
    	{
    		String errorMsg = e.toString() + "(returning -1)";
			Lib.debug(dbgProcess, errorMsg);
    		returnValue = -1;
    	}
    	finally
    	{
    	}
    	
    	return returnValue;   	
    } 

    /**
     * Handle the close() system call. 
     */
    private int handleClose()
    {
    	int returnValue = 0;

    	Processor processor = Machine.processor();
    	int fileDescriptor = processor.readRegister(4);

    	try
    	{
    		OpenFile openedFile = fileDescriptorOpenFileManager.getOpenFile(fileDescriptor);
    		if (openedFile == null)
    		{
    			Lib.debug(dbgProcess, "UserProcess::handleClose: File not opened");
    			returnValue = -1;
    		}
    		else
    		{
    			//look if this file is used somewhere else write being done?
    			
    			openedFile.close();
    			
    			//this close releases any resources associated with the file
    			
        		fileDescriptorOpenFileManager.removeFileDescriptorMapping(fileDescriptor);

        		String outputMsg = "handleClose closed file with file descriptor: " 
				+ fileDescriptor;
    			Lib.debug(dbgProcess, outputMsg);
    		}
    	}
    	catch(Exception e)
    	{
			Lib.debug(dbgProcess, e.toString());
    		returnValue = -1;
    	}
    	finally
    	{
    	}
    	
		String returnMsg = "handleClose returning (0=success, -1=error): " + returnValue;
		Lib.debug(dbgProcess, returnMsg);
    	return returnValue;
    } 
    
    /**
     * Handle the unlink() system call. 
     */
    private int handleUnlink()
    {
    	int returnValue = 0;

    	Processor processor = Machine.processor();
    	int vaddr = processor.readRegister(4);

    	byte[] data = new byte[256];
    	int fileSize = readVirtualMemory(vaddr,data);
    	String filename = readVirtualMemoryString(vaddr, fileSize);

    	FileSystem fileSystem = ThreadedKernel.fileSystem;

    	//Lib.assertTrue(fileSystem == null, "UserProcess::handleCreate: No FileSystem");
    	if (fileSystem == null)
    	{
			String errorMsg = "UserProcess::handleUnlink: No FileSystem, returning -1";
			Lib.debug(dbgProcess, errorMsg);
    		returnValue = -1;
    	}
    	else
    	{   		
    		try
    		{
    			int fileDescriptor = -1;
    			
    			//retrieve the file descriptor from the filename
    			try
    			{
    				//check if the filename is mapped to a file description
    				fileDescriptor = fileDescriptorOpenFileManager.getFileDescriptor(filename);
    			}
    	    	catch(NullPointerException e)
    	    	{
    	    		String errorMsg = e.toString() + ", file not found in descriptor map, deleting file anyway";
    				Lib.debug(dbgProcess, errorMsg);
    	    	}
    	    	
    	    	//delete the file
    	    	fileSystem.remove(filename);

    	    	//delete the file from the descriptor mapping if it is mapped
    	    	if (fileDescriptor >= 0)
    	    	{
    	    		//unlink the file from the fileDescriptor class
    	    		fileDescriptorOpenFileManager.removeFileDescriptorMapping(fileDescriptor);
    	    	}
    		}
    		catch(Exception e)
    		{
    			Lib.debug(dbgProcess, e.toString());
    			returnValue = -1;
    		}
    		finally
    		{
    		}
    	}
    	
		String returnMsg = "handleUnlink returning (0=success, -1=error): " + returnValue;
		Lib.debug(dbgProcess, returnMsg);
    	return returnValue;
    }
    
    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallCreate:
	    return handleCreate();
	case syscallOpen:
	    return handleOpen();
	case syscallRead:
	    return handleRead();
	case syscallWrite:
	    return handleWrite();
	case syscallClose:
	    return handleClose();
	case syscallUnlink:
	    return handleUnlink();
	case syscallExec:
		System.out.println("system call syscallExec");
		//todo will uncomment this line after I get this working Balaji
		//return handleExec(a0,a1,a2);
		return -1;
	case syscallJoin:
		return handleJoin();
	case syscallExit:
		return handleExit();

	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }
    
    //This function implements the handleExec system call 
    
    private int handleExec(int a0,int a1,int a2)
    {
        int returnValue = -1;
        // name of the coff file to execute
		String file = readVirtualMemoryString( a0, 256 );
        System.out.println("File name obtained "+ file);
		// If filename does not end with coff throw an error
		if ( ! file.endsWith(".coff") )
			return returnValue;

		if ( a1 < 0 )
			return returnValue;

		// Grab the data for each of the arguments in a2
		String arguments [] = new String [a1];

		byte[] data = new byte [a1 * 4];
		readVirtualMemory(a2, data, 0, a1 * 4);

		for ( int i = 0; i < a1; i++ ) 
			arguments [i] = readVirtualMemoryString( Lib.bytesToInt( data, i * 4, 4 ), 256 );

		// Create the new process
		UserProcess newChild = newUserProcess();

		// The child process that are launched by parent is added to a vector
		childprocessList.add(newChild.processId);

		// Return process Id of new child process
		if ( newChild.execute(file, arguments) )
			return newChild.processId;
		
        return returnValue;
    }
    
    //This function implements the handleJoin system call 
    
    private int handleJoin()
    {
        int returnValue = 0;
        //todo BR need to do the implementation 
        return returnValue;
    }
    
    //This function implements the handleExit system call 
    
    private int handleExit()
    {
        int returnValue = 0;
        //todo BR need to do the implementation 
        return returnValue;
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
    	case Processor.exceptionSyscall:
    		int result = handleSyscall(processor.readRegister(Processor.regV0),
    				processor.readRegister(Processor.regA0),
    				processor.readRegister(Processor.regA1),
    				processor.readRegister(Processor.regA2),
    				processor.readRegister(Processor.regA3)
    		);
    		processor.writeRegister(Processor.regV0, result);
    		processor.advancePC();
    		break;				       

    	default:
    		Lib.debug(dbgProcess, "Unexpected exception: " +
    				Processor.exceptionNames[cause]);
    		Lib.assertNotReached("Unexpected exception");
    	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
    
    private FileDescriptorOpenFileManager fileDescriptorOpenFileManager;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private Vector<Integer> childprocessList = new Vector<Integer>();
    private int processId =-1;
    private static class FileDescriptorOpenFileManager
    {
    	public FileDescriptorOpenFileManager() 
    	{
    		int standardInFileDescriptor = 0;
    		int standardOutFileDescriptor = 1;
    		fileDescriptorOpenFileClassList = 
    			new FileDescriptorOpenFileClass[listLength];
    		
    		indexStack = new Stack<Integer>();
    		
    		filenameMap = new HashMap<String,Integer>();
    		
    		//descriptors 0 and 1 are already used by standard input/output?
    		for (int i = listLength-1; i >=2; i--)
    		{
    			fileDescriptorOpenFileClassList[i] = null;
    			indexStack.push(new Integer(i));
    		}
    		OpenFile standardInFile = UserKernel.console.openForReading();
    		FileDescriptorOpenFileClass standardInClass = 
    			new FileDescriptorOpenFileClass("StandardInFile", standardInFile);

    		OpenFile standardOutFile = UserKernel.console.openForWriting();
    		FileDescriptorOpenFileClass standardOutClass = 
    			new FileDescriptorOpenFileClass("StandardOutFile", standardOutFile);
    		
    		fileDescriptorOpenFileClassList[standardInFileDescriptor] = standardInClass;
    		fileDescriptorOpenFileClassList[standardOutFileDescriptor] = standardOutClass;
    		
    	}

    	public OpenFile getOpenFile(int fileDescriptor)
    	{
    		FileDescriptorOpenFileClass fileDescriptorOpenFileClass = 
    			fileDescriptorOpenFileClassList[fileDescriptor];
    		
    		OpenFile openFile = fileDescriptorOpenFileClass.getOpenFile();
    		return openFile;
		}

		public String getFilename(int fileDescriptor)
    	{
    		FileDescriptorOpenFileClass fileDescriptorOpenFileClass = 
    			fileDescriptorOpenFileClassList[fileDescriptor];
    		
    		//String filename = fileDescriptorOpenFileClass.getFilename();
    		//return filename;
    		return fileDescriptorOpenFileClass.getFilename();
    	}
		
		public int getFileDescriptor(String filename)
		{
			return filenameMap.get(filename);
		}
		
		public int getPositionIndex(int fileDescriptor)
    	{
    		FileDescriptorOpenFileClass fileDescriptorOpenFileClass = 
    			fileDescriptorOpenFileClassList[fileDescriptor];
    		int positionIndex = 0;
    		
    		if (fileDescriptor != 1)
    		{
    			positionIndex = fileDescriptorOpenFileClass.getPositionIndex();
    		}
    		return positionIndex;
    	}
		
		public void incrementPositionIndex(int fileDescriptor, int offset)
    	{
    		FileDescriptorOpenFileClass fileDescriptorOpenFileClass = 
    			fileDescriptorOpenFileClassList[fileDescriptor];
    		
    		int currentPositionIndex = 
    			fileDescriptorOpenFileClass.getPositionIndex();
    		
    		fileDescriptorOpenFileClass.setPositionIndex(currentPositionIndex + offset);
    	}
    	
    	public int insertFileDescriptorMapping(String filename, OpenFile openedFile) throws EmptyStackException
    	{
    		FileDescriptorOpenFileClass fileDescriptorOpenFileClass = 
    			new FileDescriptorOpenFileClass(filename, openedFile);
    		
    		int index = indexStack.pop();
    		fileDescriptorOpenFileClassList[index] = 
    			fileDescriptorOpenFileClass;
    		
    		filenameMap.put(filename, index);
    		
    		return index;
    	}
    	
    	public void removeFileDescriptorMapping(int fileDescriptor) throws EmptyStackException, ArrayIndexOutOfBoundsException
    	{
    		String filename = getFilename(fileDescriptor);
    		filenameMap.remove(filename);
    		
    		fileDescriptorOpenFileClassList[fileDescriptor] = null;
    		indexStack.push(fileDescriptor);	
    	}
    	
    	private FileDescriptorOpenFileClass[] fileDescriptorOpenFileClassList;
    	private Stack<Integer> indexStack;
    	private HashMap<String,Integer> filenameMap;
    	private final int listLength = 18;
    	
    	private class FileDescriptorOpenFileClass
    	{
    		public FileDescriptorOpenFileClass(String aFilename, OpenFile aOpenFile)
    		{
    			filename = aFilename;
    			openedFile = aOpenFile;
    			positionIndex = 0;
    		}
    		public void setPositionIndex(int aPositionIndex)
    		{
    			positionIndex = aPositionIndex;
				
			}
			public FileDescriptorOpenFileClass(String aFilename, OpenFile aOpenFile, int aPositionIndex)
    		{
    			filename = aFilename;
    			openedFile = aOpenFile;
    			positionIndex = aPositionIndex;
    		}
    		public String getFilename()
    		{
    			return filename;
    		}
    		public OpenFile getOpenFile()
    		{
    			return openedFile;
    		}
    		public int getPositionIndex()
    		{
    			return positionIndex;
    		}
    		private String filename;
    		private OpenFile openedFile;
    		private int positionIndex;    		
    	}
    }
}
