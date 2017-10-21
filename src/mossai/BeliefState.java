package mossai;

import java.util.Arrays;
import java.util.List;

/** Encapsulation of the uncertain game state. */
public class BeliefState
{
    public static final int DEAL = 16;
    
    // Bitmasks for card locations.
    public static final int ACTIVE = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int OUT = 3;
    
    /** Bitmasks for each of the above locations. */
    private static final byte[] masks;
    
    static { masks = new byte[] {1, 2, 4, 8}; }
    
    /**
     * Represents the valid potential locations each card might occupy.
     * The bit corresponding to that location is set 1 if the card might be there.
     */
    private byte[] valid;
    
    /** Number of uncertain cards in each location. */
    private int[] pools;
    
    public BeliefState()
    {
        valid = new byte[Deck.DECK_SIZE];
        Arrays.fill(valid, (byte)15);
        pools = new int[] {DEAL, DEAL, DEAL, 4};
    }
    
    /** Return the probability of an opponent holding a certain card. */
    public double chance(Card c, int loc)
    {
        int i = Deck.cardToInt(c);
        byte locMask = masks[loc];
        
        // If this is a non valid location for the card.
        if((valid[i] & locMask) == 0) return 0.0;
        // If this is the only valid location for the card.
        else if(valid[i] == locMask) return 1.0;
        else
        {
            double numerator = 0.0;
            double denominator = 0.0;
            
            for(int j = 0; j < 4; j++)
            {
                byte m = masks[j];
                
                if(m == locMask) numerator = (double)pools[j];
                if((valid[i] & m) != 0) denominator += (double)pools[j];
            }
            
            return numerator / denominator;
        }
    }
    
    /** Update the belief when a card is played from a location. */
    public void cardPlayed(Card c, int loc, Card lead)
    {
        int i = Deck.cardToInt(c);
        byte locMask = masks[loc];
        
        // If the card wasn't known to be in that player's hand, there is now
        // one less unknown card there.
        if(valid[i] != locMask) pools[loc]--;
        // The card is now known to be out of play.
        valid[i] = masks[OUT];
        
        // TODO: Invalidate cards in suit if lead not followed.
        //       Reduce pools if any cards gained certain location.
    }
    
    public void includeHand(List<Card> hand, List<Card> discard, int player)
    {
        pools[player] = 0;
        
        for(Card c : hand)
        {
            int i = Deck.cardToInt(c);
            valid[i] = masks[player];
        }
        
        if(!discard.isEmpty())
        {
            pools[OUT] = 0;
            
            for(Card c : discard)
            {
                int i = Deck.cardToInt(c);
                valid[i] = masks[OUT];
            }
        }
        
        // TODO: Reduce pools of other players if any cards gained certain location.
    }
}
