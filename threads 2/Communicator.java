package nachos.threads;

import nachos.machine.*;
import java.util.*;

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

    private static class SpeakerWriterTester implements Runnable {
        int which;
        Communicator tester;
        SpeakerWriterTester(int which, Communicator tester) {
            this.which = which;
            this.tester = tester;
        }

        public void run() {
            if(which %2 == 0){
                for(int i = 0; i<2; ++i){
                    tester.speak(which * 100 + i);
                }
            }
            else{
                for(int i = 0; i<2; ++i)
                    tester.listen();
            }
        }
    }

    public static void selfTest(){
        Communicator tester = new Communicator();
        ArrayList<KThread> a = new ArrayList<KThread>();
        for(int i = 0;i<10;++i){
            a.add(new KThread(new SpeakerWriterTester(i, tester)).setName("forked thread2"));
            a.get(i).fork();
        }
        for(int i = 0;i<10;++i)
            a.get(i).join();
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

        Lib.debug('c', " speak " + word);
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
		while (can_write){
			++nlistener;
			speaker.wake();
			listener.sleep();
			--nlistener;
		}

		int word = word_trans;
        Lib.debug('c', " listen " + word);
		can_write = true;
		speaker.wake();
		lock.release();
		return word;
    }
	private Lock lock;
	private Condition speaker;
	private Condition listener;
	private int word_trans, nlistener, nspeaker;
	private boolean can_write;
}
