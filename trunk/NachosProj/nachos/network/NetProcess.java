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

    private int handleConnect(int id, int portNum)
    {	
    	int descriptorAvail = getFirstAvailableFd();
    	
    	if(descriptorAvail != -1 )
    	{
    		OpenFile newConn = null;
			try
			{
				newConn = NetKernel.postOffice.handleConnect(id, portNum);
			} 
			catch (MalformedPacketException e) 
			{
				e.printStackTrace();
			}

			if(newConn == null)
			{
				System.err.println("Failed to get available file descriptor");
				return -1;
			}
			// Put it in the file array, give it a file descriptor (from User Process)
			fileDescriptors[descriptorAvail] = newConn;
			System.out.println("Successfully established connection "+ ((Channel)newConn).srcId 
					            + " " + ((Channel)newConn).srcPort + " " + ((Channel)newConn).destId 
					            + " "+ ((Channel)newConn).destPort);
    	}
    
    	return descriptorAvail;
    }
    
    private int handleAccept(int port)
    {
    	// Get available descriptor number
    	int descriptorAvail = getFirstAvailableFd();
		
		if ( descriptorAvail != -1 )
		{
			// Open a file for connection
			OpenFile acceptConn = null;
			try
			{
				acceptConn = NetKernel.postOffice.handleAccept(port);
			} catch (MalformedPacketException e)
			{
				e.printStackTrace();
			}
			if (acceptConn == null)
			{
				System.err.println("Failed to get available file descriptor");
				return -1;
			}
			// Put it in the file array, give it a file descriptor (from User Process)
			fileDescriptors[descriptorAvail] = acceptConn;
			System.out.println("Successfully accepted connection "+ ((Channel)acceptConn).srcId 
		            + " " + ((Channel)acceptConn).srcPort + " " + ((Channel)acceptConn).destId 
		            + " "+ ((Channel)acceptConn).destPort);
		}
		return descriptorAvail;   	
    }
    
    /**
     * Handle the read() system call.
     * The function prototype is; 
     * int read(int fd, char *buffer, int size);
     * where the arguments are fetched from registers a0, a1, and a2 respectively
     */
    protected int handleRead(int a0,int a1, int a2)
    {
    	Lib.debug(dbgProcess, "NetProcess trying to read file descriptor " + a0);
    	// verify the file descriptor id is legal
    	if ( a0 < 0 || a0 > 17 )
    	{
			Lib.debug(dbgProcess, "NetProcess::handleRead: illegal file descriptor");
    		return -1;
    	}
    	
    	OpenFile fd = fileDescriptors[a0];
    	
    	if( fd == null )
    	{
			Lib.debug(dbgProcess, "NetProcess::handleRead: file descriptor " + a0 + " is null");
    		return -1;
    	}
    	
    	// read from network into a buffer
    	byte buf [] = new byte[a2];
    	int offset = 0;
    	Channel ch = (Channel)fd;
        int bytesRead = ch.read(buf, offset, a2);
        
        StringBuffer outputMsg = new StringBuffer("handleRead read " + a2 + 
				" bytes from file descriptor: " + a0 + ": ");
		for (int i=0; i<buf.length; i++)
		{
			char c = (char)buf[i];
			outputMsg.append(c );
		}
		Lib.debug(dbgProcess, outputMsg.toString());
		
		writeVirtualMemory(a1,buf,offset,bytesRead);
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
			Lib.debug(dbgProcess, "NetProcess::handleWrite: illegal file descriptor");
    		return -1;
    	}
    	
    	OpenFile fd = fileDescriptors[a0];
    	if( fd == null )
    	{
			Lib.debug(dbgProcess, "NetProcess::handleWrite: file descriptor " + a0 + " is null");
    		return -1;
    	}

    	byte buf [] = new byte[a2];
    	int offset = 0;
        int bytesRead = readVirtualMemory(a1,buf);
        if(bytesRead != a2 )
        {
			Lib.debug(dbgProcess, "NetProcess::handleWrite: virual memory read less bytes than excepted " +bytesRead);
        	return -1;
        }
        // write to the network
        Channel ch = (Channel)fd;
        int bytesWritten = ch.write(buf,offset,a2);
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
	    return handleConnect(a0,a1);
	case syscallAccept:
	    return handleAccept(a0);
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
