package mossai;

import java.util.HashMap;

/**
 * Collection of data and methods for iterating through cards.
 * @author Dylan Johnson
 */
public class Deck
{
    public static final int DECK_SIZE = 52;
    public static final int SUIT_SIZE = 13;
    static final Suit TRUMP = Suit.SPADES;
    
    private static final HashMap<Suit, Integer> SUIT_MAP;
    private static final HashMap<Card, Integer> CARD_MAP;
    
    private static final Card[] CARD_ARRAY;
    
    static
    {
        SUIT_MAP = new HashMap(4);
        SUIT_MAP.put(Suit.HEARTS, 0);
        SUIT_MAP.put(Suit.CLUBS, SUIT_SIZE);
        SUIT_MAP.put(Suit.DIAMONDS, SUIT_SIZE*2);
        SUIT_MAP.put(Suit.SPADES, SUIT_SIZE*3);
        
        CARD_MAP = new HashMap(DECK_SIZE);
        CARD_ARRAY = new Card[DECK_SIZE];
        
        for(Card c : Card.values())
        {
            int value = SUIT_MAP.get(c.suit) + c.rank-2;
            CARD_MAP.put(c, value);
            CARD_ARRAY[value] = c;
        }
        
    }
    
    static int[] suitRange(Suit s)
    {
        int[] range = new int[SUIT_SIZE];
        
        for(int i = 0; i < SUIT_SIZE; i++) range[i] = SUIT_MAP.get(s) + i;
        
        return range;
    }
    
    static int suitToInt(Suit s) { return SUIT_MAP.get(s); }
    public static int cardToInt(Card c) { return CARD_MAP.get(c); }
    public static Card intToCard(int i) { return CARD_ARRAY[i]; }
}
