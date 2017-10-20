package mossai;

import java.util.Arrays;

/** An omniscient snapshot of a game in play, for use in Monte Carlo search. */
public class SampleState
{
    private final int leader;
    private final boolean[][] hands;
    private final Card[] table;
    
    public final int turn;
    public final int score;
    
    /** Construct a sample from an agent and a belief state. */
    public SampleState(Hand agent, BeliefState distrib, Card[] t, int l, int s)
    {
        hands = new boolean[3][Deck.DECK_SIZE];
        hands[AI.AGENT] = agent.getCards();
        
        boolean[][] opponents = distrib.sampleHands();
        hands[AI.LEFT] = opponents[0];
        hands[AI.RIGHT] = opponents[1];
        
        table = t;
        leader = l;
        score = s;
    }
    
    /** Advance one turn to a new state. */
    public SampleState(SampleState old, Card move)
    {
        leader = old.leader;
        hands = new boolean[3][Deck.DECK_SIZE];
        table = old.table;
        turn = (old.turn + 1)%3;
        
        // TODO: Incorporate score update and table clearing between tricks.
        
        for(int i = 0; i < 3; i++)
        {
            hands[i] = Arrays.copyOf(old.hands[i], Deck.DECK_SIZE);
            
            if(i == old.turn)
            {
                hands[i][Deck.cardToInt(move)] = false;
                table[i] = move;
            }
        }
    }
    
    /**
     * Transform this state into the belief of one player about the hands of the
     * other two.
     */
    public Belief[] perspective()
    {
        
    }
    
    /** Predict an opponent's move from this state. */
    public Card opponentEval()
    {
        // TODO: Use perspective() combined with the greedy evaluation.
    }
}
