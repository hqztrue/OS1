package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.*;
import java.lang.Math;
//import java.util.PriorityQueue;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        ThreadState state = getThreadState(thread);
        if (priority != state.getPriority())state.setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        boolean flag = true;
        if (priority == priorityMaximum)
            flag = false;
        else setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return flag;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        boolean flag = true;
        if (priority == priorityMinimum)
            flag = false;
        else setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return flag;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {

        private java.util.PriorityQueue<ThreadState> waitQueue = new java.util.PriorityQueue<ThreadState>(1, new TComparator(this));
        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me
            if (waitQueue.isEmpty())
                return null;
            else {
                acquire(waitQueue.poll().thread);
                return currentThread;
            }
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return	the next thread that <tt>nextThread()</tt> would
         *		return.
         */
        protected ThreadState pickNextThread() {
            // implement me
            return waitQueue.peek();
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }

        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;
        protected KThread currentThread = null;
        protected class TComparator implements Comparator<ThreadState>{
            private PriorityQueue priorityQueue;
            public TComparator(PriorityQueue q){

                priorityQueue = q;
            }
            public int compare(ThreadState t1, ThreadState t2){
                int p1 = t1.getEffectivePriority(), p2 = t2.getEffectivePriority();
                if (p1 > p2)return -1;
                else if (p1 < p2)return 1;
                else {
                    Long time1 = t1.waiting.get(priorityQueue), time2 = t2.waiting.get(priorityQueue);
                    if (time1 < time2)return -1;
                    else if (time1 > time2)return 1;
                    else return 0;
                }
            }
        }
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param	thread	the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;
            //setPriority(priorityDefault);
            effectivePriority = priority = priorityDefault;
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return	the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return	the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            // implement me
            return effectivePriority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param	priority	the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)return;
            this.priority = priority;
            // implement me
            calculateEffectivePriority();
        }

        public void calculateEffectivePriority(){
            int ans = priority;
            for (PriorityQueue q : acquired)
                if (q.transferPriority){
                    ThreadState ts = q.pickNextThread();
                    if (ts != null)ans = Math.max(ans, ts.getEffectivePriority());
                }
            if (ans != effectivePriority){
                for (PriorityQueue q : waiting.keySet())
                    if (q.transferPriority && q.currentThread != null)
                        getThreadState(q.currentThread).calculateEffectivePriority();
            }
            effectivePriority = ans;
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param	waitQueue	the queue that the associated thread is
         *				now waiting on.
         *
         * @see	nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityQueue waitQueue) {
            // implement me
            if (!waiting.containsKey(waitQueue)){
                release(waitQueue);
                long time = Machine.timer().getTime();
                waiting.put(waitQueue, time);
                waitQueue.waitQueue.add(this);
                if (waitQueue.currentThread != null)
                    getThreadState(waitQueue.currentThread).calculateEffectivePriority();
            }
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see	nachos.threads.ThreadQueue#acquire
         * @see	nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
            // implement me
			//Lib.assertTrue(waitQueue.waitQueue.isEmpty());
            if (waitQueue.currentThread != null){
				//assert(false);
                getThreadState(waitQueue.currentThread).release(waitQueue);
			}
            waitQueue.currentThread = this.thread;
            waitQueue.waitQueue.remove(this);
            acquired.add(waitQueue);
            waiting.remove(waitQueue);
            calculateEffectivePriority();
        }

        public void release(PriorityQueue waitQueue) {
            if (acquired.remove(waitQueue)) {
                waitQueue.currentThread = null;
                calculateEffectivePriority();
            }
        }

        /** The thread with which this object is associated. */	   
        protected KThread thread;
        /** The priority of the associated thread. */
        protected int priority;
        protected int effectivePriority;
        private HashSet<PriorityQueue> acquired = new HashSet<PriorityQueue>();
        private HashMap<PriorityQueue, Long> waiting = new HashMap<PriorityQueue,Long>();

    }

	public void Testself(){

        boolean intStatus = Machine.interrupt().disable();

		int N = 50;
		KThread[] threads = new KThread[N];
		for (int i=0;i<N;++i)threads[i] = new KThread();
		ThreadQueue[] queues = new PriorityQueue[N];
		for (int i=0;i<N;++i)queues[i] = newThreadQueue(true);
		for (int i=0;i<8;++i)queues[i].waitForAccess(threads[i]);
		for (int i=0;i<8;++i)setPriority(threads[i], 4);
		//System.out.println("test 1");
		//for (int i=0;i<8;++i)System.out.println(i + "" + getEffectivePriority(threads[i]));
		for (int i=0;i<8;++i)Lib.assertTrue(getEffectivePriority(threads[i]) == 4);
		for (int i=0;i<8;++i)setPriority(threads[i + 8], i);
		for (int i=0;i<8;++i)queues[i].acquire(threads[i+8]);
		//System.out.println("test 2");
		//for (int i=0;i<8;++i)System.out.println(i + "" + getEffectivePriority(threads[i]));
		for (int i=0;i<8;++i)Lib.assertTrue(getEffectivePriority(threads[i + 8]) == (i>=4?i:4));
		for (int i=0;i<8;++i)Lib.assertTrue(getEffectivePriority(threads[i]) == 4);
		{
			for (int i=7;i>0;--i)queues[i-1].waitForAccess(threads[i + 8]);
			for (int i=0;i<7;++i){
                //System.out.println("haha"+(((PriorityQueue)queues[i]).pickNextThread().getEffectivePriority()));
                Lib.assertTrue((((PriorityQueue)queues[i]).pickNextThread().getEffectivePriority())==7);
            }
			System.out.println("test end 1");
	Machine.interrupt().restore(intStatus);
			return;
		}
		//for (int i=0;i<8;++i)System.out.println(i + "" + );
		//System.out.println("test 3");
		/*for (int i=0;i<8;++i)(getThreadState(threads[i + 8])).release((PriorityQueue)queues[i]);
		for (int i=0;i<8;++i)Lib.assertTrue(getEffectivePriority(threads[i + 8]) == i);
		for (int i=0;i<8;++i)Lib.assertTrue(getEffectivePriority(queues[i].nextThread())==4);
		//for (int i=0;i<16;++i)System.out.println(i + "" + getEffectivePriority(threads[i]));
		System.out.println("test end");
	Machine.interrupt().restore(intStatus);*/
	}
    public static void selfTest(){
        PriorityScheduler a = new PriorityScheduler();
        a.Testself();
    }
}


