/* open.c
 *	Creates, closes, and tries to open a filename.
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
  int fileDescriptor1 = 0;
  int fileDescriptor2 = 0;
  int success = 0;

  //syscall.h has create misspelled as creat
  fileDescriptor1 = creat("CreateNewTestFile.txt");
  //printf("Creating file - CreateNewTestFile.txt, creat returned: %d\n", fileDescriptor1);
  success = close(fileDescriptor1);
  //printf("Closing - CreateNewTestFile.txt, close returned: %d\n", success);

  //opens a file that does not exist
  fileDescriptor2 = open("FileNotFoundError.txt");
  //printf("Attempting to open a non-existing file, open returned: %d\n", fileDescriptor2);

  //opens a file that does exist
  fileDescriptor1 = open("CreateNewTestFile.txt");
  //printf("Opening CreateNewTestFile.txt, open returned: %d\n", fileDescriptor1);

  success = close(fileDescriptor2);
  success = close(fileDescriptor1);

  halt();
  /* not reached */
}
