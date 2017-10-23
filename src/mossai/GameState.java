package mossai;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Encapsulation of the concrete knowledge about the game's state at a point in time.
 * @author dj
 */
public class GameState
{
    private static Raptor agent;
    /** The player going first this trick. */
    private int first;
    /** The player currently taking their turn, having not yet played their card. */
    private int turn;
    
    /** The cards so far played by each player this trick. */
    private final Card[] table;
    
    /** Running tally of the scores. */
    private final int[] scores;
    
    /** Construct a blank state for a new game. */
    public GameState(Raptor ai, int leader)
    {
        agent= ai;
        first = leader;
        turn = first;
        table = new Card[3];
        scores = new int[] {-8, -4, -4};
    }
    
    /** Copy a state then advance it by one move. */
    public GameState(GameState prev, Card played)
    {
        first = prev.first;
        turn = prev.turn;
        table = Arrays.copyOf(prev.table, 3);
        scores = Arrays.copyOf(prev.scores, 3);
        advance(played);
    }
    
    public int getFirst() { return first; }
    
    public int[] getScores() { return Arrays.copyOf(scores, 3); }
    
    public Card getLead() { return table[first]; }
    
    /** Move the state forward by one turn. */
    private void advance(Card played)
    {
        table[turn] = played;
        turn = (turn + 1) % 3;
        
        // If trick over, update score, clear table, set first and turn to winner.
        if(turn == first)
        {
            int best = first;
            
            for(int i = 1; i < 3; i++)
            {
                int p = (first+i)%3;
                boolean beat = false;
                
                if(table[best].suit == Game.TRUMP)
                {
                    if(table[p].suit == Game.TRUMP && table[p].rank > table[best].rank) beat = true;
                }
                else if(table[p].suit == table[best].suit && table[p].rank > table[best].rank) beat = true;
                
                if(beat) best = p;
            }
            
            Arrays.fill(table, null);
            scores[best]++;
            first = best;
        }
    }
    
   /**
    * Encapsulates the state of a simulated game for use with Monte Carlo.
    * @author Dylan Johnson
    */
    private class Simulation
    {
        /** The move that lead from the parent node to this one. */
        private final Card prevMove;
        
        private GameState sState;
        
        /** The beliefs held by the agent and the two simulated opponents. */
        private BeliefState[] sBeliefs;

        private double playthroughs;
        private double wins;
        private ArrayList<Simulation> children;

        /** Start a new tree from a sampled state. */
        private Simulation(int[] sCards, GameState state, BeliefState belief, BeliefState history)
        {
            prevMove = null;
            sState = state;
            sBeliefs = new BeliefState[3];

            for(int i = 0; i < 3; i++)
            {
                if(i == agent.getPos()) sBeliefs[i] = belief.clone();
                else sBeliefs[i] = new BeliefState(i, history, sCards);
            }

            playthroughs = 0.0;
            wins = 0.0;
            children = null;
       }
        
        /** Clone a previous node's state and enact a move by the agent. */
        private Simulation(Simulation prev, Card move)
        {
            prevMove = move;
            sState = new GameState(prev.sState, prevMove);
            sBeliefs = new BeliefState[3];
            for(int i = 0; i < 3; i++)
            {
                sBeliefs[i] = prev.sBeliefs[i].clone();
                sBeliefs[i].cardPlayed(move, agent.getPos(), prev.sState.table[prev.sState.first]);
            }
            playthroughs = 0.0;
            wins = 0.0;
            children = null;
        }

       /** Generate the children of a node on the tree. */
       private void expand()
       {
           // Enumerate legal moves, initialise child array to correct size.
           // Clone the node and modify it with the move represented by each child.
           // Predict the two opponent moves after that and modify the child.
           // with those moves.
           // Update score and table as appropriate.
           if(children == null)
           {
               children = new ArrayList();
               ArrayList<Card>[] hand = sBeliefs[agent.getPos()].getHand();
               
               // Determine the suits from which cards can be played.
               int[] legal;
               int leadSuit = Game.suitToInt(sState.table[first].suit);
               
               if(agent.getPos() == sState.first || hand[leadSuit].isEmpty())
               {
                   legal = new int[] {0, 1, 2, 3};
               }
               else legal = new int[] {leadSuit};
               
               for(int s : legal)
               {
                   for(Card c : hand[s])
                   {
                       Simulation child = new Simulation(this, c);
                       // Simulate predicted opponent moves.
                       child.predictOpponent();
                       child.predictOpponent();
                       children.add(child);
                   }
               }
           }
       }
       
       /** By assuming a fixed greedy opponent strategy, attempt to anticipate their move from this state. */
       private void predictOpponent()
       {
           if(sState.turn != agent.getPos())
           {
               
           }
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
       
       private double UTC(double p)
       {
           return wins / playthroughs + agent.bias * Math.sqrt(Math.log(p) / playthroughs);
       }
    }
}
