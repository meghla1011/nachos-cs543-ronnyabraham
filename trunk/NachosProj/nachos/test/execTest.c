/* this program is used for testing exec */
#include "syscall.h"

int main(int argc, char *argv[])
{
	exec("sh.coff", argc, argv);
}
