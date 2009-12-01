package nachos.network;

import java.util.LinkedList;
import java.util.Vector;

import nachos.machine.MalformedPacketException;
import nachos.machine.OpenFile;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.SynchList;

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
			 startRcvLoopThread();
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
	   
	   qLock.acquire();
	   while(inMsgQ.size() > 0)
	   {
		   MailMessage msg = inMsgQ.removeFirst();
		   System.arraycopy(msg.contents, 3, buf, bytesRcvd, msg.contents.length - 3);
		   bytesRcvd += msg.contents.length - 3;
	   }
	   qLock.release();
	   
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
		   byte[] contents = new byte[netPayload + 3];
		   contents[0] = MailMessage.SND;
		   byte[] msgIdBuf = new byte[2];
		   msgIdBuf[0] = (byte)(sendMsgId >> 8);
		   msgIdBuf[0] = (byte)(sendMsgId & 0x00FF);
		   sendMsgId++;
		   System.arraycopy(msgIdBuf, 0, contents, 1, 2);
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
		   byte[] contents = new byte[lastMsgLen + 3];
		   contents[0] = MailMessage.SND;
		   byte[] msgIdBuf = new byte[2];
		   msgIdBuf[0] = (byte)(sendMsgId >> 8);
		   msgIdBuf[0] = (byte)(sendMsgId & 0x00FF);
		   sendMsgId++;
		   System.arraycopy(msgIdBuf, 0, contents, 1, 2);
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
   
   void startRcvLoopThread()
   {
	   if(rcvLoopThread == null)
	   {
		   rcvLoopThread = new KThread(new RcvLoop(srcPort, inMsgQ, qLock));
		   rcvLoopThread.setName("Channel rcv loop # " + channelId++);
		   rcvLoopThread.fork();
	   }
   }
   
   private static class RcvLoop implements Runnable
   {
	   RcvLoop(int port, LinkedList<MailMessage> q, Lock lk)
       {
		   this.port = port;
		   this.q = q;
		   this.lk = lk;
       }

       public void run()
       {
    	   lk.acquire();
    	   MailMessage msg = NetKernel.postOffice.receive(port);
		   q.add(msg);
		   lk.release();
       }
       private int port;
       private LinkedList<MailMessage> q;
       private Lock lk;
   }
   
	 
	 
	 public int srcId, srcPort, destId, destPort;
	 private short sendMsgId = 0;
	 private short rcvMsgId = 0;
	 
	 KThread rcvLoopThread = null;
	 LinkedList<MailMessage> inMsgQ = new LinkedList<MailMessage>();
	 Lock qLock = new Lock();
	 public static int channelId = 0;
	 
	 ConnectionState stt;
	 public static enum ConnectionState {SYN_SENT, SYN_RCVD, ESTABLISHED, STP_RCVD, STP_SENT, CLOSING, CLOSED}
}
