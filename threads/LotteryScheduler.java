package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the Lottery inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a Lottery scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new priority scheduler.
     */

    /**
     * The scheduling state of a thread. This should include the thread's
     * Lottery, its effective Lottery, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see nachos.threads.KThread#schedulingState
     */
    protected class LotteryThreadState {
        /**
         * Allocate a new <tt>LotteryThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param   thread  the thread this state belongs to.
         */
        public LotteryThreadState(KThread thread) {
            this.thread = thread;
            //setLottery(LotteryDefault);
            effectiveLottery = Lottery = LotteryDefault;
        }

        /**
         * Return the Lottery of the associated thread.
         *
         * @return  the Lottery of the associated thread.
         */
        public int getLottery() {
            return Lottery;
        }

        /**
         * Return the effective Lottery of the associated thread.
         *
         * @return  the effective Lottery of the associated thread.
         */
        public int getEffectiveLottery() {
            // implement me
            return effectiveLottery;
        }

        /**
         * Set the Lottery of the associated thread to the specified value.
         *
         * @param   Lottery    the new Lottery.
         */
        public void setLottery(int Lottery) {
            if (this.Lottery == Lottery)return;
            this.Lottery = Lottery;
            // implement me
            calculateEffectiveLottery();
        }

        public void calculateEffectiveLottery(){
            int ans = Lottery;
            for (LotteryQueue q : acquired)
                if (q.transferLottery){
                    ans = ans + q.totalLottery; //Math.max(ans, ts.getEffectiveLottery());
                }

            if (ans != effectiveLottery){
            //System.out.println(Lottery + " ++ " + ans + " " + waiting.size());
                int update = ans - effectiveLottery;
                for (LotteryQueue q : waiting.keySet()){
                    //System.out.println("tttttttttttt " +  q.transferLottery);
                    q.totalLottery += update;
                    if (q.transferLottery){
                        //System.out.println("tttttttttttt " +  update);
                        if(q.currentThread != null)
                            getLotteryThreadState(q.currentThread).calculateEffectiveLottery();
                        Lib.assertTrue(q.waitQueue.contains(this));
                    }
                }
            }
            effectiveLottery = ans;
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified Lottery queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param   waitQueue   the queue that the associated thread is
         *              now waiting on.
         *
         * @see nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(LotteryQueue waitQueue) {
            // implement me
            //this function link a process with a queue 
            if (!waiting.containsKey(waitQueue)){
                //If i am not waiting for this queue
                Lib.assertTrue(waitQueue.waitQueue.contains(this) == false);
                release(waitQueue);//If I hold the lock, I need release first, I think it's strange.
                long time = Machine.timer().getTime();
                waiting.put(waitQueue, time);

                //waitQueue.findNextTread = null;
                //waitQueue.pickNextThread();

                waitQueue.waitQueue.add(this); //put myself into the queue
                waitQueue.totalLottery += this.getEffectiveLottery();

                //waitQueue.findNextTread = null;
                //waitQueue.pickNextThread();

                //System.out.println("totalLottery " + waitQueue.totalLottery + " " + this.effectiveLottery);
                if (waitQueue.currentThread != null){
                    getLotteryThreadState(waitQueue.currentThread).calculateEffectiveLottery();
                }
            }
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see nachos.threads.ThreadQueue#acquire
         * @see nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(LotteryQueue waitQueue) {
            // implement me
            //Lib.assertTrue(waitQueue.waitQueue.isEmpty());
            if (waitQueue.currentThread != null){
                //assert(false);
                getLotteryThreadState(waitQueue.currentThread).release(waitQueue);
            }
            waitQueue.currentThread = this.thread;
            //System.out.println("remove " + this.getEffectiveLottery() + " " + (waitQueue.waitQueue.contains(this)));
            //waitQueue.findNextTread = null;
            //waitQueue.pickNextThread();

            if(waitQueue.waitQueue.contains(this))
                waitQueue.totalLottery -= this.getEffectiveLottery();
            waitQueue.waitQueue.remove(this);

            acquired.add(waitQueue);
            waiting.remove(waitQueue);
            calculateEffectiveLottery();
        }

        public void release(LotteryQueue waitQueue) {
            if (acquired.remove(waitQueue)) {
                //If I have this lock
                //release this lock
                waitQueue.currentThread = null;
                calculateEffectiveLottery();
            }
        }

        /** The thread with which this object is associated. */    
        protected KThread thread;
        /** The Lottery of the associated thread. */
        protected int Lottery;
        protected int effectiveLottery;
        private HashSet<LotteryQueue> acquired = new HashSet<LotteryQueue>();
        private HashMap<LotteryQueue, Long> waiting = new HashMap<LotteryQueue,Long>();

    }


    /**
     * A <tt>ThreadQueue</tt> that sorts threads by Lottery.
     */
    protected class LotteryQueue extends ThreadQueue {

        private java.util.Set<LotteryThreadState> waitQueue = new java.util.HashSet<LotteryThreadState>();
        LotteryQueue(boolean transferLottery) {
            this.transferLottery = transferLottery;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getLotteryThreadState(thread).waitForAccess(this);
            findNextTread = null;
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getLotteryThreadState(thread).acquire(this);
            findNextTread = null;
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me
            if (waitQueue.isEmpty())
                return null;
            else {
                //System.out.println(totalLottery);
                LotteryThreadState s = pickNextThread();
                acquire(s.thread);
                return currentThread;
            }
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return  the next thread that <tt>nextThread()</tt> would
         *      return.
         */
        protected LotteryThreadState pickNextThread() {
            // implement me
            if(findNextTread!=null)
                return findNextTread;
            int sum = 0;
            //System.out.println(this);
            for(LotteryThreadState s: waitQueue){
                sum += s.getEffectiveLottery();
                //for(LotteryQueue t:s.waiting.keySet()){
                //    System.out.println(s + " " + t);
                //}
            }
            int p = 0;
            if(sum>0) p = Lib.random(sum);
            //System.out.println("Lottery sum " +  sum + " " + totalLottery);
            Lib.assertTrue(sum == totalLottery, "m");
            sum = 0;
            for(LotteryThreadState s:waitQueue){
                sum += s.getEffectiveLottery();
                findNextTread = s;
                if(sum >= p)
                    return s;
            }
            //System.out.println(" " + sum + " " + p);
            return null;
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }

        /**
         * <tt>true</tt> if this queue should transfer Lottery from waiting
         * threads to the owning thread.
         */
        public boolean transferLottery;
        protected KThread currentThread = null;
        protected LotteryThreadState findNextTread = null;
        protected int totalLottery = 0;
    }

    public LotteryScheduler() {
    }

    /**
     * Allocate a new Lottery thread queue.
     *
     * @param   transferLottery    <tt>true</tt> if this queue should
     *                  transfer Lottery from waiting threads
     *                  to the owning thread.
     * @return  a new Lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferLottery) {
        return new LotteryQueue(transferLottery);
    }

    public int getLottery(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getLotteryThreadState(thread).getLottery();
    }

    public int getEffectiveLottery(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getLotteryThreadState(thread).getEffectiveLottery();
    }

    public void setLottery(KThread thread, int Lottery) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(Lottery >= LotteryMinimum &&
                Lottery <= LotteryMaximum);

        LotteryThreadState state = getLotteryThreadState(thread);
        if (Lottery != state.getLottery())state.setLottery(Lottery);
    }

    public boolean increaseLottery() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int Lottery = getLottery(thread);
        boolean flag = true;
        if (Lottery == LotteryMaximum)
            flag = false;
        else setLottery(thread, Lottery+1);

        Machine.interrupt().restore(intStatus);
        return flag;
    }

    public boolean decreaseLottery() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int Lottery = getLottery(thread);
        boolean flag = true;
        if (Lottery == LotteryMinimum)
            flag = false;
        else setLottery(thread, Lottery-1);

        Machine.interrupt().restore(intStatus);
        return flag;
    }

    /**
     * The default Lottery for a new thread. Do not change this value.
     */
    public static final int LotteryDefault = 1;
    /**
     * The minimum Lottery that a thread can have. Do not change this value.
     */
    public static final int LotteryMinimum = 1;
    /**
     * The maximum Lottery that a thread can have. Do not change this value.
     */
    public static final int LotteryMaximum = Integer.MAX_VALUE;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param   thread  the thread whose scheduling state to return.
     * @return  the scheduling state of the specified thread.
     */
    
    protected LotteryThreadState getLotteryThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new LotteryThreadState(thread);

        return (LotteryThreadState) thread.schedulingState;
    }

    public void Testself(){

        boolean intStatus = Machine.interrupt().disable();

        int N = 50;
        KThread[] threads = new KThread[N];
        for (int i=0;i<N;++i)threads[i] = new KThread();
        ThreadQueue[] queues = new LotteryQueue[N];
        for (int i=0;i<N;++i)queues[i] = newThreadQueue(true);
        for (int i=0;i<8;++i)queues[i].waitForAccess(threads[i]);
        for (int i=0;i<8;++i)setLottery(threads[i], 4);
        //System.out.println("test 1");
        //for (int i=0;i<8;++i)System.out.println(i + "" + getEffectiveLottery(threads[i]));
        for (int i=0;i<8;++i)Lib.assertTrue(getEffectiveLottery(threads[i]) == 4);
        for (int i=0;i<8;++i)setLottery(threads[i + 8], i);
        for (int i=0;i<8;++i)queues[i].acquire(threads[i+8]);
        System.out.println("selfTest Lottery");
    }
    public static void selfTest(){
        LotteryScheduler a = new LotteryScheduler();
        a.Testself();
    }
}