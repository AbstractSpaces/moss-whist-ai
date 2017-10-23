package mossai;

import java.util.ArrayList;

/**
 * Encapsulation of the concrete knowledge about the game's state at a point in time.
 * @author dj
 */
public class GameState
{
    /** The player going first this trick. */
    private final int first;
    /** The player currently taking their turn. */
    private int turn;
    
    /** The cards so far played by each player this trick. */
    private final int[] table;
    
    /** Running tally of the scores. */
    private final int[] scores;
    
    /** Construct a blank state for a new game. */
    public GameState(int leader)
    {
        first = leader;
        turn = first;
        table = new int[3];
        scores = new int[] {-8, -4, -4};
    }
}
