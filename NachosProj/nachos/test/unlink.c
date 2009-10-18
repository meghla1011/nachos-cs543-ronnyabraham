/* create.c
 *	Simple program to test whether running a user program works.
 *	
 *	Just do a create the filename specified.
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
    //fileDescriptor = open("TestFileToDelete.log");

	success = unlink("TestFileToDelete.log");

	//success = close(fileDescriptor);

    halt();
    /* not reached */
}
