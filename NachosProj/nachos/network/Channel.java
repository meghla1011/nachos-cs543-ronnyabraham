package nachos.network;

import java.util.LinkedList;
import java.util.Vector;

import nachos.machine.MalformedPacketException;
import nachos.machine.OpenFile;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.Semaphore;
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
	     state = ConnectionState.CLOSED;
	     slidingWnd = new SlidingWindow();
	 }
	 
	 public OpenFile connectToSrv() throws MalformedPacketException
	 {
		 if(state != ConnectionState.CLOSED)
		 {
			 return null;
		 }
		 
		 byte[] content = new byte[1];
		 content[0] = MailMessage.SYN;
		 MailMessage msg = new MailMessage (destId, destPort, srcId, srcPort, content);
		 NetKernel.postOffice.send(msg);
		 state = ConnectionState.SYN_SENT;
		 // at this point wait for the server to send a syn ack
		 MailMessage rspMsg = NetKernel.postOffice.receive(srcPort);
		 byte rspFlag = rspMsg.contents[0];
		 if(rspFlag ==  MailMessage.SYN_ACK)
		 {
			 state = ConnectionState.ESTABLISHED;
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
	   if(state != ConnectionState.ESTABLISHED)
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
	   if(state != ConnectionState.ESTABLISHED)
	   {
		   return -1;
	   }
	   int netPayload = DataMsg.maxPayload;
	   int numMessages = calcNumMessages(length, MailMessage.maxContentsLength - 3);
	   int lastMsgLen = lastMsgLen(length, MailMessage.maxContentsLength - 3);
	   
	   int idx = 0;
	   int bytesSent = 0;
	   for(int i = 0; i < numMessages - 1; ++i)
	   {
		   byte[] contents = new byte[netPayload];
		   System.arraycopy(buf, idx, contents, 0, netPayload);
		   idx += netPayload;
		   MailMessage msg = null;
		   try
		   {
			   DataMsg dm = new DataMsg(destId, destPort, srcId, srcPort, contents, sendMsgId);
			   sendMsgId++;
			   msg = dm.mailMsg();
			   
		   }
		   catch (MalformedPacketException e)
		   {
			e.printStackTrace();
		   }
		   // check to see whether the sliding window is full, if it is,
		   // wait for the receive loop to wake me up, the window is protected with
		   // the same lock used to synchronize the user thread and the recv loop thread
		   qLock.acquire();
		   if(slidingWnd.isWindowFull())
		   {
			   qLock.release();
			   sema.P();
		   }
		   else
		   {
			   slidingWnd.sentMsgIdList.add(new Short((short)(sendMsgId - 1)));
			   qLock.release();
		   }
		   
		   // send a data message
		   NetKernel.postOffice.send(msg);
		   bytesSent += netPayload;
	   }
	   if(lastMsgLen > 0)
	   {
		// last msg
		   byte[] contents = new byte[lastMsgLen];
		   System.arraycopy(buf, idx, contents, 0, lastMsgLen);
		   MailMessage msg = null;
		   try
		   {
			   DataMsg dm = new DataMsg(destId, destPort, srcId, srcPort, contents, sendMsgId);
			   sendMsgId++;
			   msg = dm.mailMsg();
		   }
		   catch (MalformedPacketException e)
		   {
			e.printStackTrace();
		   }
		   qLock.acquire();
		   if(slidingWnd.isWindowFull())
		   {
			   qLock.release();
			   sema.P();
		   }
		   else
		   {
			   slidingWnd.sentMsgIdList.add(new Short((short)(sendMsgId - 1)));
			   qLock.release();
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
		   rcvLoopThread = new KThread(new RcvLoop(srcPort, inMsgQ, qLock, sema, slidingWnd));
		   rcvLoopThread.setName("Channel rcv loop # " + channelId++);
		   rcvLoopThread.fork();
	   }
   }
   
   private static class RcvLoop implements Runnable
   {
	   RcvLoop(int port, LinkedList<MailMessage> q, Lock lk, Semaphore s, SlidingWindow w)
       {
		   this.port = port;
		   this.q = q;
		   this.lk = lk;
		   this.windowClearSema = s;
		   this.wnd = w;
       }

       public void run()
       {
    	   while(true)
    	   {
    		   lk.acquire();
        	   MailMessage msg = NetKernel.postOffice.receive(port);
        	   if(AckMsg.isAck(msg))
        	   {
        		   // update the sliding window
        		   updateSlidingWindow(msg);
        	   }
        	   else
        	   {
        		   // send an ack!
        		   sendAck(msg);
        	   }
    		   q.add(msg);
    		   lk.release();
    	   }
       }
       
       void sendAck(MailMessage msg)
       {
    	   short msgId = DataMsg.getMsgId(msg);
    	   try
    	   {
			AckMsg ack = new AckMsg(msg.packet.srcLink, msg.srcPort, msg.packet.dstLink, msg.dstPort, msgId);
			NetKernel.postOffice.send(ack.mailMsg());
    	   }
    	   catch (MalformedPacketException e)
    	   {
			e.printStackTrace();
    	   }
       }
       
       void updateSlidingWindow(MailMessage msg)
       {
    	   int origSz = wnd.sentMsgIdList.size();
    	   Short s = new Short(AckMsg.getMsgId(msg));
    	   if(wnd.sentMsgIdList.contains(s))
    	   {
    		   wnd.sentMsgIdList.remove(s);
    		   if(origSz == SlidingWindow.maxWndCapacity)
    		   {
    			   // being here means that this update reduces the size of the window from 16 to 15
    			   // - wake up the sender so he can continue sending any messages
    			   windowClearSema.V();
    		   }
    	   }
       }
       private int port;
       private LinkedList<MailMessage> q;
       private Lock lk;
       private Semaphore windowClearSema;
       private SlidingWindow wnd;
   }
   
	 public int srcId, srcPort, destId, destPort;
	 private short sendMsgId = 0;
	 KThread rcvLoopThread = null;
	 LinkedList<MailMessage> inMsgQ = new LinkedList<MailMessage>();
	 Lock qLock = new Lock();
	 Semaphore sema = new Semaphore(0);
	 SlidingWindow slidingWnd;
	 
	 public static int channelId = 0;
	 ConnectionState state;
	 public static enum ConnectionState {SYN_SENT, SYN_RCVD, ESTABLISHED, STP_RCVD, STP_SENT, CLOSING, CLOSED}
}
