package nachos.network;

import java.util.Vector;

/*
 * class channel
 */

public class Channel {

	 private int sourceId;
	 private int sourcePort;
	 private int destId;
	 private int destPort;
	 
	 public Channel(int srcId,int srcport,int dstId,int dstport )
	 {
	     sourceId = srcId;
	     sourcePort = srcport;
	     destId = dstId;
	     destPort = dstport;
	 }
	 
	 public int handleConnect()
	 {
		 if( currState != State.CLOSED)
		 {
			 return -1;
		 }
		 
		 currState = State.SYN_SENT;
		 sendMessage(true,false,false,false);
		 
		 
		 
	 }
	 
	 public void sendMessage(boolean sync,boolean ack, boolean fin, boolean stp)
	 {
		 int message = 0;
		 if( sync)
		 {
			 message += 1000;
		 }
		 if ( ack)
		 {
			 message += 100;
		 }
		 if( stp) 
		 {
			 message += 10; 
		 }
		 if( fin)
		 {
			 message += 1;
		 }
		 
		 if( pendingMessages.contains(message))
		 {
			 return 0;
		 }
	 }
	 
	 State currState = State.CLOSED;
	 private Vector<Integer> pendingMessages = new Vector<Integer>();
	 public static enum Event {CONNECT, ACCEPT, RCVD, SEND, CLOSE, TIMER, SYN, SYNACK, DATA, ACK, STP, FIN, FINACK}
	 public static enum State {CLOSED, SYN_SENT, SYN_RCVD, ESTABLISHED, STP_SENT, STP_RCVD, CLOSING}
}
