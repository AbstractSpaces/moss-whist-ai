package mossai;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/** Encapsulation of subjective data from a certain perspective. */
final class BeliefState
{
	/** The fifth bit is set 1 until a card's location is certain. */
    private static final int TBC = 5;
	
	/** Number of bits used to represent unknowns for each player. */
    private static final int UKB = 5;
	
	/** Map for converting location bit masks back to indexes. */
	private static final HashMap<Integer, Integer> LOC_MAP;
	
    /** Macro for setting a card's location completely unknown. */
    private static final int LOCS_MASK = (1 << 6) - 1;
	
	/** Macro for setting all cards as potentially occupying a location. */
    private static final long CARDS_MASK = ((long)1 << Game.DECK_SIZE + 1) - 1;
	
	/** 13 1 bits. */
	private static final int SUIT_MASK = (1 << Game.SUIT_SIZE + 1) - 1;
	
	/** 5 1 bits. */
	private static final int UK_MASK = (1 << UKB + 1) - 1;
    
	static
	{
		LOC_MAP = new HashMap(5);
		for(int i = 0; i < 6; i++)
			LOC_MAP.put(1 << i, i);
	}
    
    /** The viewpoint from which this belief is defined. */
    private final int viewer;
    
    /*
     * By using both of the below representations, we can perform most lookup
     * operations without the need for loops.
     */
    
    /**
     * Represents the potential locations each card might occupy.
     * The fifth bit is set 1 until the location is certain.
     */
    private final int[] locs;
    
    /** Represents the potential cards that might be in each location. */
    private final long[] cards;
	
	/** Represents the number of unknown cards in a location. */
	private int unknowns;
    
    /** Create a blank belief for a new game. */
    BeliefState()
    {
        viewer = Game.OUT;
        
        locs = new int[Game.DECK_SIZE];
        Arrays.fill(locs, LOCS_MASK);
        
        cards = new long[4];
        for(int i = 0; i < cards.length; i++)
			cards[i] = CARDS_MASK;
		
		unknowns = Game.DEAL + (Game.DEAL << UKB) + (Game.DEAL << UKB*2) + (Game.DISCARDS << UKB*3);
    }
    
    /**
     * Incorporate a player's knowledge into an objective history to form a
     * belief for them.
     */
    BeliefState(int v, BeliefState history, int[] cardState)
    {
        viewer = v;
        locs = Arrays.copyOf(history.locs, history.locs.length);
        cards = Arrays.copyOf(history.cards, history.cards.length);
		// Copy unknowns from history and set unknowns for viewer to 0.
		unknowns = reduceUK(viewer, getUK(viewer, history.unknowns), history.unknowns);
		// The leader also knows about discarded cards.
		if(viewer == Game.LEADER)
			unknowns = reduceUK(Game.OUT, getUK(Game.OUT));
		
/////////////////////////////// DEBUGGING //////////////////////////////////////		
		System.out.print(viewer + " constructed with: ");
////////////////////////////////////////////////////////////////////////////////        
        // With new knowledge we can perform some invalidations and fill in the hand.
        for(int c = 0; c < Game.DECK_SIZE; c++)
        {
            // If the card is in the hand.
            if(cardState[c] == viewer)
			{
/////////////////////////////// DEBUGGING //////////////////////////////////////
				System.out.print(Game.intToCard(c) + " ");
////////////////////////////////////////////////////////////////////////////////
				locs[c] = 1 << viewer;
			}
            // If the hand was a potential location but now isn't.
            else if(maybeHas(Game.intToCard(c), viewer))
            {
                locs[c] -= 1 << viewer;
                cards[viewer] -= 1 << c;
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
                    cards[Game.OUT] -= 1 << c;
                }
            }
            
            // See if card location newly confirmed.
            confirm(c);
        }
/////////////////////////////// DEBUGGING //////////////////////////////////////
		System.out.println();
////////////////////////////////////////////////////////////////////////////////
    }
    
    /** Create a clone of an existing BeliefState. */
    BeliefState(BeliefState old)
    {
        viewer = old.viewer;
        locs = Arrays.copyOf(old.locs, old.locs.length);
        cards = Arrays.copyOf(old.cards, old.cards.length);
        unknowns = old.unknowns;
    }
	
	/** Returns true if the viewer has a given card. */
	boolean has(Card c) { return locs[Game.cardToInt(c)] == (1 << viewer); }
	
	/** Returns true if the viewer has at least one card of a certain suit. */
	boolean has(Suit s) { return (cards[viewer] & (SUIT_MASK << Game.suitToInt(s))) != 0; }
	
		/** Returns true if the viewer has a higher ranked card in the same suit. */
	boolean hasHigher(Card c) { return (CARDS_MASK << Game.cardToSuit(c) & cards[viewer]) > (1 << Game.cardToInt(c)); }
	
	/**
	 * Returns a higher ranked card that the viewer has, in the same suit
	 * as the argument, or null.
	 */
	Card beat(Card c)
	{
		for(int i = Game.cardToInt(c); i <= Game.suitEnds(c.suit); i++)
			if(has(Game.intToCard(i)))
				return Game.intToCard(i);
		
		return null;
	}
	
	/** Returns true if a card is considered to be in a certain location. */
    boolean otherHas(Card c, int loc) { return maybeHas(c, loc) && chance(c, loc) > Raptor.POSITIVE; }
	
    /**
	 * Returns true if a location is considered to contain at least one card of
	 * a certain suit.
	 */
    boolean otherHas(Suit s, int loc) { return highestInOther(s, loc) != null; }
	
    /**
     * Returns true if a location is considered to have a higher ranked card in
	 * the same suit.
     */
    boolean otherHasHigher(Card c, int loc)
    {
		if((CARDS_MASK << Game.cardToSuit(c) & cards[loc]) > (1 << Game.cardToInt(c)))
            for(int i = Game.cardToInt(c); i <= Game.suitEnds(c.suit); i++)
                if(chance(c, loc) > Raptor.POSITIVE)
                    return true;
		
        return false;
    }
	
	/** Return the highestInOther card of a suit held by the viewer, or null. */
	Card highest(Suit s)
	{
		for(int c = Game.suitEnds(s); c >= Game.suitBegins(s); c--)
			if(has(Game.intToCard(c)))
				return Game.intToCard(c);
		
		return null;
	}
	
	/** Return the highest likely card of a suit in a location. */
    Card highestInOther(Suit s, int loc)
    {
        if(maybeHas(s, loc))
            for(int c = Game.suitEnds(s); c >= Game.suitBegins(s); c--)
                if(chance(Game.intToCard(c), loc) > Raptor.POSITIVE)
                    return Game.intToCard(c);
		
        return null;
    }
    
    /** Return the lowest card of a suit that the viewer holds, or null. */
    Card lowest(Suit s)
    {
/////////////////////////////// DEBUGGING //////////////////////////////////////
		System.out.println(s);
////////////////////////////////////////////////////////////////////////////////
		for(int c = Game.suitBegins(s); c <= Game.suitEnds(s); c++)
			if(has(Game.intToCard(c)))
				return Game.intToCard(c);
		
        return null;
    }
    
	/** Returns the lowest ranked card held by the viewer, optionally excluding trumps. */
    Card lowest(boolean noTrump)
    {
        Card low = null;
		
		for(Suit s : Suit.values())
		{
			if(!(s == Game.TRUMP && noTrump))
			{
				Card contest = lowest(s);

				if(contest != null && (low == null || contest.rank < low.rank))
					low = contest;
			}
		}
		
		return low;
    }
	
    /** Update the belief when a card is played. */
    void cardPlayed(Card c, int loc, Card lead)
    {   
        // The card is now known to be out of that hand.
        cards[loc] -= 1 << Game.cardToInt(c);
        
        if(loc == viewer)
            locs[Game.cardToInt(c)] = 1 << Game.OUT;
        // If the viewer otherHas received new knowledge.
        else
        {
            // If the card wasn't known to be in that player's hand, there is
            // now one less unknown card there.
            if(tbc(c))
                unknowns = reduceUK(loc, 1);
            
            locs[Game.cardToInt(c)] = 1 << Game.OUT;

            // If the player didn't follow the lead suit, infer that they have
            // none of that suit.
            if(lead != null && c.suit != lead.suit && c.suit != Game.TRUMP)
            {
                // Remove all cards in the suit from the belief about their hand.
                cards[loc] = CARDS_MASK - (SUIT_MASK << Game.cardToSuit(c));
				
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
        
		// The remaining number of uncertain cards for each location.
		int limits = unknowns;
		
		// Iterate through the deck to assign cards a location.
		for(int c = 0; c < Game.DECK_SIZE; c++)
		{
			// Assign the location for an already confirmed card.
			if(!tbc(Game.intToCard(c)))
				sample[c] = LOC_MAP.get(locs[c]);
			else
			{
				double p = gen.nextDouble();
				double bound = 0.0;
				
				for(int l = 0; l < 4; l++)
				{
					bound += chance(Game.intToCard(c), l, limits);
					
					if(p < bound)
					{
						sample[c] = l;
						limits = reduceUK(l, 1, limits);
						break;
					}
				}
			}
		}
/////////////////////////////// DEBUGGING //////////////////////////////////////
		for(int i = 0; i < 4; i++)
		{
			System.out.print(i + " sampled with: ");
			for(int j = 0; j < Game.DECK_SIZE; j++)
				if(i == sample[j])
					System.out.print(Game.intToCard(j) + " ");
			System.out.println();
		}
////////////////////////////////////////////////////////////////////////////////
        return sample;
    }
	
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
                    unknowns = reduceUK(Game.LEADER, 1);
                    break;
                case 1 << Game.LEFT:
                    locs[c] = 1 << Game.LEFT;
                    unknowns = reduceUK(Game.LEFT, 1);
                    break;
                case 1 << Game.RIGHT:
                    locs[c] = 1 << Game.RIGHT;
                    unknowns = reduceUK(Game.RIGHT, 1);
                    break;
                case 1 << Game.OUT:
                    locs[c] = 1 << Game.OUT;
                    unknowns = reduceUK(Game.OUT, 1);
                    break;
            }
        }
    }
	
	/** Return the probability of a card being in a location. */
	private double chance(Card c, int loc) { return chance(c, loc, unknowns); }
	
    private double chance(Card c, int loc, int uk)
    {
        if(!tbc(c))
            if(certain(c, loc))
                return 1.0;
            else
                return 0.0;
        else
        {
            double numerator = 0.0;
            double denominator = 0.0;
            
            // Scan through the locations.
            for(int l = 0; l < 4; l++)
            {
                if(l == loc)
                    numerator = (double)getUK(l, uk);
                // We only want to consider the unknown cards in a location
                // if the card in question might be located there.
                // If l is a valid location for the card.
                if(maybeHas(c, l))
                    denominator += (double)getUK(l, uk);
            }
            
            return numerator / denominator;
        }
    }
	
	/** Returns true if the card might be in the given location. */
    private boolean maybeHas(Card c, int loc) { return (locs[Game.cardToInt(c)] & (1 << loc)) != 0; }
    
    /** Returns true if there might be any cards of the given suit in a location. */
    private boolean maybeHas(Suit s, int loc) { return (cards[loc] & (SUIT_MASK << Game.suitToInt(s))) != 0; }
	
	/** Returns true if a card is definitely in a location. */
    private boolean certain(Card c, int loc) { return locs[Game.cardToInt(c)] == (1 << loc); }
    
	/** Returns true if a card's location is still in doubt. */
    private boolean tbc(Card c) { return (locs[Game.cardToInt(c)] & (1 << TBC)) != 0; }
	
	/** Retrieve the number of unknown cards in a location. */
	private int getUK(int loc) { return getUK(loc, unknowns); }
	
	private int getUK(int loc, int uk) { return (uk & (UK_MASK << UKB*loc)) >> UKB*loc; }
	
	/** Reduce the number of unknown cards in a location by a certain amount. */
	private int reduceUK(int loc, int num) { return reduceUK(loc, num, unknowns); }
	
	private int reduceUK(int loc, int num, int uk) { return uk -= num << UKB*loc; }
}
