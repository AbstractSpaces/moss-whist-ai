package mossai;

import java.util.ArrayList;
import java.util.Arrays;
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
    private final int bias;
    
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
    }

    @Override
    public void seeCard(Card card, String agent)
    {
        belief.cardPlayed(card, players.get(agent), table.get(0));
        history.cardPlayed(card, players.get(agent), table.get(0));
    }

    @Override
    public void seeResult(String winner)
    {
        
    }

    @Override
    public void seeScore(Map<String, Integer> scoreboard)
    {
        
    }

    @Override
    public String sayName() { return names[0]; }
    
    /**
    * Encapsulates the state of a simulated game for use with Monte Carlo.
    * @author Dylan Johnson
    */
    private class Simulation
    {
       /** The beliefs held by the agent and the two simulated opponents. */
       private BeliefState[] sBeliefs;

       private ArrayList<Card> sTable;
       private int[] sScores;
       private int sFirst;

       private double playthroughs;
       private double wins;
       private ArrayList<Simulation> children;

       /** Start a new tree from a sampled state. */
       private Simulation(int[] sState, int f)
       {
           sScores = Arrays.copyOf(scores, 3);
           sBeliefs = new BeliefState[3];

           for(int i = 0; i < 3; i++)
           {
               if(i == pos) sBeliefs[i] = belief.clone();
               else sBeliefs[i] = new BeliefState(i, history, sState);
           }

           sTable = new ArrayList(3);
           
           sFirst = f;
           
           playthroughs = 0.0;
           wins = 0.0;
           children = null;
       }

       /** Generate the children of a node on the tree. */
       private void expand()
       {
           // Enumerate legal moves, initialise child array to correct size.
           // Clone the node and modify it with the move represented by each child.
           // Predict the two opponent moves after that and modify the child
           // with those moves.
           // Update score and table as appropriate.
       }

       /** Choose a child node as the next in a play out. */
       private int next()
       {
           // Game over, roll back up the tree.
           if(children.isEmpty()) return -1;
           else
           {
               int best = 0;
               double max = 0;
               
               for(int i = 0; i < children.size(); i++)
               {
                   double u = children.get(i).UTC(playthroughs);
                   
                   if(u > max)
                   {
                       max = u;
                       best = i;
                   }
               }
               
               return best;
           }
       }
       
       private double UTC(double p) { return wins / playthroughs + bias * Math.sqrt(Math.log(p) / playthroughs); }
    }
}