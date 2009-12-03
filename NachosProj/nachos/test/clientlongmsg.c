
#include "stdio.h"

int main(int argc, char *argv[])
{
	int server = 0;
	int port = 100;
	int bytesSent = 0;

	int socket = connect(server,port);
	if ( socket != -1 )
	{
	  char *c = ("TEST A LONG LONG LONG MESSAGE, YES IT IS LONG, I THINK THIS IS LONG ENOUGH");
	  int bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);
	}
}
