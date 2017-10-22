package mossai;

import java.util.Arrays;

/** Class for generating a state tree and performing a Monte Carlo search on it. */
public class MonteCarlo
{
    private static class Node
    {
        /** Our agent's position in the play order. */
        private int agentPos;
        
        /** Running tally of the game score. */
        private int[] scores;
        
        /** The true state of the cards, unknown to any player. */
        private int[] deckState;
        
        /** The beliefs held by the agent and the two simulated opponents. */
        private BeliefState[] beliefs;
        
        /** Cards played so far this trick. */
        private Card[] table;
        
        private int playthroughs;
        private int wins;
        private Node[] children;
        
        /** Start a new tree from a sampled state. */
        private Node(int p, int[] scores, int[] state, BeliefState agentBelief, BeliefState history)
        {
            agentPos = p;
            scores = Arrays.copyOf(scores, 3);
            
            beliefs = new BeliefState[3];
            
            for(int i = 0; i < 3; i++)
            {
                if(i == agentPos) beliefs[i] = agentBelief.clone();
                else beliefs[i] = new BeliefState(i, history, state);
            }
            
            playthroughs = 0;
            wins = 0;
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
        
        /** Choose a child node as the next in a playout. */
        private int next()
        {
            // Use UCT function to select child based on playthroughs and wins.
        }
    }
    
    /**
     * Run Monte Carlo tree search from a sample state to evaluate the best
     * available move.
     */
    public static Card search(boolean[][] root)
    {
        
    }
    
    /** Simulate a game to its end, then back-propagate the result. */
    private static void playOut(Node start)
    {
        
    }
}
