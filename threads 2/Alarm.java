package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;
import java.util.List;
import java.util.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	waitQueue = new LinkedList<Semaphore>();
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */

    static class Testclass implements Runnable{
        int which;
        long time;
        Testclass(int which, long time){
            this.which = which;
            this.time = time;
        }
        public void run(){
            long begin = Machine.timer().getTime();
            ThreadedKernel.alarm.waitUntil(this.time);
            long end = Machine.timer().getTime();
            if(end - begin<this.time){
                Lib.debug('x', begin + " " + end);
            }
	        Lib.assertTrue(end - begin >= this.time);
        }
    }

    public static void selfTest(){
        Lib.debug('x', "Test alarm");
        KThread thread1 = new KThread(new Testclass(0, 100)).setName("a");
        KThread thread2 = new KThread(new Testclass(0, 200)).setName("b");
        KThread thread3 = new KThread(new Testclass(0, 300)).setName("c");
        KThread thread4 = new KThread(new Testclass(0, 400)).setName("d");
        thread1.fork();
        thread2.fork();
        thread3.fork();
        thread4.fork();
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
    }

    public void timerInterrupt() {
	//KThread.currentThread().yield();
	//lock.acquire();
	boolean intStatus = Machine.interrupt().disable();
	Iterator<Semaphore> it = waitQueue.iterator();
	Iterator<Long> it1 = waitTimeQueue.iterator();
	while (it.hasNext()){
		Long time = it1.next();
		Semaphore waiter = it.next();
		if (Machine.timer().getTime() > time){
			waiter.V();
			it1.remove();
			it.remove();
		}
	}
	Machine.interrupt().restore(intStatus);
	//lock.release();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	/*long wakeTime = Machine.timer().getTime() + x;
	while (wakeTime > Machine.timer().getTime())
	    KThread.yield();
	*/
	//lock.acquire();
	boolean intStatus = Machine.interrupt().disable();
	Semaphore waiter = new Semaphore(0);
	waitQueue.add(waiter);
	waitTimeQueue.add(Machine.timer().getTime() + x);
	//lock.release();
	Machine.interrupt().restore(intStatus);
	waiter.P();
    }
	private List<Semaphore> waitQueue = new LinkedList<Semaphore>();
	private List<Long> waitTimeQueue = new LinkedList<Long>();
}


