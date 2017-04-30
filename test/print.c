#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv)
{
	int i = 0, sum = 0;
	for(i = 0;i<1000;++i)
		sum += i;
	printf("End %s %d\n", argv[1], sum);
	int ans = 0;
	int fid = creat("123");
	fprintf(fid, "%s\n", argv[1]);
	close(fid);
	return 0;
}