package mossai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** An agent (hopefully) capable of intelligently playing Moss Side Whist. */
public class Raptor implements MSWAgent
{
    /** The agent's place relative to the leader. */
    private int pos;
    
    /** Names of players, left to right from the agent. */
    private String[] names;
    
    /** Map of player names to their positions. */
    private HashMap<String, Integer> players;
    
    private GameState state;
    
    /**
     * The agent's belief about the opponent's cards. Storing between tricks
     * improves performance.
     */
    private BeliefState belief;
    
    /**
     * A non-player specific belief from which the opponent's perspectives can
     * be derived.
     */
    private BeliefState history;
    
    // Parameters for modification via reinforcement learning.
    /** Bias constant for Monte Carlo play outs. */
    public final int bias;
    
    /** Number of sample states to run Monte Carlo tree search on. */
    private final int MC_runs;
    
    /** Number of play outs to sample in each Monte Carlo search. */
    private final int MC_samples;
    
    /** The probability threshold above which to treat as certain that an opponent possesses a card. */
    private final double positive;
    
    /** The probability threshold below which to treat as certain that an opponent does not possess a card. */
    private final double negative;
    
    public Raptor(int b, int mcr, int mcs, double p, double n)
    {
        pos = -1;
        names = new String[] {"Clever Girl", "", ""};
        players = new HashMap(3);
        state = null;
        belief = null;
        history = new BeliefState();
        
        bias = b;
        MC_runs = mcr;
        MC_samples = mcs;
        positive = p;
        negative = n;
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
        pos = order;
        state = new GameState(order);
        for(int i = 0; i < 3; i++) players.put(names[i], (order+i)%3);
        belief = new BeliefState(order, history, Game.CardstoInts(deal, order));
    }

    @Override
    public Card[] discard()
    {
        // TODO: Discard strategy.
        // Call belief.cardPlayed for each discarded card.
    }

    @Override
    public Card playCard()
    {
        // TODO: Generate multiple samples from belief.
        // Run MonteCarlo on each, select move with the highest average success across all samples.
        // Called belief.cardplayed and history.cardPlayed.
        // Update state.
    }

    @Override
    public void seeCard(Card card, String agent)
    {
        belief.cardPlayed(card, players.get(agent), state.getLead());
        history.cardPlayed(card, players.get(agent), state.getLead());
        state = new GameState(state, card);
    }

    @Override
    public void seeResult(String winner)
    {
        // Verify that state is working properly.
        if(state.getFirst() != players.get(winner)) System.out.println("Trick over but state not set properly.");
    }

    @Override
    public void seeScore(Map<String, Integer> scoreboard)
    {
        // Verify that state is working properly.
        int[] scores = state.getScores();
        
        for(String n : names)
        {
            if(scores[players.get(n)] != scoreboard.get(n))
            {
                System.out.println("Score incorrect for " + n + ".");
            }
        }
    }

    @Override
    public String sayName() { return names[0]; }
    
    public int getLeader() { return pos; }
}