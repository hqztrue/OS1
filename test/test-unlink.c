#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv)
{
  int fd, amount, pid, status;

  if (argc!=2) {
    printf("Usage: test-unlink <file>\n");
    return 1;
  }

  fd = open(argv[1]);
  printf("In first open.\n");
  if (fd==-1) {
    printf("Unable to open %s\n", argv[1]);
    return 1;
  }

  pid = exec("rm.coff", argc, argv);
  if (pid != -1)
	  join(pid, &status);
  else {
    printf("Unable to exec rm childprocess %s\n", argv[1]);
    return 1;
  }

  pid = exec("cat.coff", argc, argv);
  if (pid != -1)
	  join(pid, &status);
  else {
    printf("Unable to exec cat childprocess %s\n", argv[1]);
    return 1;
  }

  printf("Before close, after cat.\n");
  getchar();
  close(fd);
  printf("Closed.\n");
  getchar();

  fd = open(argv[1]);
  printf("In second open.\n");
  if (fd==-1) {
    printf("Unable to open %s\n", argv[1]);
    return 1;
  }

  return 0;
}
