/* unlink.c
 *	Deletes the file created by create.c.
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

  //opens a file
  //fileDescriptor = creat("CreateFileTest.txt");
  
  success = unlink("CreateFileTest.txt");

  halt();
  /* not reached */
}
