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
    
    /** The indices of players in their turn order for this trick. */
    private int[] order;
    
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
        order = new int[] {leader, (leader+1)%3, (leader+2)%3};
        turn = order[0];
        table = new Card[3];
        scores = new int[] {-8, -4, -4};
    }
    
    /** Copy a state. */
    private GameState(GameState old)
    {
        order= Arrays.copyOf(old.order, 3);
        turn = old.turn;
        table = Arrays.copyOf(old.table, 3);
        scores = Arrays.copyOf(old.scores, 3);
    }
    
    /** Use a fixed, greedy strategy to determine the active player's move from a state. */
    public static Card greedyEval(GameState state, BeliefState belief)
    {
        ArrayList<Card>[] hand = belief.getHand();
        
        // Evaluation for the first player.
        if(state.turn == state.order[0])
        {
            
        }
        else
        {
            Card winning = state.table[state.order[0]];
            Suit leadSuit = winning.suit;
            
            // Evaluation for the second player.
            if(state.turn == state.order[1])
            {

            }
            // Evaluation for the third player.
            else
            {
                
                
                // If the player has to follow suit.
                if(!hand[Game.suitToInt(leadSuit)].isEmpty())
                {

                }
            }
        }
    }
    
    /** Use Monte Carlo tree search to evaluate the best move from a state. */
    public static Card monteCarlo(GameState state, BeliefState belief, BeliefState history)
    {
        int[] sample = belief.sampleState();
        
        Simulation root = state.new Simulation(sample, belief, history);
        root.expand();
        
        // Run the search.
        for(int i = 0; i < agent.MCsamples; i++) root.playOut();
        
        // Identify the best card.
        Simulation best = root.children.get(0);
        for(Simulation child : root.children)
        {
            if(child.wins / child.playthroughs > best.wins / best.playthroughs) best = child;
        }
        return best.prevMove;
    }
    
    public GameState clone() { return new GameState(this); }
    
    public int getPlayer(int o) { return order[o]; }
    
    public int[] getScores() { return Arrays.copyOf(scores, 3); }
    
    public Card getLead() { return table[order[0]]; }

    
    /** Move the state forward by one turn. */
    public void advance(Card played)
    {
        table[turn] = played;
        turn = (turn + 1) % 3;
        
        // If trick over, update score, clear table, set order. 
        if(turn == order[0])
        {
            int best = order[0];
            
            for(int i = 1; i < 3; i++)
            {
                int p = order[i];
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
            order[0] = best;
            order[1] = (best + 1) % 3;
            order[2]= (best + 2) % 3;
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
        
        private GameState state;
        
        /** The beliefs held by the agent and the two simulated opponents. */
        private BeliefState[] beliefs;

        private double playthroughs;
        private double wins;
        private ArrayList<Simulation> children;

        /** Start a new tree from a sampled state. */
        private Simulation(int[] sCards, BeliefState belief, BeliefState history)
        {
            prevMove = null;
            state = GameState.this;
            beliefs = new BeliefState[3];

            for(int i = 0; i < 3; i++)
            {
                if(i == agent.pos) beliefs[i] = belief.clone();
                else beliefs[i] = new BeliefState(i, history, sCards);
            }

            playthroughs = 0.0;
            wins = 0.0;
            children = null;
       }
        
        /** Clone a previous node's state and enact a move by the agent. */
        private Simulation(Simulation prev, Card move)
        {
            prevMove = move;
            state = prev.state.clone();
            state.advance(move);
            
            beliefs = new BeliefState[3];
            for(int i = 0; i < 3; i++)
            {
                beliefs[i] = prev.beliefs[i].clone();
                beliefs[i].cardPlayed(move, agent.pos, prev.state.table[prev.state.order[0]]);
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
               ArrayList<Card>[] hand = beliefs[agent.pos].getHand();
               
               // Determine the suits from which cards can be played.
               int[] legal;
               
               int follow = -1;
               if(agent.pos != state.order[0])
               {
                   follow = Game.suitToInt(state.table[state.order[0]].suit);
                   if(hand[follow].isEmpty()) follow = -1;
               }
               
               if(follow == -1) legal = new int[] {0, 1, 2, 3};
               else legal = new int[] {follow};
               
               for(int s : legal)
               {
                   for(Card c : hand[s])
                   {
                       Simulation child = new Simulation(this, c);
                       
                       // Simulate predicted opponent moves.
                       for(int i = 1; i < 3; i++)
                       {
                           int opponent = (agent.pos + i) % 3;
                           child.state.advance(GameState.greedyEval(child.state, child.beliefs[opponent]));
                       }
                       
                       children.add(child);
                   }
               }
           }
       }

       /** Choose a child node as the next in a play out. */
       private boolean playOut()
       {   
           // Game over, roll back up the tree.
           if(children.isEmpty())
           {
               // See if the agent won the game.
               if(agent.drawWins) return scores[agent.pos] >= scores[(agent.pos+1)%3] && scores[agent.pos] >= scores[(agent.pos+2)%3];
               else return scores[agent.pos] > scores[(agent.pos+1)%3] && scores[agent.pos] > scores[(agent.pos+2)%3];
           }
           // Continue the play out.
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
               
               children.get(best).expand();
               
               if(children.get(best).playOut())
               {
                   wins++;
                   return true;
               }
               else return false;
           }
       }
       
       private double UTC(double p)
       {
           return wins / playthroughs + agent.bias * Math.sqrt(Math.log(p) / playthroughs);
       }
    }
}
