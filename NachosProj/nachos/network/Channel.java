package nachos.network;

import java.util.Vector;

import nachos.machine.MalformedPacketException;
import nachos.machine.OpenFile;

/*
 * class channel
 */

public class Channel extends OpenFile
{

	 
	 
	 public Channel(int srcId, int srcPort, int destId, int destPort)
	 {
	     this.srcId = srcId;
	     this.srcPort = srcPort;
	     this.destId = destId;
	     this.destPort = destPort;
	     stt = ConnectionState.CLOSED;
	 }
	 
	 public OpenFile connectToSrv() throws MalformedPacketException
	 {
		 if(stt != ConnectionState.CLOSED)
		 {
			 return null;
		 }
		 
		 byte[] content = new byte[1];
		 content[0] = MailMessage.SYN;
		 MailMessage msg = new MailMessage (destId, destPort, srcId, srcPort, content);
		 NetKernel.postOffice.send(msg);
		 stt = ConnectionState.SYN_SENT;
		 // at this point wait for the server to send a syn ack
		 MailMessage rspMsg = NetKernel.postOffice.receive(srcPort);
		 byte rspFlag = rspMsg.contents[0];
		 if(rspFlag ==  MailMessage.SYN_ACK)
		 {
			 stt = ConnectionState.ESTABLISHED;
			 return this;
		 }
		 else
		 {
			 return null;
		 }
	 }
	 
   public int read(byte[] buf, int offset, int length)
   {
		return -1;
   }

   
   public int write(byte[] buf, int offset, int length)
   {
		return -1;
   }
	    
	 
	 
	 
	 public int srcId, srcPort, destId, destPort;
	 
	 ConnectionState stt;
	 public static enum ConnectionState {SYN_SENT, SYN_RCVD, ESTABLISHED, STP_RCVD, STP_SENT, CLOSING, CLOSED}
	 
	 
	 
}
