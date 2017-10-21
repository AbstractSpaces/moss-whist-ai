package mossai;

import java.util.List;
import java.util.Random;

/** Encapsulation of the uncertain game state. */
public class BeliefState
{
    public static final int STD_DEAL = 16;
    public static final int LEAD_DEAL = 20;
    
    // Macros for card locations.
    public static final int ACTIVE = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int OUT = 3;
    
    /**
     * An array of the cards that could be in each location.
     * Element [i][j] refers to card i in location j.
     * -1 means confirmed not, 0 means maybe, 1 means confirmed yes.
     */
    private int[][] potential;
    
    /** Number of uncertain cards in each location. */
    private int[] pools;
    
    /** Return the probability of an opponent holding a certain card. */
    public double chance(Card c, int loc)
    {
        int p = potential[loc][Deck.cardToInt(c)];
        if(p < 0) return 0.0;
        else if(p > 0) return 1.0;
        else
        {
            // Number of cards in the location that could be this card.
            double num = pools[loc];
            // Number of cards overall that could be this card.
            double denom = 0.0;

            for(int i = 0; i < 4; i++)
            {
                if(potential[i][Deck.cardToInt(c)] == 0) denom += 1.0;
            }
            
            return num / denom;
        }
    }
    
    /** Update the belief when a card is played. */
    public void seeCard(Card c, int turn, Card lead)
    {
        int index = Deck.cardToInt(c);
        
        // Put in the discard pile.
        potential[OUT][index] = 1;
        // Remove potential card from all hands.
        for(int i = 0; i < 3; i++) potential[i][index] = -1;
        // Reduce number of cards in player's hand.
        pools[turn]--;
        confirm(c);

        // See if player followed the lead and infer further removals.
        if(c.suit != lead.suit)
        {
            for(int i : Deck.suitRange(lead.suit))
            {
                potential[turn][i] = -1;
                confirm(c);
            }
        }
    }
    
    /** See if a card's location can be inferred. */
    private void confirm(Card c)
    {
        int sum = 0;
        int loc = -1;
        
        // This will tell us if there is a confirmable location.
        // Also remember the location of that 0.
        for(int i = 0; i < 4; i++)
        {
            if(potential[i][Deck.cardToInt(c)] == 0) loc = i; 
            else sum += potential[i][Deck.cardToInt(c)];
        }
        
        if(sum == -3)
        {
            potential[loc][Deck.cardToInt(c)] = 1;
            pools[loc]--;
        }
    }
}
