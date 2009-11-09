package nachos.vm;

import nachos.machine.*;
import java.util.*;

public class SwappingFile {
	
	/*
	 * class SwapFile
	 * Implements page swapping in ant out of a disk
	 */
	public SwappingFile ()
	{
		keys = new Vector<String>();
		entries = new Vector<TranslationEntry>();
		swappingFile = Machine.stubFileSystem().open("SwappingFile", true);
	}

	/*
	 * readPage
	 * read a page from the swap file
	 */
	public TranslationEntry readPage(int pid, int vpn, int ppn)
	{
		String key = pid + "," + vpn;	
		int idx = keys.indexOf(key);
		if(idx == -1)
		{
			return null;
		}
		int pos = idx * Machine.processor().pageSize;
		// read a page from the swap file
		swappingFile.read(pos, Machine.processor().getMemory(), Processor.makeAddress(ppn, 0), Machine.processor().pageSize);
		// set the translation entry with the new info
		TranslationEntry retval = entries.get(idx);
		retval.ppn = ppn;
		retval.used = false;
		retval.dirty = false;
		// stop tracking this page as it is going to the inverted table
		keys.set(idx, "");
		entries.set(idx, null);
		return retval;
	}
	
	public void writePage(int pid, int vpn, TranslationEntry entry)
	{
		// add a new key and entry to our vectors
		String key = pid + "," + vpn;
		keys.add(key);
		entries.add(entry);
		// write the page in our vector's i-th position
		int pos = keys.indexOf(key) * Machine.processor().pageSize;
		swappingFile.write(pos, Machine.processor().getMemory(), Processor.makeAddress(entry.ppn, 0), Machine.processor().pageSize );
	}
	
	// we maintain 2 vectors, a vector of keys, where a key is a [ process-id, vpn] pair
	// and a matching vector of translation entries
	Vector<String> keys;
	Vector<TranslationEntry> entries;
	OpenFile swappingFile;
}
