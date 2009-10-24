/* write.c
 *	Just do a create the filename specified.
 *
 * 	NOTE: for some reason, user programs with global data structures 
 *	sometimes haven't worked in the Nachos environment.  So be careful
 *	out there!  One option is to allocate data structures as 
 * 	automatics within a procedure, but if you do this, you have to
 *	be careful to allocate a big enough stack to hold the automatics!
 */

#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int
main()
{
  int fileDescriptor = 0;
  int success = 0;
  int bytesWritten = 0;

  //syscall.h has create misspelled as creat
  fileDescriptor = open("TestReadFile.log");

  //void* tempBuffer[100];
  char *c = (" - Only Ernie and Kevin should both get an A++ in this class!");

   int bytesToWrite = strlen(c);

  bytesWritten = write(fileDescriptor, (void*)c, bytesToWrite);
  success = close(fileDescriptor);

  halt();
  /* not reached */
}
