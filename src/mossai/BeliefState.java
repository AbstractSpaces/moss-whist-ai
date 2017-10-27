package mossai;

import java.util.Arrays;
import java.util.Random;

/** Encapsulation of subjective data from a certain perspective. */
final class BeliefState
{
    /** Macro for setting a card's location completely unknown. */
    private static final int ALL_LOCS = (1 << 6) - 1;
    
    /** The fifth bit is set 1 until a card's location is certain. */
    private static final int TBC = 5;
    
    /** Macro for setting all cards of a suit potentially occupying a location. */
    private static final int ALL_RANKS = (1 << (Game.SUIT_SIZE + 1)) - 1;
    
    /** Macro for checking number of unknown cards. */
    private static final int UNKNOWN = 4;
    
    /** The viewpoint from which this belief is defined. */
    private final int viewer;
    
    /*
     * By using both of the below representations, we can perform most llokup
     * operations without the need for loops.
     */
    
    /**
     * Represents the potential locations each card might occupy.
     * The fifth bit is set 1 until the location is certain.
     */
    private final int[] locs;
    
    /**
     * Represents the potential cards that might be in each location.
     * Each cards[i] relates to location i.
     * For j = 0 to 3, cards[i][j] represents the potential cards of suit j.
     * cards[i][4] stores the number of unknown cards in location c.
     */
    private final int[][] cards;
    
    /** Create a blank belief for a new game. */
    BeliefState()
    {
        viewer = Game.OUT;
        
        locs = new int[Game.DECK_SIZE];
        Arrays.fill(locs, ALL_LOCS);
        
        cards = new int[4][5];
        for(int i = 0; i < cards.length; i++)
        {
            Arrays.fill(cards[i], ALL_RANKS);
            cards[i][UNKNOWN] = Game.DEAL;
        }
        cards[Game.OUT][UNKNOWN] = Game.DISCARDS;
    }
    
    /**
     * Incorporate a player's knowledge into an objective history to form a
     * belief for them.
     */
    BeliefState(int v, BeliefState history, int[] cardState)
    {
        viewer = v;
        locs = Arrays.copyOf(history.locs, Game.DECK_SIZE);
        
        cards = new int[history.cards.length][history.cards[0].length];
        for(int i = 0; i < cards.length; i++)
			cards[i] = Arrays.copyOf(history.cards[i], cards[0].length);
		
        cards[viewer][UNKNOWN] = 0;
        if(viewer == Game.LEADER)
			cards[Game.OUT][UNKNOWN] = 0;
        
        // With new knowledge we can perform some invalidations and fill in the hand.
        for(int c = 0; c < Game.DECK_SIZE; c++)
        {
            // If the card is in the hand.
            if(cardState[c] == viewer)
				locs[c] = 1 << viewer;
            // If the hand was a potential location but now isn't.
            else if(maybeHas(Game.intToCard(c), viewer))
            {
                locs[c] -= 1 << viewer;
                cards[viewer][Game.intToSuit(c)] -= 1 << Game.intToRank(c);
            }
            
            // The leader has no uncertainty over discarded cards.
            if(viewer == Game.LEADER)
            {
                if(cardState[c] == Game.OUT)
					locs[c] = 1 << Game.OUT;
                // If the card was thought discarded but wasn't.
                else if(maybeHas(Game.intToCard(c), Game.OUT))
                {
                    locs[c] -= 1 << Game.OUT;
                    cards[Game.OUT][Game.intToSuit(c)] -= 1 << Game.intToRank(c);
                }
            }
            
            // See if card location newly confirmed.
            confirm(c);
        }
    }
    
    /** Create a clone of an existing BeliefState. */
    BeliefState(BeliefState old)
    {
        viewer = old.viewer;
        locs = Arrays.copyOf(old.locs, Game.DECK_SIZE);
        
        cards = new int[old.cards.length][old.cards[0].length];
        for(int i = 0; i < cards.length; i++)
			cards[i] = Arrays.copyOf(old.cards[i], cards[0].length);
    }
    
    /** Return the probability of a card being in a location. */
    double chance(Card c, int loc)
    {
        if(!tbc(c))
        {
            if(certain(c, loc))
                return 1.0;
            else
                return 0.0;
        }
        else
        {
            double numerator = 0.0;
            double denominator = 0.0;
            
            // Scan through the locations.
            for(int l = 0; l < 4; l++)
            {
                if(l == loc)
                    numerator = (double)cards[l][UNKNOWN];
                // We only want to consider the unknown cards in a location
                // if the card in question might be located there.
                // If l is a valid location for the card.
                if(maybeHas(c, l))
                    denominator += (double)cards[l][UNKNOWN];
            }
            
            return numerator / denominator;
        }
    }
    
    /** Update the belief when a card is played. */
    void cardPlayed(Card c, int loc, Card lead)
    {   
        // The card is now known to be out of that hand.
        cards[loc][Game.cardToSuit(c)] -= 1 << Game.cardToRank(c);
        
        if(loc == viewer)
            locs[Game.cardToInt(c)] = 1 << Game.OUT;
        // If the viewer has received new knowledge.
        else
        {
            // If the card wasn't known to be in that player's hand, there is
            // now one less unknown card there.
            if(tbc(c))
                cards[loc][UNKNOWN]--;
            
            locs[Game.cardToInt(c)] = 1 << Game.OUT;

            // If the player didn't follow the lead suit, infer that they have
            // none of that suit.
            if(lead != null && c.suit != lead.suit && c.suit != Game.TRUMP)
            {
                // Remove all cards in the suit from the belief about their hand.
                cards[loc][Game.cardToSuit(c)] = 0;
				
                // Iterate through cards in the lead suit.
                for(int i = Game.suitBegins(lead.suit); i <= Game.suitEnds(lead.suit); i++)
                {
                    // If the card hasn't already been invalidated.
                    if(maybeHas(Game.intToCard(i), loc))
                    {
                        // Invalidate the card from this player's hand.
                        locs[i] -= 1 << loc;
                        confirm(i);
                    }
                }
            }
        }
    }
    
    /** Derive a sample game state from the belief. */
    int[] sampleState()
    {
        Random gen = new Random();
        int[] sample = new int[Game.DECK_SIZE];
        
        // The difference between these bounds acts as the weighting for the
        // probability of a card being assigned to a location.
        double upperBound;
        double lowerBound = 0.0;
        
        // Assign a location to each card in the deck.
        for(int c = 0; c < Game.DECK_SIZE; c++)
        {
            double p = gen.nextDouble();
            
            // Consider the probability of the card being in each location.
            for(int l = 0; l < 4; l++)
            {
                // We can skip forward if the location is certain.
                if(certain(Game.intToCard(c), l))
                {
                    sample[c] = l;
                    break;
                }
                // We can skip ahead if this is an invalid location.
                else if(maybeHas(Game.intToCard(c), l))
                {
                    upperBound = lowerBound + chance(Game.intToCard(c), l);

                    // We have already ruled out p < lowerBound, so if
                    // p < upperBound it falls between the two bounds.
                    if(p < upperBound)
                    {
                        sample[c] = l;
                        break;
                    }
                    else
                        lowerBound = upperBound;
                }
            }
        }
        
        return sample;
    }
	
	/** Return the highest card of a suit likely to be located somewhere. */
    Card highest(Suit s, int loc)
    {
        if(maybeHas(s, loc))
            for(int c = Game.suitEnds(s); c >= Game.suitBegins(s); c--)
                if(chance(Game.intToCard(c), loc) > Raptor.POSITIVE)
                    return Game.intToCard(c);
		
        return null;
    }
    
    /** Return the lowest card of a suit likely to be located somewhere. */
    Card lowest(Suit s, int loc)
    {
        if(maybeHas(s, loc))
            for(int c = Game.suitBegins(s); c <= Game.suitEnds(s); c++)
                if(chance(Game.intToCard(c), loc) > Raptor.POSITIVE)
                    return Game.intToCard(c);
		
        return null;
    }
    
    Card maxCard(Card a, Card b)
    {
        if(a != null)
        {
            if(b != null)
                if(a.rank > b.rank)
                    return a;
                else
                    return b;
            return a;
        }
        return b;
    }
	
    Card minCard(Card a, Card b)
    {
        if(a != null)
        {
            if(b != null)
                if(a.rank > b.rank)
                    return b;
                else
                    return a;
			
            return a;
        }
        return b;
    }
    
    Card lowest(int loc)
    {
        return minCard(minCard(BeliefState.this.lowest(Suit.HEARTS, loc), BeliefState.this.lowest(Suit.DIAMONDS, loc)), BeliefState.this.lowest(Suit.CLUBS, loc));
    }
    Card highest(int loc)
    {
        return maxCard(maxCard(BeliefState.this.highest(Suit.HEARTS, loc), BeliefState.this.highest(Suit.DIAMONDS, loc)), maxCard(BeliefState.this.highest(Suit.CLUBS, loc), BeliefState.this.highest(Suit.SPADES, loc)));
    }
    
    /**
     * Return true if a location is considered to have a higher ranked card in
	 * the same suit.
     */
    boolean hasHigher(Card c, int loc)
    {
		if(loc == viewer)
			return cards[viewer][Game.cardToSuit(c)] > (1 << Game.cardToRank(c));
        // A quick check to see if there is even a chance of returning true.
		else if(cards[loc][Game.cardToSuit(c)] > (1 << Game.cardToRank(c)))
            for(int i = Game.cardToInt(c); i <= Game.suitEnds(c.suit); i++)
                if(chance(c, loc) > Raptor.POSITIVE)
                    return true;
		
        return false;
    }
	
    /** Returns true if a card is considered to be in a certain location. */
    boolean has(Card c, int loc)
	{
		if(loc == viewer)
			return locs[Game.cardToInt(c)] == (1 << viewer);
		else if(maybeHas(c, loc) && chance(c, loc) > Raptor.POSITIVE)
			return true;
		else
			return false;
	}
	
    /**
	 * Returns true if a location is considered to contain at least one card of
	 * a certain suit.
	 */
    boolean has(Suit s, int loc)
	{
		if(loc == viewer)
			return cards[viewer][Game.suitToInt(s)] > 0;
		else if(BeliefState.this.highest(s, loc) != null)
			return true;
		else
			return false;
	}
	
	/** Returns true if the card might be in the given location. */
    private boolean maybeHas(Card c, int loc) { return (locs[Game.cardToInt(c)] & (1 << loc)) != 0; }
    
    /** Returns true if there might be any cards of the given suit in a location. */
    private boolean maybeHas(Suit s, int loc) { return cards[loc][Game.suitToInt(s)] > 0; }
	
	/** Returns true if a card is definitely in a location. */
    private boolean certain(Card c, int loc) { return locs[Game.cardToInt(c)] == (1 << loc); }
    
	/** Returns true if a card's location is still in doubt. */
    private boolean tbc(Card c) { return (locs[Game.cardToInt(c)] & (1 << TBC)) != 0; }
    
    /** See if new knowledge allows confirming a card's location. */
    private void confirm(int c)
    {
        // If the card was already confirmed, skip the checks.
        if(tbc(Game.intToCard(c)))
        {
            switch(locs[c] - (1 << TBC))
            {
                case Game.LEADER:
                    locs[c] = Game.LEADER;
                    cards[Game.LEADER][UNKNOWN]--;
                    break;
                case 1 << Game.LEFT:
                    locs[c] = 1 << Game.LEFT;
                    cards[Game.LEFT][UNKNOWN]--;
                    break;
                case 1 << Game.RIGHT:
                    locs[c] = 1 << Game.RIGHT;
                    cards[Game.RIGHT][UNKNOWN]--;
                    break;
                case 1 << Game.OUT:
                    locs[c] = 1 << Game.OUT;
                    cards[Game.OUT][UNKNOWN]--;
                    break;
            }
        }
    }
}
