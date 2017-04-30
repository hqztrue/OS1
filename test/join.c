#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"


int main(int argc, char **argv){
	int array[10];
	printf("start %d\n", -1);
	int tmp[10], i;
	for(i=0;i<10;i++){
		char buffer[33];
		buffer[0] = i + 'a';
		buffer[1] = 0;
		printf("start_j %d %s\n", i, buffer);
		char* argv2[] = {"print.coff", buffer};
		array[i] = exec("print.coff", 2, argv2);
	}
	for(i = 0;i<10;++i)
		join(array[i], tmp + i);
	return 0;
}