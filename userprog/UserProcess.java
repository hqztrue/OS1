package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.*;
import java.lang.Math.*;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	{  //can be ignored
		for (int i=0; i<numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i,i,false,false,false,false);  //i,i,true,false,false,false
	}
	UserKernel.semProcessID.P();
	processID = UserKernel.nextProcessID++;
	++UserKernel.numExistingProcesses;
	UserKernel.semProcessID.V();
	
	fileTable = new OpenFile[MAX_NUM_OPEN_FILES];
	fileTable[0] = UserKernel.console.openForReading();
	fileTable[1] = UserKernel.console.openForWriting();
	
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
	//if (!validAddr(vaddr))return 0;
	memoryLock.acquire();
	byte[] memory = Machine.processor().getMemory();
	int nread = 0;
	while (length>0){
		if (!validAddr(vaddr))break;
		int vpn = Processor.pageFromAddress(vaddr), voffset = Processor.offsetFromAddress(vaddr);
		int len = Math.min(Processor.pageSize - voffset, length);
		TranslationEntry entry = pageTable[vpn];
		if (entry==null || !entry.valid)break;
		System.arraycopy(memory, Processor.pageSize * entry.ppn + voffset, data, offset, len);
		nread += len;
		length -= len;
		vaddr += len;
		offset += len;
	}
	memoryLock.release();
	/*// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(memory, vaddr, data, offset, amount);*/
	return nread;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
	memoryLock.acquire();
	byte[] memory = Machine.processor().getMemory();
	int nwrite = 0;
	while (length>0){
		if (!validAddr(vaddr))break;
		int vpn = Processor.pageFromAddress(vaddr), voffset = Processor.offsetFromAddress(vaddr);
		int len = Math.min(Processor.pageSize - voffset, length);
		TranslationEntry entry = pageTable[vpn];
		if (entry==null || !entry.valid || entry.readOnly)break;
		System.arraycopy(data, offset, memory, Processor.pageSize * entry.ppn + voffset, len);
		entry.used = entry.dirty = true;
		nwrite += len;
		length -= len;
		vaddr += len;
		offset += len;
	}
	memoryLock.release();
	return nwrite;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}
	int numPages_ = numPages;
	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;
	for (int i=numPages_;i<numPages;++i){
		if (pageTable[i]==null)pageTable[i] = new TranslationEntry(i,i,false,false,false,false);
		TranslationEntry entry = pageTable[i];
		UserKernel.semFreePhyPages.P();
		if (UserKernel.freePhyPages.size()==0){
			UserKernel.semFreePhyPages.V();
			coff.close();
			unloadSections();
			return false;
		}
		int id = UserKernel.freePhyPages.removeFirst();
		UserKernel.semFreePhyPages.V();
		entry.valid = true;
		entry.ppn = id;
	}
	if (!loadSections()){
		//coff.close();
		unloadSections();
	    return false;
	}

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
			int vpn = section.getFirstVPN()+i;
			
			if (pageTable[vpn]==null)pageTable[vpn] = new TranslationEntry(vpn,vpn,false,false,false,false);
			TranslationEntry entry = pageTable[vpn];
			UserKernel.semFreePhyPages.P();
			if (UserKernel.freePhyPages.size()==0){
				UserKernel.semFreePhyPages.V();
				coff.close();
				return false;
			}
			int id = UserKernel.freePhyPages.removeFirst();
			UserKernel.semFreePhyPages.V();
			entry.valid = true;
			entry.readOnly = section.isReadOnly();
			entry.ppn = id;
			section.loadPage(i, entry.ppn);
			/*// for now, just assume virtual addresses=physical addresses
			section.loadPage(i, vpn);*/
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
		UserKernel.semFreePhyPages.P();
		for (int i=0;i<pageTable.length;++i){
			TranslationEntry entry = pageTable[i];
			if (entry!=null && entry.valid){
				entry.valid = false;
				UserKernel.freePhyPages.add(entry.ppn);
			}
		}
		UserKernel.semFreePhyPages.V();
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

	private boolean validAddr(int vaddr){
		int vpn = Processor.pageFromAddress(vaddr);
		return vpn < numPages && vpn >= 0;
	}
	private int abnormalExit(){
		handleExit(null);
		return -1;
	}
    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
		if (processID!=0)return 0;
		unloadSections();
		for (int i=0;i<fileTable.length;++i)  //i=2
			if (fileTable[i]!=null)fileTable[i].close();
		Machine.halt();
		
		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
    }
	
	private int handleExit(Integer status){  //null: unhandled exception
		unloadSections();
		for (int i=0;i<fileTable.length;++i)
			if (fileTable[i]!=null)fileTable[i].close();
		exitStatus = status;
		semJoin.V();
		
		UserKernel.semProcessID.P();
		if (--UserKernel.numExistingProcesses == 0)
            Kernel.kernel.terminate();
		UserKernel.semProcessID.V();
		KThread.finish();
		return 0;
	}
	
	private int handleExec(int name, int argc, int argv){
		if (!validAddr(name) || !validAddr(argv))return abnormalExit();
		String name_str = readVirtualMemoryString(name, MAX_STR_LEN);
		if (argc<0 || name_str == null || !name_str.endsWith(".coff"))return -1;
		//no upperbound for argc?
		String args[] = new String[argc];
		byte argvs[] = new byte[argc*4];
		if (readVirtualMemory(argv, argvs)!=argc*4)return -1;
		for (int i=0;i<argc;++i){
			int p = Lib.bytesToInt(argvs, 4*i);
			if (!validAddr(p))return abnormalExit();
			args[i] = readVirtualMemoryString(p, MAX_STR_LEN);
		}
		UserProcess child = new UserProcess();
		childs.put(child.processID, child);
		if (!child.execute(name_str, args))return -1;
		return child.processID;
	}
	
	private int handleJoin(int pid, int status){
		if (!validAddr(status))return abnormalExit();
		UserProcess child = childs.get(pid);
		if (child==null)return -1;
		child.semJoin.P();
		childs.remove(pid);
		if (child.exitStatus==null)return 0;
		else {
			writeVirtualMemory(status, Lib.bytesFromInt(child.exitStatus.intValue()));
			return 1;
		}
	}
	
	private int open(int name, boolean flag){
		if (!validAddr(name))return abnormalExit();
		String name_str = readVirtualMemoryString(name, MAX_STR_LEN);
		//OpenFile file = Machine.stubFileSystem().open(name_str, flag);
		OpenFile file = UserKernel.fileSystem.open(name_str, flag);
		if (file != null){
			for (int i=0;i<MAX_NUM_OPEN_FILES;++i)
				if (fileTable[i]==null){
					fileTable[i] = file;
					return i;
				}
		}
		return -1;
	}
	private int handleCreate(int name){
		return open(name, true);
	}
	
	private int handleOpen(int name){
		return open(name, false);
	}
	
	private int handleRead(int fd, int buffer, int size){
		if (!validAddr(buffer))return abnormalExit();
		if (fd<0 || fd>=MAX_NUM_OPEN_FILES)return -1;
		OpenFile file = fileTable[fd];
		if (file==null)return -1;
		byte buf[] = new byte[size];
		int size_read = file.read(buf, 0, size);
		if (size_read == -1)return -1;
		int size_write = writeVirtualMemory(buffer, buf, 0, size_read);
		if (size_read != size_write)return -1;
		return size_read;
	}
	
	private int handleWrite(int fd, int buffer, int size){
		if (!validAddr(buffer))return abnormalExit();
		if (fd<0 || fd>=MAX_NUM_OPEN_FILES)return -1;
		OpenFile file = fileTable[fd];
		if (file==null)return -1;
		byte buf[] = new byte[size];
		int size_read = readVirtualMemory(buffer, buf);
		if (size_read != size)return -1;
		int size_write = file.write(buf, 0, size_read);
		if (size_write != size)return -1;
		return size_write;
	}
	
	private int handleClose(int fd){
		if (fd<0 || fd>=MAX_NUM_OPEN_FILES || fileTable[fd] == null)return -1;
		fileTable[fd].close();
		fileTable[fd] = null;
		return 0;
	}
	
	private int handleUnlink(int name){
		if (!validAddr(name))return abnormalExit();
		String name_str = readVirtualMemoryString(name, MAX_STR_LEN);
		if(UserKernel.fileSystem.remove(name_str))
            return 0;
        else return -1;
	}

    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallExit:
		return handleExit(a0);
	case syscallExec:
		return handleExec(a0, a1, a2);
	case syscallJoin:
		return handleJoin(a0, a1);
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a0);
	case syscallRead:
		return handleRead(a0, a1, a2);
	case syscallWrite:
		return handleWrite(a0, a1, a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);

	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
		abnormalExit();
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
	
	public static final int MAX_NUM_OPEN_FILES = 16, MAX_STR_LEN = 256;
	private OpenFile fileTable[];
	private int processID;
	private Integer exitStatus = null;
	public Semaphore semJoin = new Semaphore(0);
	public HashMap<Integer, UserProcess> childs = new HashMap<Integer, UserProcess>();
	private Lock memoryLock = new Lock();
}
