package mossai;

import java.util.List;

/** Encapsulates probabilities of an opponent holding certain cards. */
public class Belief
{
    /**
     * What we know about the opponent's hand.
     * Set true if we're certain they don't have that card.
     */
    private boolean[] ruledOut;
    
    /** Number of cards held but with unknown value. */
    private int unknown;
    
    /** Construct a belief based on cards to dealt to us. */
    public Belief(List<Card> deal, boolean agentLead)
    {
        ruledOut = new boolean[Deck.DECK_SIZE];
        unknown = BeliefState.STD_DEAL;;
        
        for(Card c : deal) ruledOut[Deck.cardToInt(c)] = true;
    }
    
    public int getUnknown() { return unknown; }
    
    /**
     * Return the probability of opponent holding a certain card.
     * @return An int from 0 to 100 representing probability percentage.
     */
    public int chance(Card c, int pool)
    {
        if(ruledOut[Deck.cardToInt(c)]) return 0;
        else return unknown * 100 / pool;
    }
    
    /** Called when a card is ruled out from an agent's hand. */
    public void remove(Card c)
    {
        if(!ruledOut[Deck.cardToInt(c)])
        {
            ruledOut[Deck.cardToInt(c)] = true;
            unknown--;
        }
    }
}
