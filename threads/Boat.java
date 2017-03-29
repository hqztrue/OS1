package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    static int numChildA, numAdultA, numChildAToSee, numAdultAToSee, numChildBToSee, numAdultBToSee;
    static int boatSide, start = 0;
    //boatSide 0: A, 1: B ,2:in Use

    final static Lock lock = new Lock();//lock of A
    final static Condition waitA  = new Condition(lock);
    final static Condition waitB  = new Condition(lock);
    final static Condition waitRider  = new Condition(lock);

    public static void selfTest()
    {
        BoatGrader b = new BoatGrader();

        System.out.println("\n ***Testing Boats with only 2 children***");
        begin(0, 2, b);

        //System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
        //begin(1, 2, b);

        //System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
        //begin(3, 3, b);

        //System.out.println("\n ***Testing Boats with 10 children, 4 adults***");
        //begin(10, 4, b);

        System.out.println("\n ***Testing Boats with 20 children, 20 adults***");
        begin(20, 20, b);
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

        numChildAToSee = 0;
        numAdultAToSee = 0;
        for(int j =0; j<adults + children; ++j){
            t[j].fork();
        }

        lock.acquire();
        while(numAdultA>0 || numChildA>0){
            waitB.sleep();
        }
        for(int j = 0;j<adults + children;++j)
            t[j].finish();
        lock.release();
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
        int side = 0;
        lock.acquire();
        numAdultAToSee += 1;
        lock.release();
        while(true){
            lock.acquire();
            if(side == 0){
                while(boatSide != 0 && numChildAToSee>=2)
                    waitA.sleep();
                numAdultA--;
                numAdultAToSee--;
                numAdultBToSee++;

                boatSide = 1;
                bg.AdultRowToMolokai();

                waitB.wakeAll();
            }
            else if(side == 1){
                while(boatSide !=1 && numChildBToSee>=1)
                    waitB.sleep();
                numAdultA ++;
                numAdultAToSee++;
                numAdultBToSee--;

                boatSide = 0;
                bg.AdultRowToOahu();
                waitA.wakeAll();
            }
            lock.release();
        }
    }

    static void sendChildToMolokai(int num){
        numChildA -= num;
        numChildAToSee -= num;
        numChildBToSee += num;
        bg.ChildRowToMolokai();
        if(num == 2)
            bg.ChildRideToMolokai();
        if(num == 1){
            boatSide = 1;
            waitB.wakeAll();
        }
    }

    static void sendInvite(){
        //System.out.println("send invite " + KThread.currentThread().getName() + " " + numChildA);
        boatSide = 2;
        waitA.wakeAll();
        waitRider.sleep();

        boatSide = 1;
        waitB.wakeAll();
        //System.out.println("receive invite " + KThread.currentThread().getName() + " " + numChildA);
    }

    static void ChildItinerary()
    {
        bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE. 

        lock.acquire();
        numChildAToSee += 1;
        lock.release();

        int side = 0;
        while(true){
//        System.out.println(KThread.currentThread().getName() +" "+ numChildA + " " + numChildAToSee);
            lock.acquire();
            if(side == 0){
                while(boatSide == 1 || boatSide == 3 || (numChildAToSee == 1 && numAdultAToSee>0) || numChildAToSee == 1)
                    waitA.sleep();
                start = 1;
                if(boatSide==2){
                    boatSide = 3;
                    sendChildToMolokai(2);
                    waitRider.wake();
                }else{
                    if(numChildAToSee == 1){
                        sendChildToMolokai(1);
                    }else if(numChildAToSee>1)
                        sendInvite();
                }
                side = 1;
            }
            else if(side == 1){
                while(boatSide!=1)
                    waitB.sleep();
                bg.ChildRowToOahu();
                boatSide = 0;

                numChildA+=1;
                numChildAToSee+=1;
                numChildBToSee-=1;
                waitA.wakeAll();
                side = 0;
            }
            lock.release();
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
