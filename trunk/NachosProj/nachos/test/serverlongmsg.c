#include "stdio.h"

int main(int argc, char *argv[])
{

	void* tempBuffer[100];
	int bytesToRead = 80;

	  int port = 100;
	  //while(1)
	  //{
		  int socket = accept(port);
		  if ( socket != -1 )
		  {
				printf("Server successfully accepted a client connection \n");
		  }
		  else{
			   printf("Failed to accept a client a connection \n");
		  }
		  int i = 0;
		  while(i < 10)
		  {
			  read(socket, tempBuffer, bytesToRead);
			  read(socket, tempBuffer, bytesToRead);
			  read(socket, tempBuffer, bytesToRead);
			  i++;
		  }

      //}
}