package mossai;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** An agent (hopefully) capable of intelligently playing Moss Side Whist. */
public class Raptor implements MSWAgent
{
    /** The agent's place relative to the leader. */
    public final int pos;
    
    /** Names of players, left to right from the agent. */
    private final String[] names;
    
    /** Map of player names to their positions. */
    private final HashMap<String, Integer> players;
    
    private GameState state;
    
    /**
     * The agent's belief about the opponent's cards. Storing between tricks
     * improves performance.
     */
    private BeliefState belief;
    
    /**
     * A non-player specific belief from which the opponent's perspectives can
     * be derived.
     */
    private final BeliefState history;
    
    // Parameters for modification via reinforcement learning.
    /** Bias constant for Monte Carlo play outs. */
    public final int bias;
    
    /** Whether to treat draws as wins in Monte Carlo playouts. */
    public final boolean drawWins;
    
    /** How long to keep running Monte Carlo on samples each turn. */
    public final long searchTime;
    
    /** Number of play outs to sample in each Monte Carlo search. */
    public final int MCsamples;
    
    /** The probability threshold above which to treat as certain that an opponent possesses a card. */
    public final double positive;
    
    /** The probability threshold below which to treat as certain that an opponent does not possess a card. */
    public final double negative;
    
    public Raptor(int b, boolean dw, long t, int mcs, double p, double n)
    {
        pos = -1;
        names = new String[] {"Clever Girl", "", ""};
        players = new HashMap(3);
        state = null;
        belief = null;
        history = new BeliefState();
        
        bias = b;
        drawWins = dw;
        searchTime = t;
        MCsamples = mcs;
        positive = p;
        negative = n;
    }
    
    @Override
    public void setup(String agentLeft, String agentRight)
    {
        names[1] = agentLeft;
        names[2] = agentRight;
    }

    @Override
    public void seeHand(List<Card> deal, int order)
    {
        pos = order;
        state = new GameState(this, order);
        for(int i = 0; i < 3; i++) players.put(names[i], (order+i)%3);
        belief = new BeliefState(order, history, Game.CardstoInts(deal, order));
    }

    @Override
    public Card[] discard()
    {
        // TODO: Discard strategy.
        // todo idea: discard and playout to see if a good discard
        
        int discarded = 0;
        Card card;
        Card[] discardPile = new Card[4];
        List<Card>[] hand =  belief.getHand().clone();
        
        while(discarded < 4)
        {
            List<Card> handH = hand[Game.suitToInt(Suit.HEARTS)];
            List<Card> handC = hand[Game.suitToInt(Suit.CLUBS)];
            List<Card> handD = hand[Game.suitToInt(Suit.DIAMONDS)];
        
            // If any of the suits are below 3 cards, get rid of them
            card = checkDiscardSuit(handH);
            if(card == null)
            {
                card = checkDiscardSuit(handC);
                if(card == null)
                {
                    card = checkDiscardSuit(handD);        
                }
            }
            if(card != null)
            {
                discardPile[discarded] = card;
                hand[Game.suitToInt(card.suit)].remove(card);
                discarded++;
            }
            else
            {
                // Otherwise get rid of the lowest card (but not spades)
                card = GameState.findLowestCard(handH, handC, handD, new ArrayList<Card>());
            }
            
            // add that card to discard pile
            discardPile[discarded] = card;
            
            // remove it from the hand
            hand[Game.suitToInt(card.suit)].remove(card);
            belief.cardPlayed(card, 0, null);
            
            discarded++;
        }
        
        return discardPile;
    }
    private Card checkDiscardSuit(List<Card> handSuit)
    {
        Card card = null;
        
        if(!handSuit.isEmpty() && handSuit.size() < 3)
        {
            if(handSuit.get(0).rank < 12)
            {
                card = handSuit.get(0);
            }
        }
        return card;
    }
    
    @Override
    public Card playCard()
    {
        Card best = null;
        long start = System.nanoTime();
 /*       
        // A record of how many times each card was recommended by a Monte Carlo search.
        HashMap<Card, Integer> results = new HashMap();
        
        while(System.nanoTime() - start < searchTime)
        {
            Card c = GameState.monteCarlo(state, belief, history);
            
            if(results.get(c) == null) results.put(c, 1);
            else results.put(c, results.get(c) + 1);
        }
        
        for(Card c : results.keySet()) if(best == null || results.get(c) > results.get(best)) best = c;
  */      
        // Temp version.
        best = GameState.greedyEval(state, belief);
        
        update(best, pos);
        return best;
    }

    @Override
    public void seeCard(Card card, String agent) { update(card, players.get(agent)); }

    @Override
    public void seeResult(String winner)
    {
        // Verify that state is working properly.
        if(state.getFirst() != players.get(winner)) System.out.println("Trick over but state not set properly.");
    }

    @Override
    public void seeScore(Map<String, Integer> scoreboard)
    {
        // Verify that state is working properly.
        int[] scores = state.getScores();
        
        for(String n : names)
        {
            if(scores[players.get(n)] != scoreboard.get(n))
            {
                System.out.println("Score incorrect for " + n + ".");
            }
        }
    }

    @Override
    public String sayName() { return names[0]; }
    
    private void update(Card played, int player)
    {
        belief.cardPlayed(played, player, state.getLead());
        history.cardPlayed(played, player, state.getLead());
        state.advance(played);
    }
}