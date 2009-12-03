/* clientUserInput.c
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
  char *c = ("Launched clientUserInput");
  unsigned int bytesToWrite = strlen(c);
  int bytesWritten = write(fileDescriptor, (void*)c, bytesToWrite);
//////////////////////////////////
  */

//Assume the server will be network address 0
	int stdInFd = 0;
	int serverAddress = 0;
	int port = 15;
	char localBuffer[1];
	char writeBuffer[1000];
	int exitSequence = 0;
	int socket = connect(serverAddress,port);
	int bufferIndex = 0;

	if ( socket != -1 )
	{
		printf("Client successfully connected to server\n");

		//loop until it receives exit message
		while (! exitSequence)
		{
			read(stdInFd,localBuffer,1);

			//if not <CR> 13 ascii, append to buffer
			if ((int)localBuffer != 13)
			{
				writeBuffer[bufferIndex] = localBuffer[0];
				bufferIndex++;
			}
			else
			{
				//check if exit sequence was entered, period at beginning
				//of the line (ascii 46)
				if ((int)writeBuffer[0] == 46 )
				{
					exitSequence = 1;
				}

				write (socket,writeBuffer,bufferIndex);

				//reset the writeBuffer
				bufferIndex = 0;
			}
		}
	}	  
	else
	{
	   printf("Failed to connect to the chat server\n");
	}
}
