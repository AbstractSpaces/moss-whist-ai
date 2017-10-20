package mossai;

/** Class for generating a state tree and performing a Monte Carlo search on it. */
public class MonteCarlo
{
    private class Node
    {
        SampleState state;
        int playthroughs;
        int wins;
        Node[] children;
        
        /**
         * Generate the children of a node on the tree.
         * Will use state.opponentEval() to predict the two intermediate
         * opponent moves.
         */
        private void expand()
        {

        }
        
        /** Choose a child node as the next in a playout. */
        private int next(Node[] children)
        {

        }
    }
    
    /**
     * Run Monte Carlo tree search from a sample state to evaluate the best
     * available move.
     */
    public static Card search(SampleState root)
    {
        
    }
    
    /** Simulate a game to its end, then back-propagate the result. */
    private static void playOut(Node start)
    {
        
    }
}
