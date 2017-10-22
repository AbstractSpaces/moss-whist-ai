/** Needs total rework. */
package mossai;

import java.util.List;
import java.util.Map;

public class AI implements MSWAgent
{
    /** The agent's place in the turn order. */
    private int pos;
    
    /**
     * The agent's belief about the opponent's cards. Storing between tricks
     * improves performance.
     */
    private BeliefState belief;
    
    /**
     * A non-player specific belief from which the opponent's perspectives can
     * be derived.
     */
    private BeliefState history;
    
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