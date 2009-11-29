
#include "stdio.h"

int main(int argc, char *argv[])
{

      int server = 0;
	  int port = 100;
	  
	  int socket = connect(server,port);
	  if ( socket != -1 )
	  {
			printf("Client successfully connected to server\n");
	  }	  
	  else
	  {
		   printf("Failed to connect to a server\n");
	  }
}
