package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    static int numChildA, numChildB, numAdultA, numAdultB;
    static int boatSide, start = 0;
    //boatSide 0: A, 1: B ,2:in Use

    final static Lock lock = new Lock();//lock of A
    final static Condition HasAdult  = new Condition(lock);
    final static Condition HasBoat  = new Condition(lock);
    final static Condition waitRider  = new Condition(lock);
    final static Condition ChildPilotBack  = new Condition(lock);
    
    public static void selfTest()
    {
        BoatGrader b = new BoatGrader();

        System.out.println("\n ***Testing Boats with only 2 children***");
        //begin(0, 2, b);

        System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        //begin(1, 2, b);

        System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        //begin(3, 3, b);

        System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        begin(10, 4, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
        start = 0;
        boatSide = 0;
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;

        // Instantiate global variables here

        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

        KThread[] t = new KThread[adults + children];
        for(int i = 0;i<adults;++i){
            t[i] = new KThread( new Runnable() {
                public void run() {
                    AdultItinerary();
                }
            }).setName("adult "+i);
        }
        for(int j = adults;j<adults + children;j++){
            t[j] = new KThread(new Runnable() {
                public void run() {
                    ChildItinerary();
                }
            }).setName("child "+j);
        }
        numChildA = children;
        numAdultA = adults;
        for(int j =0; j<adults + children; ++j)
            t[j].fork();
        for(int j= 0;j<adults + children; ++j)
            t[j].join();
    }

    static void AdultItinerary()
    {
        bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE. 

        /* This is where you should put your solutions. Make calls
           to the BoatGrader to show that it is synchronized. For
example:
bg.AdultRowToMolokai();
indicates that an adult has rowed the boat across to Molokai
*/
        lock.acquire();
        while(numChildA>=2 || boatSide != 0){
            //System.out.println(boatSide + " " + KThread.currentThread().getName());
            HasAdult.sleep();
        }
        //System.out.println(boatSide + " " + KThread.currentThread().getName());
        numAdultA--;
        numAdultB++;
        boatSide = 1;
        bg.AdultRowToMolokai();
        ChildPilotBack.wakeAll();
        lock.release();
    }

    static void sendChildToMolokai(int num){
        numChildA -= num;
        bg.ChildRowToMolokai();
        if(num == 2)
            bg.ChildRideToMolokai();
        numChildB += num;
        if(num == 1){
            boatSide = 1;
            ChildPilotBack.wakeAll();
        }
    }

    static void sendInvite(){
        //System.out.println("send invite " + KThread.currentThread().getName() + " " + numChildA);
        boatSide = 2;
        HasBoat.wake();
        waitRider.sleep();

        boatSide = 1;
        ChildPilotBack.wakeAll();
        //System.out.println("receive invite " + KThread.currentThread().getName() + " " + numChildA);
    }

    static void ChildItinerary()
    {
        bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE. 
        int side = 0;
        while(true){
            if(side == 0){
                lock.acquire();
                while(start==1 && ( boatSide == 1 || boatSide == 3 || (numChildA == 1 && numAdultA>0)) ){
                    HasBoat.sleep();
                }
                start = 1;
                if(boatSide==2){
                    boatSide = 3;
                    sendChildToMolokai(2);
                    waitRider.wake();
                }else{
                    if(numChildA == 1){
                        if(numAdultA==0){
                            sendChildToMolokai(1);
                        }else
                            side = -1;
                    }
                    else if(numChildA>1){
                        sendInvite();
                    }
                }
                if(side == -1){
                    HasAdult.wake();
                }else
                    side = 1;
                lock.release();
                if(numChildA == 0 && numAdultA == 0)
                    break;
            }
            else if(side == 1){
                lock.acquire();
                while(boatSide!=1){
                    ChildPilotBack.sleep();
                }
                if(numChildA == 0 && numAdultA == 0){
                    lock.release();
                    break;
                }

                numChildB-=1;
                bg.ChildRowToOahu();
                boatSide = 0;

                numChildA+=1;
                //System.out.println("haha " + boatSide + " "+ numChildA + " " + KThread.currentThread().getName());
                if(numChildA == 1)
                    HasAdult.wake();
                else
                    HasBoat.wakeAll();
                lock.release();
                side = 0;
            }
        }
    }

    static void SampleItinerary()
    {
        // Please note that this isn't a valid solution (you can't fit
        // all of them on the boat). Please also note that you may not
        // have a single thread calculate a solution and then just play
        // it back at the autograder -- you will be caught.
        System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }

}
