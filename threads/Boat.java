package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    static int numChildA, numChildB, numAdultA, numAdultB;
    static int boatSide, hasPilot, start = 0;

    final static Lock lockA = new Lock();//lock of A
    final static Condition2 HasAdult  = new Condition2(lockA);
    final static Condition2 HasBoat  = new Condition2(lockA);
    final static Condition2 waitRider  = new Condition2(lockA);

    final static Lock lockB = new Lock();//lock of B
    final static Condition2 ChildPilotBack  = new Condition2(lockB);
    
    public static void selfTest()
    {
        BoatGrader b = new BoatGrader();

        //System.out.println("\n ***Testing Boats with only 2 children***");
        //begin(0, 2, b);

        //System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        //begin(1, 2, b);

        System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        begin(0, 3, b);
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
        lockA.acquire();
        while(numChildA>=2 || boatSide == 1){
            HasAdult.sleep();
        }

        lockB.acquire();
        numAdultA--;
        numAdultB++;
        boatSide = 1;
        bg.AdultRowToMolokai();
        ChildPilotBack.wakeAll();
        lockB.release();
        lockA.release();
    }

    static void sendChildToMolokai(int num){
        lockB.acquire();
        numChildA -= num;
        bg.ChildRowToMolokai();
        if(num == 2)
            bg.ChildRideToMolokai();
        numChildB += num;
        boatSide = 1;
        ChildPilotBack.wakeAll();
        lockB.release();
    }

    static void ChildItinerary()
    {
        bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE. 
        int side = 0;
        while(true){
            if(side == 0){
                lockA.acquire();
                if(start==1 && hasPilot==0)
                    HasBoat.sleep();
                start = 1;
                if(hasPilot==1){
                    hasPilot = 0;
                    sendChildToMolokai(2);
                    waitRider.wake();
                }else{
                    if(numChildA == 1){
                        if(numAdultA==0)
                            sendChildToMolokai(1);
                        else
                            HasAdult.wake();
                    }
                    else if(numChildA>1){
                        hasPilot = 1;
                        HasBoat.wake();
                        waitRider.sleep();
                    }
                }
                lockA.release();
                if(numChildA == 0 && numAdultA == 0){
                    break;
                }
                side = 1;
            }
            else if(side == 1){
                lockB.acquire();
                while(boatSide==0)
                    ChildPilotBack.sleep();
                lockA.acquire();
                System.out.println(numChildA + " " + numAdultA);
                if(numChildA == 0 && numAdultA == 0){
                    lockA.release();
                    break;
                }
                numChildB-=1;
                bg.ChildRowToOahu();
                boatSide = 0;
                numChildA+=1;
                if(numChildA == 1){
                    HasAdult.wake();
                }else{
                    hasPilot = 1;
                    HasBoat.wake();
                }
                lockA.release();
                lockB.release();
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
