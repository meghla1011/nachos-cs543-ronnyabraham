package nachos.vm;

import nachos.machine.*;
import java.util.*;

public class SwapFile {
	
	public SwapFile ()
	{
		pageSize = Machine.processor().pageSize;
		swapFile = Machine.stubFileSystem().open("SwapFile", true);
	}

	public void writeToFile(int pid, int vpn, TranslationEntry te)
	{
		String key = pid + "," + vpn;
		currentFileEntries.add(key);
		currentFileTE.add(te);
		int index = currentFileEntries.indexOf(key);
		
		swapFile.write( (index * pageSize), Machine.processor().getMemory(), Processor.makeAddress(te.ppn, 0), pageSize );
	}

	public TranslationEntry readFromFile(int pid, int vpn, int ppn)
	{
		// First read the data, save to te
		String key = pid + "," + vpn;
		
		int indexToRead = currentFileEntries.indexOf(key);
		
		if ( indexToRead == -1 )
			return null;
		
		int readLen = swapFile.read(indexToRead  * pageSize, Machine.processor().getMemory(), Processor.makeAddress(ppn, 0), pageSize);

		TranslationEntry te = currentFileTE.get(indexToRead);
		te.ppn = ppn;
		te.dirty = false;
		te.used = false;
		
		// Now blank out it's entry, destroying the links
		currentFileEntries.set(indexToRead, "");
		currentFileTE.set(indexToRead, null);
		
		return te;
	}



	Vector <String> currentFileEntries = new Vector <String>();
	Vector <TranslationEntry> currentFileTE = new Vector <TranslationEntry>();
	OpenFile swapFile;
	static int pageSize;

}
