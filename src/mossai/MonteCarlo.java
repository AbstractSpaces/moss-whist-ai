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
        private boolean[][] deckState;
        
        /** The beliefs held by the agent and the two simulated opponents. */
        private BeliefState[] beliefs;
        
        /** Cards played s far this trick. */
        private Card[] table;
        
        private int playthroughs;
        private int wins;
        private Node[] children;
        
        /** Start a new tree from a sampled state. */
        private Node(int p, int[] scores, boolean[][] state, BeliefState[] history)
        {
            agentPos = p;
            scores = Arrays.copyOf(scores, 3);
            
            // We can't construct beliefs with just the current state, we need
            // the history of the game up to this point.
            // If it turns out we don't need to modify history, we can remove
            // the cloning and gain some performance increase.
            beliefs = new BeliefState[3];
            for(int i = 0; i < 3; i++) beliefs[i] = history[i].clone();
            
            playthroughs = 0;
            wins = 0;
            children = null;
        }
        
        /** Generate the children of a node on the tree. */
        private void expand()
        {
            // Enumerate legal moves, initialise child array to correct size.
            // Clone the history, state, etc. and modify them with the move
            // represented by the each child.
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
