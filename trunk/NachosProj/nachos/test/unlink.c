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
  int fd1, fd2, fd3;
  int success = 0;

  success = unlink("CreateFileTest.txt");
  printf("Deleting file  - CreateFileTest.txt, verify it is deleted!\n\n");

  printf("Creating file UnlinkTest.txt, then referring it from several handles\n");
  printf("Testing that unlink defers the deletion until close is called on the last handle\n");

  fd1 = creat("UnlinkTest.txt");
  fd2 = open("UnlinkTest.txt");

  printf("We now have fd1 and fd2 referring to UnlinkTest.txt with values: %d, %d\n\n", fd1, fd2);
  success = unlink("CreateFileTest.txt");
  printf("Calling unlink on fd2, this returned %d\n", success);
  printf("Check that UnlinkTest.txt is not deleted\n\n\n\n");

  printf("Creating file UnlinkTest2.txt, then referring it from several handles\n");
  fd1 = creat("UnlinkTest2.txt");
  fd2 = open("UnlinkTest2.txt");
  fd3 = open("UnlinkTest2.txt");
  printf("fd1, fd2 and fd3 are %d, %d, %d\n", fd1, fd2, fd3);
  close(fd1);
  success = unlink("CreateFileTest2.txt");
  printf("Closed fd1, then called unlink which returned: %d\n\n", success);

  printf("Writing to file to test it succeeded and unlink did not delete the file\n\n");
  char *c = ("blabla");
  int bytesToWrite = strlen(c);
  int bytesWritten = write(fd2, (void*)c, bytesToWrite);
  printf("Write returned %d\n\n", bytesWritten);

  close(fd2);
  close(fd3);
  printf("Closed all file descriptors to UnlinkTest2.txt, verify it is deleted\n");

  halt();
  /* not reached */
}
