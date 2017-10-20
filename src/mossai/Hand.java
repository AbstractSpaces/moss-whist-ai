package mossai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Encapsulates data pertaining to the agent's hand. */
public class Hand
{
    /** A list of possible cards, set true if the player is holding that card. */
    private boolean[] handCards;
    
    /** Construct a hand based on cards given by the dealer. */
    public Hand(List<Card> deal)
    {
        handCards = new boolean[Deck.DECK_SIZE];
        for(Card c : deal) handCards[Deck.cardToInt(c)] = true;
    }
    
    public boolean[] getCards() { return Arrays.copyOf(handCards, Deck.DECK_SIZE); }
    
    /** Retrieve cards belonging to a given suit. */
    public ArrayList<Card> getSuit(Suit s)
    {   
        ArrayList<Card> suitCards = new ArrayList(Deck.SUIT_SIZE);
        
        for(int i : Deck.suitRange(s))
        {
            if(handCards[i]) suitCards.add(Deck.intToCard(i));
        }
        
        return suitCards;
    }
    
    /** Called when the agent plays a card. */
    public void removeCard(Card c) { handCards[Deck.cardToInt(c)] = false; }
}
