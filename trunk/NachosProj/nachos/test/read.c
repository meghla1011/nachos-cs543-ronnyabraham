/* read.c
 *	Opens a preexisting file, reads from it and closes it.
 *
 * 	NOTE: for some reason, user programs with global data structures
 *	sometimes haven't worked in the Nachos environment.  So be careful
 *	out there!  One option is to allocate data structures as
 * 	automatics within a procedure, but if you do this, you have to
 *	be careful to allocate a big enough stack to hold the automatics!
 */

#include "syscall.h"
#include "stdio.h"

int
main()
{
  int fileDescriptor = 0;
  int success = 0;
  int bytesRead = 0;

  //syscall.h has create misspelled as creat
  fileDescriptor = open("TestReadFile.log");

  void* tempBuffer[100];
  int bytesToRead = 391;

  bytesRead = read(fileDescriptor, tempBuffer, bytesToRead);

  //printf("Opening TestReadFile.log for reading, then writing it to stdout:\n");
  write(1,tempBuffer,bytesToRead);


  success = close(fileDescriptor);

  halt();
  /* not reached */
}
