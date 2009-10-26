/* createClose.c
 *	Just does a create and close of a filename specified.
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

  //syscall.h has create misspelled as creat
  //printf("Creating and then closing file  - CreateOpenCloseTestFile.txt\n");
  fileDescriptor = creat("CreateOpenCloseTestFile.txt");

  success = close(fileDescriptor);

  halt();
  /* not reached */
}
