
#include "stdio.h"

int main(int argc, char *argv[])
{
	void* tempBuffer[100];
	int bytesToRead = 23;
	int server = 0;
	int port = 100;
	int bytesSent = 0;

	int socket = connect(server,port);
	if ( socket != -1 )
	{
	  char *c = ("Test message1");
	  int bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);

	  c = ("Test message2");
	  bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);

	  c = ("Test message3");
	  bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);

	  c = ("Test message4");
	  bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);

	  c = ("Test message5");
	  bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);

	  c = ("Test message6");
	  bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);

	  c = ("Test message7");
	  bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);

	  c = ("Test message8");
	  bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);

	  c = ("Test message9");
	  bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);

	  c = ("Test messag10");
	  bytesToWrite = strlen(c);
	  bytesSent = write(socket, (void*)c, bytesToWrite);

	  close(socket);
	}
}
