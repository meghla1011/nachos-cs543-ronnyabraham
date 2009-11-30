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
	   if(stt != ConnectionState.ESTABLISHED)
	   {
		   return -1;
	   }
	   
	   int bytesRcvd = 0;
	   while(bytesRcvd < length)
	   {
		   MailMessage msg = NetKernel.postOffice.receive(srcPort);
		   System.arraycopy(msg.contents, 3, buf, bytesRcvd, msg.contents.length - 3);
		   bytesRcvd += msg.contents.length - 3;
		   
	   }
	   
	   return bytesRcvd;
   }

   
   public int write(byte[] buf, int offset, int length)
   {
	   if(stt != ConnectionState.ESTABLISHED)
	   {
		   return -1;
	   }
	   int netPayload = MailMessage.maxContentsLength - 3;
	   int numMessages = calcNumMessages(length, MailMessage.maxContentsLength - 3);
	   int lastMsgLen = lastMsgLen(length, MailMessage.maxContentsLength - 3);
	   
	   int idx = 0;
	   int bytesSent = 0;
	   for(int i = 0; i < numMessages - 1; ++i)
	   {
		   byte[] contents = new byte[netPayload];
		   contents[0] = MailMessage.SND;
		   System.arraycopy(new Short(sendMsgId++), 0, contents, 1, 2);
		   System.arraycopy(buf, idx, contents, 3, netPayload);
		   idx += netPayload;
		   MailMessage msg = null;
		   try
		   {
			   msg = new MailMessage(destId, destPort, srcId, srcPort, contents);
		   }
		   catch (MalformedPacketException e)
		   {
			e.printStackTrace();
		   }
		   NetKernel.postOffice.send(msg);
		   bytesSent += netPayload;
	   }
	   if(lastMsgLen > 0)
	   {
		// last msg
		   byte[] contents = new byte[lastMsgLen];
		   contents[0] = MailMessage.SND;
		   System.arraycopy(new Short(sendMsgId++), 0, contents, 1, 2);
		   System.arraycopy(buf, idx, contents, 3, lastMsgLen);
		   MailMessage msg = null;
		   try
		   {
			   msg = new MailMessage(destId, destPort, srcId, srcPort, contents);
		   }
		   catch (MalformedPacketException e)
		   {
			e.printStackTrace();
		   }
		   NetKernel.postOffice.send(msg);
		   bytesSent += lastMsgLen;
	   }
	   
	   
	   return bytesSent;
   }
   
   int calcNumMessages(int contentLen, int netPayload)
   {
	   return (contentLen / netPayload) + 1;
   }
   
   int lastMsgLen(int contentLen, int netPayLoad)
   {
	   return contentLen % netPayLoad;
   }
	    
	 
	 
	 
	 public int srcId, srcPort, destId, destPort;
	 private short sendMsgId = 0;
	 private short rcvMsgId = 0;
	 
	 ConnectionState stt;
	 public static enum ConnectionState {SYN_SENT, SYN_RCVD, ESTABLISHED, STP_RCVD, STP_SENT, CLOSING, CLOSED}
	 
	 
	 
}
