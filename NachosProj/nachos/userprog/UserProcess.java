package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
//import nachos.userprog.*;
import nachos.userprog.UserKernel.Page;
import nachos.userprog.UserKernel.MemoryManager;

import java.io.EOFException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
		Lib.debug(dbgProcess, "UserProcess::UserProcess Entered");
		
		// open stdin and stdout
		for(int i = 0; i < fileDescriptors.length; ++i)
		{
			fileDescriptors[i] = null;
		}
		
		fileDescriptors[0] = UserKernel.console.openForReading();
		if(fileDescriptors[0] == null)
		{
			String errorMsg = "UserProcess::UserProcess Failed to open file for reading";
			Lib.debug(dbgProcess, errorMsg);
		}
			
		fileDescriptors[1] = UserKernel.console.openForWriting();
		if(fileDescriptors[1]  == null)
		{
			String errorMsg = "UserProcess::UserProcess Failed to open file for writing";
			Lib.debug(dbgProcess, errorMsg);	
		}
		
		//Each process will be allocated 15 pages, since 8 did not seem to be sufficient
		int numPhysPages = 15;
		pageTable = new TranslationEntry[numPhysPages];
		MemoryManager memoryManager = UserKernel.memoryManager; 
		
		if (memoryManager == null)
		{
			Lib.debug(dbgProcess, "memory manager is null");
			//TODO:  Assert here?
		}
			
		virtualMemory = memoryManager.getPages(numPhysPages);
		
		if (virtualMemory == null)
		{
			//TODO:  call handleExit?
		}
		
		int ppn = 0;
		Page currentPage;
		for (int i=0; i<numPhysPages; i++)
		{
			currentPage = virtualMemory.get(i);
			ppn = currentPage.getValue();
			pageTable[i] = new TranslationEntry(i,ppn, true,false,false,false);
		}

		processId = nextProcessId;
		nextProcessId++;
		activeProcesses.put(processId, this);
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
	
	currThread =  new UThread(this);
	currThread.setName(name).fork();

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
    	
    Lib.debug(dbgProcess, "UserProcess.readVirtualMemory entered"); 
    	
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// performs virtual addresses to physical addresses mapping
	if (vaddr < 0 || vaddr >= virtualMemory.size() * pageSize)
	    return 0;
	
	int virtualPageNumber = vaddr / pageSize;
	int realMemOffset = vaddr % pageSize;
	int realPageNumber = pageTable[virtualPageNumber].ppn;
	
	int amount = Math.min(length, memory.length-vaddr);

	if (realPageNumber * pageSize + realMemOffset + amount > memory.length )
	{
		return 0;
	}

	int newOffset = offset;
	
	while (realMemOffset + amount > pageSize) 
	{
		int amountToWrite = pageSize - realMemOffset;
		System.arraycopy(memory, (realPageNumber*pageSize) + realMemOffset, data, newOffset, amountToWrite);
		amount = amount - amountToWrite;
		newOffset = newOffset + amountToWrite;
		
		virtualPageNumber++;
		realPageNumber = pageTable[virtualPageNumber].ppn;
		realMemOffset = 0;
	}
	
	//Lib.debug(dbgProcess, "copying " + amount + " bytes from memory position " + realPageNumber + realMemOffset + " to destination offset " + newOffset);
	
	//arraycopy parameters: (src, src_position, destination, destination_position, length)
	System.arraycopy(memory, (realPageNumber*pageSize) + realMemOffset, data, newOffset, amount);

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
    	
    Lib.debug(dbgProcess, "UserProcess.writeVirtualMemory entered");
    	
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	// performs virtual addresses to physical addresses mapping
	if (vaddr < 0 || vaddr >= virtualMemory.size() * pageSize)
	    return 0;
	int virtualPageNumber = vaddr / pageSize;
	int realMemOffset = vaddr % pageSize;
	int realPageNumber = pageTable[virtualPageNumber].ppn;
	
	int amount = Math.min(length, memory.length-vaddr);

	if (realPageNumber * pageSize + realMemOffset + amount > memory.length )
	{
		return 0;
	}

	int newOffset = offset;
	
	//while the amount to write is larger than a page
	while (realMemOffset + amount > pageSize) 
	{
		int amountToWrite = pageSize - realMemOffset;
		System.arraycopy(data, newOffset, memory, (realPageNumber*pageSize) + realMemOffset, amount);
		amount = amount - amountToWrite;
		newOffset = newOffset + amountToWrite;
		
		virtualPageNumber++;
		realPageNumber = pageTable[virtualPageNumber].ppn;
		realMemOffset = 0;
	}

	//write the remainder on to the last page
	System.arraycopy(data, newOffset, memory, (realPageNumber*pageSize) + realMemOffset, amount);

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
	
	//TODO:  Handle coff sections that are read only
	
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
 	if (numPages > virtualMemory.size()) {
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

		TranslationEntry translationEntry = pageTable[vpn];
		int physicalAddress = translationEntry.ppn;
		
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
    protected int handleCreate(int a0)
    {
    	Lib.debug(dbgProcess, "UserProcess::handleCreate Entered");
    	
    	//Get the file name
    	String filename = readVirtualMemoryString(a0, 256);
    	Lib.debug(dbgProcess, "handleCreate trying to create file: " + filename);
    	
    	if(filename == null)
    	{
			Lib.debug(dbgProcess, "UserProcess::handleCreate: read virtual memory string failed");
    		return -1;
    	}
    		
    	if( filename.length() > 256)
    	{
			Lib.debug(dbgProcess, "UserProcess::handleCreate FAILED: file name size too big");
    		return -1;
    	}
    	
    	if(deleteList.contains(filename))
    	{
			Lib.debug(dbgProcess, "UserProcess::handleCreate FAILED: cannot create file that is pending deletion");
    		return -1;
    	}
    	
    	int openSlot = getFirstAvailableFd();
    	if(openSlot == -1)
    	{
			Lib.debug(dbgProcess, "UserProcess::handleCreate FAILED: cannot create file all file descriptors are used.");
    		return -1;
    	}
    	else
		{
			OpenFile fd = UserKernel.fileSystem.open(filename, true);

			if ( fd == null )
			{
				Lib.debug(dbgProcess, "UserProcess::handleCreate FAILED: can't open file, fileSystem.open() returned null.");
	    		return -1;
			}
			
			fileDescriptors[openSlot] = fd;
			Lib.debug(dbgProcess, "handleCreate created file: " + filename + " with descriptor " + openSlot);
			return openSlot;
		}
    }
    
    protected int getFirstAvailableFd ()
	{
		// Find the first open slot
		for ( int i = 2; i < fileDescriptors.length; i++ )
		{
			if  ( fileDescriptors[i] == null )
			{
				Lib.debug(dbgProcess, "getFirstAvailableFd returning file descriptor " + i);
				return i;
			}	
		}
		return -1;
	}


    /**
     * Handle the open() system call. 
     */
    protected int handleOpen(int a0)
    {
    	Lib.debug(dbgProcess, "UserProcess::handleOpen Entered");
    	
    	//Get the file name
    	String filename = readVirtualMemoryString(a0, 256);
    	Lib.debug(dbgProcess, "handleOpen trying to open file: " + filename);
    	
    	if(filename == null)
    	{
			Lib.debug(dbgProcess, "UserProcess::handleOpen: read virtual memory string failed");
    		return -1;
    	}
    		
    	if( filename.length() > 256)
    	{
			Lib.debug(dbgProcess, "UserProcess::handleOpen: file name size too big");
    		return -1;
    	}
    	
    	if(deleteList.contains(filename))
    	{
			Lib.debug(dbgProcess, "UserProcess::handleOpen: cannot create file that is pending deletion");
    		return -1;
    	}
    	
    	int openSlot = getFirstAvailableFd();
    	if(openSlot == -1)
    	{
			Lib.debug(dbgProcess, "UserProcess::handleOpen: cannot create file all file descriptors are used.");
    		return -1;
    	}
    	else
		{
			OpenFile fd = UserKernel.fileSystem.open(filename, false);

			if ( fd == null )
			{
				Lib.debug(dbgProcess, "UserProcess::handleOpen: can't open file, fileSystem.open() returned null.");
	    		return -1;
			}
			
			fileDescriptors[openSlot] = fd;
			Lib.debug(dbgProcess, "handleOpen opened file: " + filename + " with descriptor " + openSlot);
			return openSlot;
		}
    }
    
    /**
     * Handle the read() system call.
     * The function prototype is; 
     * int  read(int fd, char *buffer, int size);
     * where the arguments are fetched from registers a0, a1, and a2 respectively
     */
    protected int handleRead(int a0,int a1, int a2 )
    {
    	Lib.debug(dbgProcess, "handleRead trying to read file descriptor " + a0);
    	
    	// verify the file descriptor id is legal
    	if ( a0 < 0 || a0 > 17 )
    	{
			Lib.debug(dbgProcess, "UserProcess::handleRead: illegal file descriptor");
    		return -1;
    	}
    	// get our file descriptor
    	OpenFile fd = fileDescriptors[a0];
    	
    	if( fd == null )
    	{
			Lib.debug(dbgProcess, "UserProcess::handleRead: file descriptor " + a0 + " is null");
    		return -1;
    	}
    	// read from file into a buffer
    	byte buf [] = new byte[a2];
    	int offset = 0;
        int bytesRead = fd.read(buf, offset, a2);
        
        StringBuffer outputMsg = new StringBuffer("handleRead read " + a2 + 
				" bytes from file descriptor: " + a0 + ": ");
		for (int i=0; i<buf.length; i++)
		{
			char c = (char)buf[i];
			outputMsg.append(c );
		}
		Lib.debug(dbgProcess, outputMsg.toString());
        
        // write the buffer to our vm
        int bytesWritten = writeVirtualMemory(a1,buf,offset,bytesRead);
    	return bytesWritten;    	
    }
    
    /**
     * Handle the write() system call. 
     * int  write(int fd, char *buffer, int size);
     */
    protected int handleWrite(int a0,int a1, int a2)
    {
    	Lib.debug(dbgProcess, "handleWrite trying to write " + a2 + " bytes to file descriptor " + a0);
    	
    	// verify the file descriptor id is legal
    	if ( a0 < 0 || a0 > 17 )
    	{
			Lib.debug(dbgProcess, "UserProcess::handleWrite: illegal file descriptor " + a0);
    		return -1;
    	}
    	// get our file descriptor
    	OpenFile fd = fileDescriptors[a0];
    	
    	if( fd == null )
    	{
			Lib.debug(dbgProcess, "UserProcess::handleWrite: file descriptor " + a0 + " is null");
    		return -1;
    	}
    	// read the vm
    	byte buf [] = new byte[a2];
    	int offset = 0;
        int bytesRead = readVirtualMemory(a1,buf);
        if(bytesRead != a2 )
        {
			Lib.debug(dbgProcess, "UserProcess::handleWrite: virual memory read less bytes than excepted " +bytesRead);
        	return -1;
        }
        // write to file
        int bytesWritten = fd.write(buf,offset,a2);
        if(bytesWritten != a2 )
        {
			Lib.debug(dbgProcess, "UserProcess::handleWrite: filedescriptor wrote less bytes than excepted " +bytesWritten);
        	return -1;
        }
        
        Lib.debug(dbgProcess, "handleWrite wrote " + bytesWritten + " bytes to file descriptor " + a0);
        
    	return bytesWritten;
    } 

    /**
     * Handle the close() system call. 
     */
    protected int handleClose(int a0)
    {
    	Lib.debug(dbgProcess, "handleClose trying to close file descriptor " + a0);
    	
    	// verify the file descriptor id is legal
    	if ( a0 < 0 || a0 > 17 )
    	{
			Lib.debug(dbgProcess, "UserProcess::handleClose FAILED: illegal file descriptor");
    		return -1;
    	}
    	// get our file descriptor
    	OpenFile fd = fileDescriptors[a0];
    	if(fd == null)
    	{
			Lib.debug(dbgProcess, "UserProcess::handleClose FAILED: file descriptor does not exist");
    		return -1;
    	}
    	String filename = fd.getName();
    	
    	// deallocate the fd from our descriptor array
    	fileDescriptors[a0] = null;
    	
    	// check whether this is a last close request of a file that was previously unlinked
    	boolean fdExists = false;
    	
    	for (int i = 2; i < fileDescriptors.length; i++ )
		{
    		if(fileDescriptors[i] != null)
    		{
    			OpenFile tmpFd = fileDescriptors[i];
    			String tmpFilename = tmpFd.getName();
    			if(filename.equals(tmpFilename))
    			{
    				fdExists = true;
    				break;
    			}
    		}
		}
    	if(!fdExists)
    	{
    		// this was the last reference to this file, check if it was marked for deletion
    		if(deleteList.contains(filename) == true)
    		{
    			// delete the file
    			UserKernel.fileSystem.remove(filename);
    			deleteList.remove(filename);
    		}
    	}
    	
    	Lib.debug(dbgProcess, "handleClose returned successfully");
    	return 0;
    } 
    
    /**
     * Handle the unlink() system call. 
     */
    protected int handleUnlink(int a0)
    {   	
    	//Get the file name
    	String filename = readVirtualMemoryString(a0, 256);
    	Lib.debug(dbgProcess, "handleUnlink trying to delete file: " + filename);
    	
    	if(filename == null)
    	{
			Lib.debug(dbgProcess, "UserProcess::handleUnlink FAILED: read virtual memory string failed");
    		return -1;
    	}
    		
    	if( filename.length() > 256)
    	{
			Lib.debug(dbgProcess, "UserProcess::handleUnlink FAILED: file name size too big");
    		return -1;
    	}
    	
    	// search our file descriptor array and find if there is any
    	// other file descriptor referring this file
    	boolean fdExists = false;
    	for (int i = 2; i < fileDescriptors.length; i++ )
		{
    		if(fileDescriptors[i] != null)
    		{
    			OpenFile tmpFd = fileDescriptors[i];
    			String tmpFilename = tmpFd.getName();
    			if(filename.equals(tmpFilename))
    			{
    				fdExists = true;
    				break;
    			}
    		}
		}
    	if(fdExists)
    	{
    		// there exists a handle to this file, defer the deletion
    		if(deleteList.contains(filename) == false)
    		{
    			deleteList.add(filename);
    		}
    	}
    	else
    	{
    		// delete the file
    		UserKernel.fileSystem.remove(filename);
    	}
    	
    	Lib.debug(dbgProcess, "handleUnlink returned successfully");
    	return 0;
    }
    
    protected static final int
    syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallConnect = 11,
	syscallAccept = 12;; 

	protected static final int syscallWrite = 7;

	protected static final int syscallClose = 8;

	static final int syscallUnlink = 9;

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
	    return handleCreate(a0);
	case syscallOpen:
	    return handleOpen(a0);
	case syscallRead:
	    return handleRead(a0,a1,a2);
	case syscallWrite:
	    return handleWrite(a0,a1,a2);
	case syscallClose:
	    return handleClose(a0);
	case syscallUnlink:
	    return handleUnlink(a0);
	case syscallExec:
		System.out.println("system call syscallExec");
		return handleExec(a0,a1,a2);
		//return -1;
	case syscallJoin:
		return handleJoin(a0,a1);
	case syscallExit:
		return handleExit(a0);

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
    
    private int handleJoin(int a0,int a1)
    {
    	System.out.println("Invoking handleJoin with child id "+a0);
    	System.out.println("process id of the parent "+processId);
        int returnValue = 1;
     
        //If we cannot find the child process return
		if ( ! childprocessList.contains(a0) )
			return -1;

		//check for the running status of the child process
		if ( ! activeProcesses.containsKey(a0) ) 
			return 0;

		
		UserProcess childprocess = activeProcesses.get(a0);
		

		if ( childprocess.status != this.statusFinished )
		{
			// shouldn't this be - 
			childprocess.currThread.join();
			//currThread.join();
		}
		else
		{
			byte [] data = Lib.bytesFromInt(0);
			writeVirtualMemory (a1, data);
			// write the status in memory
		}	
		
		
        return returnValue;
    }
    
    //This function implements the handleExit system call 
    
    private int handleExit(int a0)
    {
    	//BEGIN:This needs to be done if exiting cleanly, where does this call go if not a clean exit?
		MemoryManager memoryManager = UserKernel.memoryManager; 
		memoryManager.freePages(virtualMemory);
		//END:
		
        int returnValue = 0;
             
		for ( int i = 0; i < fileDescriptors.length; i++ )
		{
			if ( fileDescriptors[i] != null )
				handleClose(i);
		}
		

		// Next, unload the sections
		unloadSections(); 

		//remove the process from the list
		activeProcesses.remove(processId);

		// Assign  the status
		this.status = a0;

		
		if ( activeProcesses.size() == 0 )
		{
			//If this is the last process then call terminate
			UserKernel.kernel.terminate();
		}
		
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

    LinkedList<Page> virtualMemory;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
    private UThread currThread=null;
	public int status =-1;
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private Vector<Integer> childprocessList = new Vector<Integer>();
    private static Map <Integer, UserProcess> activeProcesses = new HashMap <Integer, UserProcess> ();
    private int processId =-1;
    private static int nextProcessId = 0;
    private static final int statusFinished = 4;
    private LinkedList <String> deleteList = new LinkedList<String> ();
    protected OpenFile[] fileDescriptors = new OpenFile[18];
    // fds for stdout and stdin
    private static final int fdStandardInput = 0;
    private static final int fdStandardOutput = 1;
    
}
