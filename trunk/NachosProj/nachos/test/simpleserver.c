#include "stdio.h"

int main(int argc, char *argv[])
{
      
	  int port = 100;
	  while(1)
	  {
		  int socket = accept(port);
		  if ( socket != -1 )
		  {
				printf("Server successfully accepted a client connection \n");
		  }
		  else{
			   printf("Failed to accept a client a connection \n");
		  }
      }	  
}
