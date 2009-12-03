package nachos.network;

import nachos.machine.MalformedPacketException;

public class FinAckMsg
{
	
	public FinAckMsg(int dstLink, int dstPort, int srcLink, int srcPort, short msgId) throws MalformedPacketException
	{	
		byte[] msgIdBuf = new byte[2];
		msgIdBuf[0] = (byte)(msgId >> 8);
		msgIdBuf[1] = (byte)(msgId & 0x00FF);
		byte[] contents = new byte[3];
		contents[0] = MailMessage.FINACK;
		System.arraycopy(msgIdBuf, 0, contents, 1, 2);
		msg = new MailMessage(dstLink, dstPort, srcLink, srcPort, contents);
	}
	
	public MailMessage mailMsg()
	{
		return msg;
	}
	
	public static boolean isFinAck(MailMessage msg)
	{
		boolean retval = false;
		if(msg.contents.length == 3)
		{
			if(msg.contents[0] == MailMessage.FINACK)
			{
				retval = true;
			}
		}
		return retval;
	}
	
	public static short getMsgId(MailMessage msg)
	{
		
		short retval = 0;
		if(msg.contents.length >= 3)
		{
			retval |= msg.contents[1];
			retval <<= 8;
			retval |= msg.contents[2];
		}
		return retval;
	}
	
	MailMessage msg;
}
