/*
 *
 */
package mossai;

import java.util.*;

// git pull

// git pull -s ours

// git puls -s theirs

// git add .
// git commit
// git push origin master

public class AI implements MSWAgent
{
    /** An encapsulation of our knowledge/beliefs about an opposing agent. */
    private class Opponent
    {
        int pos;
        
        private String name;
        
        /**
         * Each belief element maps a card to the probability of the opponent
         * holding that card.
         */
        private HashMap<Card, Double> belief;
        
        // The number of unknown cards in the opponent's hand.
        private double unknowns;
        
        private Opponent() { belief = new HashMap(52); }
        
        /**
         * Initialise the belief probabilities using what we know about our own
         * hand.
         */
        private void init(List<Card> hand, int p)
        {
            pos = p;
            
            // Cards not in our hand have an even chance of being in this opponent's hand.
            if(pos == LEADER) unknowns = 20.0;
            else unknowns = 16.0;
            
            for(Card c : Card.values()) belief.put(c, unknowns/pool);
            for(Card h : hand) belief.put(h, 0.0);
        }

    }
    
    // Map for indexing into our hand array.
    private HashMap<Suit, Integer> suits;
    
    private String name;
    
    // Opponents are counted from our left.
    private Opponent[] opponents;
    
    // Our position in the order.
    private int pos;
    
    // Cards in our hand.
    private ArrayList<Card>[] hand;
    
    // Cards played so far this turn.
    private Card[] table;
    
    // The number of cards with uncertain location.
    private double pool;

    public AI()
    {
        suits = new HashMap();
        suits.put(Suit.HEARTS, 0);
        suits.put(Suit.CLUBS, 1);
        suits.put(Suit.DIAMONDS, 2);
        suits.put(Suit.SPADES, 3);
        
        name = "Halo";
        opponents = new Opponent[2];
        table = new Card[3];
        
        hand = new ArrayList[4];
        for(ArrayList suit : hand) suit = new ArrayList(13);
        pool = 52.0;
    }

    /**
     * Tells the agent the names of the competing agents, and their relative position.
     */
    public void setup(String agentLeft, String agentRight)
    {
        opponents[0].name = agentLeft;
        opponents[1].name = agentRight;
    }

    /**
     * Starts the round with a deal of the cards.
     * The agent is told the cards they have (16 cards, or 20 if they are the leader)
     * and the order they are playing (0 for the leader, 1 for the left of the leader, and 2 for the right of the leader).
     */
    public void seeHand(List<Card> deal, int order)
    {
        pos = order;
        pool -= deal.size();
        
        // Initialise beliefs about opponent hands.
        for(int i = 0; i < 2; i++) opponents[i].init(deal, (order+i)%3);
        
        // Sort cards based on rank before assigning them to hand.
        deal.sort(new CardCompare());
        
        // Insert ordered cards into hand.
        for(Card c : deal) hand[suits.get(c.suit)].add(c);
    }
    
    /**
     * This method will be called on the leader agent, after the deal.
     * If the agent is not the leader, it is sufficient to return an empty array.
     */
    public Card[] discard()
    {
        Card[] picks = new Card[4];
        int i = 0;
        
        if(pos == LEADER)
        {
            while(i < 4)
            {
                picks[i] = evalDiscard();
                hand[suits.get(picks[i].suit)].remove(picks[i]);
                i--;
            }
        }
        
        return picks;
    }
    
    /**
     * Evaluate cards in the hard for the best to discard.
     * @return The chosen card.
     */
    private Card evalDiscard()
    {
        // TODO: Write a formula than can improve through learning.
        return null;
    }

    // -------------------------------------------------------------------------
    /**
     * Agent returns the card they wish to play.
     * A 200 ms timelimit is given for this method
     * @return the Card they wish to play.
     */
    public Card playCard()
    {
        return null;
    }
    
    /**
     * Sees an Agent play a card.
     * A 50 ms timelimit is given to this function.
     * @param card, the Card played.
     * @param agent, the name of the agent who played the card.
     */
    public void seeCard(Card card, String agent)
    {
        
    }

    /**
     * See the result of the trick. 
     * A 50 ms timelimit is given to this method.
     * This method will be called on each eagent at the end of each trick.
     * @param winner, the player who played the winning card.
     */
    public void seeResult(String winner)
    {
    }
    
    /**
     * See the score for each player.
     * A 50 ms timelimit is givien to this method
     * @param scoreboard, a Map from agent names to their score.
     */
    public void seeScore(Map<String, Integer> scoreboard)
    {
    }

    /**
     * @return the Agents name.
     * A 10ms timelimit is given here.
     * This method will only be called once.
     */
    public String sayName()
    {
        return "Halo";//return names[0];
    }
    
    // ------------------------------------------------------------------------- 
     
    private void copyHand(List<Card> a, List<Card> b)
    {
        for(int i = 0; i < a.size(); i++)
        {
            b.add(a.get(i));
        }
    }
    
    private String printHand(List<Card> cards)
    {
        StringBuilder output = new StringBuilder();
        
        for(int i = 0; i < cards.size(); i++)
        {
            output.append(cards.get(i).toString() + ", ");
        }
        
        return output.toString();
    }
}
