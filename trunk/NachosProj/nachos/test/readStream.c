/* readStream.c
 *	Reads from the console.
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
  int fileDescriptor = 0;
  int success = 0;
  int bytesRead = 0;

  void* tempBuffer[100];
  int bytesToRead = 5;
  bytesRead = read(fileDescriptor, tempBuffer, bytesToRead);
  //printf("You typed: %s", (char*)tempBuffer);

  halt();
  /* not reached */
}
