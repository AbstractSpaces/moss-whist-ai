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
    
    /** The probability threshold below which to treat as certain that an opponent does not possess a card. */
    static final double NEGATIVE;
    
    static
    {
        BIAS = Math.sqrt(2.0);
        DRAW_WINS = false;
        SEARCH_TIME = 190;
        MC_SAMPLES = 10;
        POSITIVE = 0.75;
        NEGATIVE = 0.01;
    }
    
    /** Names of players, left to right from the agent. */
    private final String[] names;
    
    /** Map of player names to their positions. */
    private final HashMap<String, Integer> players;
    
    private GameState state;
    
    public Raptor()
    {
        names = new String[] {"Clever Girl", "", ""};
        players = new HashMap(3);
        state = null;
    }
    
    @Override
    public void setup(String agentLeft, String agentRight)
    {
        names[1] = agentLeft;
        names[2] = agentRight;
    }

    @Override
    public void seeHand(List<Card> deal, int order)
    {
        state = new GameState(order, deal);
		
        for(int i = 0; i < 3; i++)
			players.put(names[i], (order+i)%3);
		
		System.out.println("Agent: " + order);
    }

    @Override
    public Card[] discard()
    {
		Card[] chosen = new Card[4];
		
        for(int i = 0; i < 4; i++)
			chosen[i] = state.discardLow();
		
		return chosen;
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
		
        state.advance(best);
        return best;
    }

    @Override
    public void seeCard(Card card, String agent) { state.advance(card); }

    @Override
    public void seeResult(String winner)
    {
        // Verify that state is working correctly.
		if(state.active() != players.get(winner))
			System.out.println("State judged incorrect winner.");
    }

    @Override
    public void seeScore(Map<String, Integer> scoreboard)
    {
        // Verify that state is working properly.
        int[] scores = state.getScores();
        
        for(String n : names)
			if(scores[players.get(n)] != scoreboard.get(n))
				System.out.println("Score incorrect for " + n + ".");
    }

    @Override
    public String sayName() { return names[0]; }
}