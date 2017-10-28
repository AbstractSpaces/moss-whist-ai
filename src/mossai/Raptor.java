package mossai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** An agent (hopefully) capable of intelligently playing Moss Side Whist. */
public class Raptor implements MSWAgent
{
    /** Bias constant for Monte Carlo play outs. */
    static final double BIAS;
    
    /** Whether to treat draws as wins in Monte Carlo play outs. */
    static final boolean DRAW_WINS;
    
    /** How long to keep running Monte Carlo on samples each turn, in milliseconds. */
    static final int SEARCH_TIME;
    
    /** Number of play outs to sample in each Monte Carlo search. */
    static final int MC_SAMPLES;
    
    /** The probability threshold above which to treat as certain that an opponent possesses a card. */
    static final double POSITIVE;
    
    private static String ME;
    
    static
    {
        BIAS = Math.sqrt(2.0);
        DRAW_WINS = false;
        SEARCH_TIME = 190;
        MC_SAMPLES = 10;
        POSITIVE = 0.75;
        ME = "Clever Girl";
    }
    
    private String left;
    private String right;
	
	private HashMap<String, Integer> positions;
	private HashMap<String, Integer> totalScores;
	private int[] handScores;
    
    private GameState state;
    
    public Raptor()
	{
		positions = new HashMap(3);
/////////////////////////////// DEBUGGING //////////////////////////////////////
		totalScores = new HashMap(3);
////////////////////////////////////////////////////////////////////////////////
	}
    
    @Override
    public void setup(String agentLeft, String agentRight)
    {
        left = agentLeft;
        right = agentRight;
    }

    @Override
    public void seeHand(List<Card> deal, int order)
    {
		positions.put(ME, order);
		positions.put(left, (order+1)%3);
		positions.put(right, (order+2)%3);
		
		state = new GameState(order, deal);
/////////////////////////////// DEBUGGING //////////////////////////////////////	
		for(String p : positions.keySet())
			if(totalScores.get(p) == null)
				if(positions.get(p) == 0)
					totalScores.put(p, -8);
				else
					totalScores.put(p, -4);
			else
				if(positions.get(p) == 0)
					totalScores.put(p, totalScores.get(p)-8);
				else
					totalScores.put(p, totalScores.get(p)-4);
		
		handScores = new int[] {-8, -4, -4};
////////////////////////////////////////////////////////////////////////////////
    }

    @Override
    public Card[] discard()
    {
        if(positions.get(ME) == 0)
        {
            Card[] chosen = new Card[4];

            for(int i = 0; i < 4; i++)
				chosen[i] = state.discardLow();
			
            return chosen;
        }
        else return new Card[4];
    }
	
    @Override
    public Card playCard()
    {
	
        Card best = null;
        long start = System.nanoTime();
/*     
        // A record of how many times each card was recommended by a Monte Carlo search.
        HashMap<Card, Integer> results = new HashMap();
        
        while(System.nanoTime() - start < SEARCH_TIME * 1000000)
        {
            Card c = state.monteCarlo();
            
            if(results.get(c) == null)
                results.put(c, 1);
            else
                results.put(c, results.get(c) + 1);
        }
        
        for(Card c : results.keySet())
            if(best == null || results.get(c) > results.get(best))
                best = c;
*/        
        
        // Temp version.
        best = state.greedyEval();
        
        return best;
    }

    @Override
    public void seeCard(Card card, String agent)
	{
/////////////////////////////// DEBUGGING //////////////////////////////////////
		System.out.println("seeCard given move by: " + agent);
		System.out.println(agent + " order set " + positions.get(agent) + " in Raptor.");
		if(positions.get(agent) != state.active())
			System.out.println("BIG OBVIOUS PRINT THAT IS EASY TO SEE");
////////////////////////////////////////////////////////////////////////////////
		state.advance(card);
	}

    @Override
    public void seeResult(String winner)
    {
/////////////////////////////// DEBUGGING //////////////////////////////////////
		totalScores.put(winner, totalScores.get(winner) + 1);
		handScores[positions.get(winner)]++;
////////////////////////////////////////////////////////////////////////////////
    }

    @Override
    public void seeScore(Map<String, Integer> scoreboard)
    {
/////////////////////////////// DEBUGGING //////////////////////////////////////
		System.out.println();
		
		System.out.println("Hand scores in Raptor:");
		for(int i : handScores)
			System.out.println(i);
		
		System.out.println();
		
		System.out.println("Hand scores in GameState:");
			int[] gsScores = state.getScores();
			for(int i : gsScores)
				System.out.println(i);
		
		System.out.println();
		for(String p : totalScores.keySet())
			System.out.println(p + " score in Raptor: " + totalScores.get(p));
////////////////////////////////////////////////////////////////////////////////
    }

    @Override
    public String sayName() { return ME; }
}