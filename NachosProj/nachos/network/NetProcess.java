package nachos.network;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>VMProcess</tt> that supports networking syscalls.
 */
public class NetProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public NetProcess() {
    	
    	super();
    	
    	for(int i = 0; i < fileDescriptors.length; ++i)
    	{
    		fileDescriptors[i] = null;
    	}
    	
    }

    private int handleConnect()
    {
    	Lib.debug(dbgProcess, "Still need to implement handleConnect");
    	
    	//TODO:  will need to return a file descriptor for this connection
    	
    	return 0;
    }
    
    private int handleAccept()
    {
    	Lib.debug(dbgProcess, "Still need to implement handleAccept");
    	
    	//TODO:  will need to return a file descriptor for this connection
    	
    	return 0;   	
    }
    
    /**
     * Handle the read() system call.
     * The function prototype is; 
     * int read(int fd, char *buffer, int size);
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
    	
    	// TODO: get our file descriptor from fileDescriptor array.
    	// May need a manager class to be the go between guy between
    	// using openFile and the network stuff (PostOffice?).
    	
    	OpenFile fd = fileDescriptors[a0];
    	
    	if( fd == null )
    	{
			Lib.debug(dbgProcess, "UserProcess::handleRead: file descriptor " + a0 + " is null");
    		return -1;
    	}
    	
    	//TODO:  How do we read from the file descriptor
    	// Do we want to just need to make a call to setup something special
    	// for the network send and receive and then call our handleRead from
    	// UserProcess.
    	
        int bytesRead = super.handleRead(a0, a1, a2);
    	return bytesRead;    	
    }
    
    /**
     * Handle the write() system call. 
     * int  write(int fd, char *buffer, int size);
     */
    protected int handleWrite(int a0,int a1, int a2)
    {
    	Lib.debug(dbgProcess, "handleWrite trying to write to file descriptor " + a0);
    	
    	// verify the file descriptor id is legal
    	if ( a0 < 0 || a0 > 17 )
    	{
			Lib.debug(dbgProcess, "UserProcess::handleWrite: illegal file descriptor");
    		return -1;
    	}
    	
    	// TODO: get our file descriptor from fileDescriptor array.
    	// May need a manager class to be the go between guy between
    	// using openFile and the network stuff (PostOffice?).
    	
    	OpenFile fd = fileDescriptors[a0];
    	
    	if( fd == null )
    	{
			Lib.debug(dbgProcess, "UserProcess::handleWrite: file descriptor " + a0 + " is null");
    		return -1;
    	}
    	
    	//TODO:  How do we write to the file descriptor
    	// Solution will be similiar to how we handle read calls
    	
        int bytesWritten = super.handleWrite(a0, a1, a2);
    	return bytesWritten;  
    } 
    
    
    private static final int
	syscallConnect = 11,
	syscallAccept = 12;
    
    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>11</td><td><tt>int  connect(int host, int port);</tt></td></tr>
     * <tr><td>12</td><td><tt>int  accept(int port);</tt></td></tr>
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
	case syscallConnect:
	    return handleConnect();
	case syscallAccept:
	    return handleAccept();
	case syscallRead:
	    return handleRead(a0,a1,a2);
	case syscallWrite:
	    return handleWrite(a0,a1,a2);
	default:
	    return super.handleSyscall(syscall, a0, a1, a2, a3);
	}
    }
    
    private static final char dbgProcess = 'n';
    private OpenFile[] fileDescriptors = new OpenFile[18];
    
}
