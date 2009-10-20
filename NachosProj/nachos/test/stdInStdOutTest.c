/* stdInStdOutTest.c
 *	Tests std input and std output file descriptors.
 *
 * 	NOTE: for some reason, user programs with global data structures 
 *	sometimes haven't worked in the Nachos environment.  So be careful
 *	out there!  One option is to allocate data structures as 
 * 	automatics within a procedure, but if you do this, you have to
 *	be careful to allocate a big enough stack to hold the automatics!
 */

#include "syscall.h"

int
main()
{
  //read
  int fileDescriptor = 0;
  int success = 0;
  int bytesRead = 0;

  //write
  fileDescriptor = 1;
  char *c = ("Waiting to read the first 15 bytes typed to console: ");
  unsigned int bytesToWrite = strlen(c);
  int bytesWritten = write(fileDescriptor, (void*)c, bytesToWrite);
  
  //read
  fileDescriptor = 0;
  void* tempBuffer[100];
  int bytesToRead = 15;
  bytesRead = read(fileDescriptor, tempBuffer, bytesToRead);
  
  //write
  fileDescriptor = 1;
  char* returnMsg = ("Read 15 bytes from console!!!");
  bytesToWrite = strlen(returnMsg);
  bytesWritten = write(fileDescriptor, (void*)returnMsg, bytesToWrite);

  fileDescriptor = 0;
  success = close(fileDescriptor);
  fileDescriptor = 1;
  success = close(fileDescriptor);

  //attempt to write after descriptor was closed - should return -1 since the stream was already closed
  fileDescriptor = 1;
  char* newMsg = ("Writing to console after closing descriptor 1");
  bytesToWrite = strlen(newMsg);
  bytesWritten = write(fileDescriptor, (void*)newMsg, bytesToWrite);


  halt();
  /* not reached */
}
