/* chatServer.c
 *	Chat Server.
 */

#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int
main()
{
	int port = 15;
	int fileDescriptor = 0;
	int nextAvailIndex = 0;
	int counter = 0;
	int maxNumClients = 20;
	int maxLength = 1000;
	char localBuffer[maxLength];
	int clientList[maxNumClients];
	int exitTest = 0;

  while (exitTest != 1)
  {
	  //accept pending clients
	  while ( (fileDescriptor = accept(port)) && (fileDescriptor != -1) )
	  {
		clientList[nextAvailIndex] = fileDescriptor;
		nextAvailIndex++;
	  }

	  int i = 0;
	  while (i < maxNumClients)
	  {
		  if (clientList[i] != 0)
		  {
			  //read 1000 bytes from fileDescriptor i
			  int amountRead = read(clientList[i],localBuffer,maxLength);

				if (amountRead > 0)
				{
					int j = 0;
  					while (j < maxNumClients)
					{
						if (clientList[j] != 0)
						{
						  int bytesWritten = write(clientList[j], (void*)localBuffer, amountRead);
						}
						if ((int)localBuffer[0] == 46 )
						{
							clientList[i] = 0;
						}
						j++;
					}
				}
		  }
			 
		  i++;
	  }
  }
}
