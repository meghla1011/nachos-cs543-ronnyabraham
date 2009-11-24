/* connect.c
 *	Calls connect.
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
  int host = 1;
  int port = 21;
  int fileDescriptor = connect(host, port);

  //need to call some reads and writes (do this in the selfTests of the NetProcess or here?)

    halt();
    /* not reached */
}
