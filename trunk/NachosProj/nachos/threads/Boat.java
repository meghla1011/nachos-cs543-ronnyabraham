package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	
	//System.out.println("\n ***Testing Boats with only 2 children***");
	//begin(0, 2, b);

	//System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
  	//begin(1, 2, b);

  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
    	// Store the externally generated autograder in a class
    	// variable to be accessible by children.
    	bg = b;

    	// Instantiate global variables here

    	// Create threads here. See section 3.4 of the Nachos for Java
    	// Walkthrough linked from the projects page.

    	Runnable adultRunnable = new Runnable() {
    		public void run() {
    			AdultItinerary();
    		}
    	};

    	Runnable childRunnable = new Runnable() {
    		public void run() {
    			ChildItinerary();
    		}
    	};

    	for (int a =0; a < adults; a++)
    	{
    		KThread t = new KThread(adultRunnable);
    		t.setName("Adult " + a);
    		t.fork();
    	}

    	for (int c =0; c < children; c++)
    	{
    		KThread t = new KThread(childRunnable);
    		t.setName("Child " + c);
    		t.fork();
    	}
    }

    static void AdultItinerary()
    {
    	if (! adultRoleCallLock.isHeldByCurrentThread())
    	{
    		adultRoleCallLock.acquire();
    		numberOfAdults++;
    		adultRoleCallLock.release();
    	}
    	
    	if (! adultWaitingForTurnLock.isHeldByCurrentThread())
    	{
    		adultWaitingForTurnLock.acquire();
			Lib.debug(debug, "Adult Waiting for Turn, acquired lock");
    	}
    	if (! adultReadyToRideLock.isHeldByCurrentThread())
    	{
    		adultReadyToRideLock.acquire();
    		Lib.debug(debug, "Adult Ready to ride, Sleeping until woken by child");
    		adultReadyToRideCondition.sleep();
    		
        	if (! adultRoleCallLock.isHeldByCurrentThread())
        	{
        		adultRoleCallLock.acquire();
        		numberOfAdults--;
        		adultRoleCallLock.release();
        	}

    		Lib.debug(debug, "Adult rowing to Molokai");
    		bg.AdultRowToMolokai();
    		
    		//wake up child sleeping on Molokai Island
    		if (! childOnOahuLock.isHeldByCurrentThread())
    		{
    			Lib.debug(debug, "Adult waking child sleeping on Molokai");
    			childOnOahuLock.acquire();
    			childOnOahuCondition.wake();
    			childOnOahuLock.release();
    		}
    		
    		if ( adultReadyToRideLock.isHeldByCurrentThread())
    		{
    			adultReadyToRideLock.release();
    			Lib.debug(debug, "Adult released ready to ride lock");
        		
    		}
    		if (adultWaitingForTurnLock.isHeldByCurrentThread())
    		{
    			adultWaitingForTurnLock.release();
    			Lib.debug(debug, "Adult released waiting for turn lock");
    		}
    	}
    	//Adult is across and done!
    }

    static void ChildItinerary()
    {
    	if (! childRoleCallLock.isHeldByCurrentThread())
    	{
    		childRoleCallLock.acquire();
    		numberOfChildren++;
    		childRoleCallLock.release();
    	}
    	
    	while (!scenarioComplete) 
    	{
    		if (! childWaitingForTurnOahuLock.isHeldByCurrentThread())
    		{
    			childWaitingForTurnOahuLock.acquire();
    			Lib.debug(debug, "Child Waiting for Turn on Oahu, acquired lock");
    		}

    		if (! driver)
    		{
    			driver = true;

    			if (! childRoleCallLock.isHeldByCurrentThread())
    			{
    				childRoleCallLock.acquire();
    				numberOfChildren--;
    				childRoleCallLock.release();
    			}

    			bg.ChildRowToMolokai();
    			Lib.debug(debug, "Child rowing to Molokai");

    			if (! childOnOahuLock.isHeldByCurrentThread())
    			{
    				childOnOahuLock.acquire();
    			}

    			childWaitingForTurnOahuLock.release();
    			childOnOahuCondition.sleep();

    			bg.ChildRowToOahu();

    			if (! childReadyToRideLock.isHeldByCurrentThread())
    			{
    				childReadyToRideLock.acquire();
    				childReadyToRideCondition.wake();
    				childReadyToRideLock.release();
    			}

    			if (childOnOahuLock.isHeldByCurrentThread())
    			{
    				childOnOahuLock.release();
    			}

    			//loop back up to get in line
    		}
    		else //child will be passenger
    		{    		
    			Lib.debug(debug, "Child riding to Molokai");

    			if (! childRoleCallLock.isHeldByCurrentThread())
    			{
    				childRoleCallLock.acquire();
    				numberOfChildren--;
    				childRoleCallLock.release();
    			}

    			bg.ChildRideToMolokai();

    			Lib.debug(debug, "Child riding to Oahu");

    			if (! childRoleCallLock.isHeldByCurrentThread())
    			{
    				childRoleCallLock.acquire();
    				numberOfChildren++;
    				childRoleCallLock.release();
    			}

    			bg.ChildRowToOahu();

    			if (numberOfAdults > 0)
    			{
    				if (! adultReadyToRideLock.isHeldByCurrentThread())
    				{
    					adultReadyToRideLock.acquire();
    					Lib.debug(debug, "Adult woken by child to ride");
    					adultReadyToRideCondition.wake();
    					adultReadyToRideLock.release();
    				}
    				if (! childReadyToRideLock.isHeldByCurrentThread())
    				{
    					childReadyToRideLock.acquire();
    					childReadyToRideCondition.sleep();
    				}

    				if (childReadyToRideLock.isHeldByCurrentThread())
    				{
    					childReadyToRideLock.release();
    				}  

    				if (childWaitingForTurnOahuLock.isHeldByCurrentThread())
    				{
    					driver = false;
    					childWaitingForTurnOahuLock.release();
    				}
    				//loop back up
    			}
    			else if (numberOfChildren > 1)
    			{
    				//release locks
    				if (childWaitingForTurnOahuLock.isHeldByCurrentThread())
    				{
    					driver = false;
    					childWaitingForTurnOahuLock.release();
    				}
    				//loop up
    			}
    			else
    			{
    				Lib.debug(debug, "Child riding to Molokai");

    				if (! childRoleCallLock.isHeldByCurrentThread())
    				{
    					childRoleCallLock.acquire();
    					numberOfChildren--;
    					childRoleCallLock.release();
    				}

    				//rows back
    				bg.ChildRowToMolokai();

    				if (childWaitingForTurnOahuLock.isHeldByCurrentThread())
    				{
    					childWaitingForTurnOahuLock.release();
    				}

    				//lock not needed because only the last child will edit this variable
    				scenarioComplete = true;
    			}
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
    

    private static final char debug = 't';
    
	private static Lock adultRoleCallLock = new Lock();
	private static Lock childRoleCallLock = new Lock();
    private static Lock adultWaitingForTurnLock = new Lock();
    private static Lock childWaitingForTurnOahuLock = new Lock();
    
    private static Lock adultReadyToRideLock = new Lock();
    private static Condition2 adultReadyToRideCondition = 
    	new Condition2(adultReadyToRideLock);
    
    private static Lock childReadyToRideLock = new Lock();
    private static Condition2 childReadyToRideCondition =
    	new Condition2(childReadyToRideLock);
	
    private static Lock childOnOahuLock = new Lock();
    private static Condition2 childOnOahuCondition =
    	new Condition2(childOnOahuLock);
	
	private static boolean driver = false;
	private static boolean scenarioComplete = false;
	
	private static int numberOfChildren = 0;
	private static int numberOfAdults = 0;
    
//    private Lock adultReadyToRideLock;
//    Condition adultReadyToRideCondition;
}
