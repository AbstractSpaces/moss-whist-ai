package mossai;

import java.util.List;
import java.util.Random;

/** Encapsulation of the uncertain state of the opponent's hands. */
public class BeliefState
{
    public static final int STD_DEAL = 16;
    public static final int LEAD_DEAL = 20;
    
    private final int leader;
    
    private final Belief leftHand;
    private final Belief rightHand;
    
    /** Number of cards with unknown value in play. */
    private int pool;
    
    /** Construct a belief state from an initial card dealing. */
    public BeliefState(int agentPos, List<Card> deal)
    {
        leader = agentPos;
        
        leftHand = new Belief(deal, leader == AI.AGENT);
        rightHand = new Belief(deal, leader == AI.AGENT);
        
        if(leader == AI.AGENT) pool = STD_DEAL * 2;
        else pool = LEAD_DEAL + STD_DEAL;
    }
    
    /** Generate sample from the probability distribution of potential states. */
    public boolean[][] sampleHands()
    {
        boolean[][] samples = new boolean[2][Deck.DECK_SIZE];
        Random rand = new Random();
        
        for(Card c : Card.values())
        {
            int i = Deck.cardToInt(c);
            
            int leftChance = leftHand.chance(c, pool);
            int rightChance = rightHand.chance(c, pool);
            int threshold = rand.nextInt(100) + 1;

            if(threshold <= leftChance) samples[AI.LEFT-1][i] = true;
            else if(rightChance != 0) samples[AI.RIGHT-1][i] = true;
        }
        
        return samples;
    }
    
    public void cardPlayed(int player, Card c, Card leadCard)
    {
        Belief active;
        Belief passive;

        if(player == AI.LEFT)
        {
            active = leftHand;
            passive = rightHand;
        }
        else
        {
            active = rightHand;
            passive = leftHand;
        }

        active.remove(c);
        passive.remove(c);

        if(player != leader && c.suit != Suit.SPADES && c.suit != leadCard.suit)
        {
            for(int i : Deck.suitRange(c.suit)) active.remove(Deck.intToCard(i));
        }

        pool--;
    }
}
