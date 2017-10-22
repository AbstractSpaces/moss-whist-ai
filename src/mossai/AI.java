/** Needs total rework. */
package mossai;

import java.util.List;
import java.util.Map;

public class AI implements MSWAgent
{
    /** The agent's place in the turn order. */
    private int pos;
    
    /**
     * The agent's belief about the opponent's cards, as well as the predicted
     * beliefs of the opponents about the agent's cards.
     */
    private BeliefState[] beliefs;
    
    /**
     * Representation of the agent's hand, with elements set true for each card
     * in possession.
     */
    private boolean hand[];
    
    @Override
    public void setup(String agentLeft, String agentRight)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void seeHand(List<Card> hand, int order)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Card[] discard()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Card playCard()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void seeCard(Card card, String agent)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void seeResult(String winner)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void seeScore(Map<String, Integer> scoreboard)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String sayName()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}