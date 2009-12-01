
#include "stdio.h"

int main(int argc, char *argv[])
{

      int server = 0;
	  int port = 100;
	  int bytesSent = 0;

	  int socket = connect(server,port);
	  if ( socket != -1 )
	  {
		  char *c = ("Hello World");
		  int bytesToWrite = strlen(c);
		  bytesSent = write(socket, (void*)c, bytesToWrite);
	  }
}
