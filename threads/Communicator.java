package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
		lock = new Lock();
		speaker = new Condition(lock);
		listener = new Condition(lock);
		nlistener = 0;
		nspeaker = 0;
		can_write = true;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
		lock.acquire();
		while (nlistener==0 || !can_write){
			++nspeaker;
			speaker.sleep();
			--nspeaker;
		}
		word_trans = word;
		can_write = false;
		listener.wake();
		lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
		lock.acquire();
		++nlistener;
		while (can_write){
			speaker.wake();
			listener.sleep();
		}
		int word = word_trans;
		can_write = true;
		--nlistener;
		lock.release();
		return word;
    }
	private Lock lock;
	private Condition speaker;
	private Condition listener;
	private int word_trans, nlistener, nspeaker;
	private boolean can_write;
}
