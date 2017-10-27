package mossai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** An agent (hopefully) capable of intelligently playing Moss Side Whist. */
public class GreedyRaptor implements MSWAgent
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
    
    private GameState state;
    
    public GreedyRaptor() {}
    
    @Override
    public void setup(String agentLeft, String agentRight)
    {
        left = agentLeft;
        right = agentRight;
    }

    @Override
    public void seeHand(List<Card> deal, int order)
    {
        state = new GameState(order, deal);
    }

    @Override
    public Card[] discard()
    {
        if(state.getPos() == 0)
        {
            Card[] chosen = new Card[4];

            for(int i = 0; i < 4; i++)
            {
                chosen[i] = state.discardLow();
            }
            return chosen;
        }
        else return new Card[4];
    }
	
    @Override
    public Card playCard()
    {
	
        Card best = state.greedyEval();
        
        return best;
    }

    @Override
    public void seeCard(Card card, String agent) { state.advance(card); }

    @Override
    public void seeResult(String winner)
    {
    }

    @Override
    public void seeScore(Map<String, Integer> scoreboard)
    {
    }

    @Override
    public String sayName() { return ME; }
}