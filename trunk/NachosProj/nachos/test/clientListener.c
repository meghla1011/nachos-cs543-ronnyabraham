/* clientListener.c
 */

#include "syscall.h"
#include "stdio.h"

int
main()
{
	//Code to test launching multiple processes
	/*
  int fileDescriptor = 1;
  int success = 0;
  int bytesRead = 0;

  //write
  char *c = ("Launched clientListener");
  unsigned int bytesToWrite = strlen(c);
  int bytesWritten = write(fileDescriptor, (void*)c, bytesToWrite);
//////////////////////////////////
  */

	int stdOutFd = 1;
	int serverAddress = 0;
	int port = 15;
	int maxLength = 1000;
	char localBuffer[maxLength];
	int exitSequence = 0;
	int socket = connect(serverAddress,port);
	int bufferIndex = 0;

	if ( socket != -1 )
	{
		printf("Client successfully connected to server\n");

		//loop until it receives exit message
		while (! exitSequence)
		{
			int amountRead = read(socket,localBuffer,maxLength);

			//write to standard out console
			int bytesWritten = write(stdOutFd, (void*)localBuffer, amountRead);

			//check if exit sequence was entered, period at beginning
			//of the line (ascii 46)
			if ((int)localBuffer[0] == 46 )
			{
				exitSequence = 1;
			}
		}
	}	  
	else
	{
	   printf("Failed to connect to the chat server\n");
	}
}
