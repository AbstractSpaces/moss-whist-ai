package mossai;

import java.util.HashMap;

/**
 * Collection of utility data and methods relating to the structure and rules of
 * Moss Side Whist.
 * @author Dylan Johnson
 */
class Game
{
    // Data relating to the rules of MSW.
    static final int DEAL = 16;
    static final int DISCARDS = 4;
    static final Suit TRUMP = Suit.SPADES;
    
    static final int LEADER = 0;
    static final int LEFT = 1;
    static final int RIGHT = 2;
    static final int OUT = 3;
    
    // Data relating to the deck of cards.
    static final int DECK_SIZE = 52;
    static final int SUIT_SIZE = 13;
	static final int NUM_SUITS = 4;
	static final int TRUMP_INT;
    
    private static final HashMap<Suit, Integer> SUIT_MAP;
    private static final HashMap<Card, Integer> CARD_MAP;
    private static final Card[] CARD_ARRAY;
	private static final Suit[] SUIT_ARRAY;
    
    static
    {
		SUIT_MAP = new HashMap(4);
		SUIT_ARRAY = new Suit[4];
		int i = 0;
		
		for(Suit s : Suit.values())
		{
			SUIT_MAP.put(s, i);
			SUIT_ARRAY[i] = s;
			i++;
		}
		
		TRUMP_INT = SUIT_MAP.get(TRUMP);
        
        CARD_MAP = new HashMap(DECK_SIZE);
        CARD_ARRAY = new Card[DECK_SIZE];
        
        for(Card c : Card.values())
        {
            i = SUIT_SIZE * SUIT_MAP.get(c.suit) + c.rank - 2;
            CARD_MAP.put(c, i);
            CARD_ARRAY[i] = c;
        }
    }
    
    /** Take a suit, return an index for it. */
    static int suitToInt(Suit s) { return SUIT_MAP.get(s); }
	
	/** Take a suit index, return it corresponding suit. */
	static Suit suitIntToSuit(int s) { return SUIT_ARRAY[s]; }
    
    /** Take a suit, return the index of its lowest card. */
    static int suitBegins(Suit s) { return SUIT_MAP.get(s) * SUIT_SIZE; }
    
    /** Take a suit and return the index of its highest card. */
    static int suitEnds(Suit s) { return SUIT_MAP.get(s) * SUIT_SIZE + SUIT_SIZE - 1; }
    
    /** Take a card, return its index. */
    static int cardToInt(Card c) { return CARD_MAP.get(c); }
    
    /** Take a card, return its suit's index. */
    static int cardToSuit(Card c) { return SUIT_MAP.get(c.suit); }
    
    /** Take a card, return its rank's index. */
    static int cardToRank(Card c) { return c.rank - 2; }
    
    /** Take a card index, return its suit's index. */
    static int cardIntToSuit(int c) { return c / SUIT_SIZE; }
    
    /** Take a card, return its index. */
    static Card intToCard(int c) { return CARD_ARRAY[c]; }
    
    /** Take a card index, return its rank's index. */
    static int intToRank(int c) { return c % SUIT_SIZE; }
}
