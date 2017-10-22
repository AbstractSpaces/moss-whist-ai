package mossai;

import java.util.Arrays;
import java.util.Random;

/**
 * Encapsulation of the uncertain game state.
 * @author Dylan Johnson
 */
public class BeliefState
{
    public static final int DEAL = 16;
    
    // Bitmasks for card locations.
    public static final int LEADER = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int OUT = 3;
    
    /** Bitmasks for each of the above locations. */
    private static final byte[] masks;
    
    static { masks = new byte[] {1, 2, 4, 8}; }
    
    /** The player to which this belief belongs. */
    public final int viewer;
    
    /**
     * Represents the valid potential locations each card might occupy.
     * The bit corresponding to that location is set 1 if the card might be there.
     */
    private final byte[] valid;
    
    /** Number of uncertain cards in each location. */
    private final int[] unknowns;
    
    /** Create a blank belief for a new game. */
    public BeliefState(int v, boolean[][] state)
    {
        viewer = v;
        valid = new byte[Deck.DECK_SIZE];
        
        // Invalidate cards from known invalid locations.
        for(int i = 0; i < Deck.DECK_SIZE; i++)
        {   
            // The card is in the viewer's hand.
            if(state[viewer][i]) valid[i] = masks[viewer];
            // The viewer knows about the discards.
            else if(viewer == LEADER)
            {
                // The card was discarded.
                if(state[OUT][i]) valid[i] = masks[OUT];
                // The card is in one of the opponent's hands.
                else valid[i] = (byte)(masks[(viewer+1)%3] + masks[(viewer+2)%3]);
            }
            // The card could be anywhere else.
            else valid[i] = (byte)(15 - masks[viewer]);
        }
        
        unknowns = new int[] {DEAL, DEAL, DEAL, 4};
        unknowns[viewer] = 0;
        if(viewer == LEADER) unknowns[OUT] = 0;
    }
    
    /** Create a clone of an existing BeliefState. */
    private BeliefState(BeliefState old)
    {
        viewer = old.viewer;
        valid = Arrays.copyOf(old.valid, Deck.DECK_SIZE);
        unknowns = Arrays.copyOf(old.unknowns, 4);
    }
    
    /** Update the belief when a card is played. */
    public void cardPlayed(Card c, int player, Card lead)
    {   
        int i = Deck.cardToInt(c);
        
        if(player == viewer) valid[i] = masks[OUT];
        // If the viewer has received new knowledge.
        else
        {
            // If the card wasn't known to be in that player's hand, there is
            // now one less unknown card there.
            if(valid[i] != masks[player]) unknowns[player]--;
            
            // The card is now known to be out of play.
            valid[i] = masks[OUT];

            // If the player didn't follow the lead suit, infer that they have
            // none of that suit.
            if(c.suit != Deck.TRUMP && lead != null && c.suit != lead.suit)
            {
                // Iterate through cards in the lead suit.
                for(int j : Deck.suitRange(lead.suit))
                {
                    // If the card hasn't already been invalidated.
                    if((valid[j] & masks[player]) != 0)
                    {
                        // Invalidate the card from this player's hand.
                        valid[j] -= masks[player];
                        
                        // See if there is only one remaining valid location
                        // for this card.
                        for(int loc = 0; loc < 4; loc++)
                        {
                            if(valid[loc] == masks[loc])
                            {
                                // There is one less unknown card here.
                                unknowns[loc]--;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    /** Derive a sample game state from the belief. */
    public boolean[][] sampleState()
    {
        Random gen = new Random();
        boolean[][] state = new boolean[4][Deck.DECK_SIZE];
        
        // Assign a location to each card in the deck.
        for(Card c : Card.values())
        {
            int i = Deck.cardToInt(c);
            double p = gen.nextDouble();
            double upperBound = 0.0;
            double lowerBound = 0.0;
            
            // Consider the probability of the card being in each location.
            for(int loc = 0; loc < 4; loc++)
            {
                // We want to avoid imprecise comparisons of doubles when we
                // know the location for certain.
                if(valid[i] == masks[loc])
                {
                    state[loc][i] = true;
                    break;
                }
                // We can skip ahead if this is an invalid location.
                else if((valid[i] & masks[loc]) != 0)
                {
                    upperBound = chance(c, loc) + lowerBound;

                    // We have already ruled out p < lowerBound, so if
                    // p < upperBound it falls between the two bounds.
                    if(p < upperBound)
                    {
                        state[loc][i] = true;
                        break;
                    }
                    else lowerBound = upperBound;
                }
            }
        }
        
        return state;
    }
    
    /** Create a copy of a BeliefState for assigning to a tree Node. */
    @Override
    public BeliefState clone() { return new BeliefState(this); }
    
    /** Return the probability of a card being in a location. */
    private double chance(Card c, int loc)
    {
        int i = Deck.cardToInt(c);
        double numerator = 0.0;
        double denominator = 0.0;
        
        // Scan through the locations.
        for(int j = 0; j < 4; j++)
        {
            byte m = masks[j];
            if(m == masks[loc]) numerator = (double)unknowns[j];
            // If j is a valid location for the card.
            // We only want to consider the unknown cards in a location
            // if the card in question might be located there.
            if((valid[i] & m) != 0) denominator += (double)unknowns[j];
        }

        return numerator / denominator;
    }
}
