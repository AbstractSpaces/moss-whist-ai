package mossai;

import java.util.Arrays;
import java.util.Random;

/**
 * Encapsulation of the uncertain game state.
 * @author Dylan Johnson
 */
public final class BeliefState
{
    /** Macro for setting a card's location completely unknown. */
    private static final int ALL_LOCS = (1 << 6) - 1;
    
    /** Macro for checking if a card's location is confirmed. */
    private static final int TBC = 1 << 5;
    
    /** Macro for setting all cards of a suit potentially occupying a location. */
    private static final int ALL_RANKS = (1 << 13) - 1;
    
    /** Macro for checking number of unknown cards. */
    private static final int UNKNOWN = 4;
    
    /**
     * The player to which this belief belongs.
     * Set -1 if the viewer is external to all players.
     */
    private final int viewer;
    
    // By using both of the below representations, we can perform most
    // operations in constant time.
    
    /**
     * Represents the potential locations each card might occupy.
     * The fifth bit is set 1 until the location is certain.
     */
    private final int[] locs;
    
    /**
     * Represents the potential cards that might be in each location.
     * Each cards[i] relates to location i.
     * For j = 0 to 3, cards[i][j] represents the potential cards of suit j.
     * For j = 4, cards[i][j] stored the number of unknown cards in location i.
     */
    private final int[][] cards;
    
    /** Create a blank belief for a new game. */
    public BeliefState()
    {
        viewer = -1;
        
        locs = new int[Game.DECK_SIZE];
        Arrays.fill(locs, ALL_LOCS);
        
        cards = new int[4][4];
        for(int i = 0; i < 4; i++)
        {
            Arrays.fill(cards[i], ALL_RANKS);
            cards[i][UNKNOWN] = Game.DEAL;
        }
        cards[Game.OUT][UNKNOWN] = Game.DISCARDS;
    }
    
    /**
     * Incorporate a player's knowledge into an objective history to form a
     * belief for them.
     */
    public BeliefState(int v, BeliefState history, int[] state)
    {
        viewer = v;
        locs = Arrays.copyOf(history.locs, Game.DECK_SIZE);
        
        cards = new int[4][4];
        for(int i = 0; i < 4; i++) cards[i] = Arrays.copyOf(history.cards[i], 5);
        cards[viewer][UNKNOWN] = 0;
        if(viewer == Game.LEADER) cards[Game.OUT][UNKNOWN] = 0;
        
        // With new knowledge we can perform some invalidations and fill in the hand.
        for(int i = 0; i < Game.DECK_SIZE; i++)
        {
            // If the card is in the hand.
            if(state[i] == viewer) locs[i] = 1 << viewer;
            // If the hand was a potential location but now isn't.
            else if(maybe(i, viewer))
            {
                locs[i] -= 1 << viewer;
                cards[viewer][Game.intToSuit(i)] -= 1 << Game.intToRank(i);
            }
            
            // The leader has no uncertainty over discarded cards.
            if(viewer == Game.LEADER)
            {
                if(state[i] == Game.OUT) locs[i] = 1 << Game.OUT;
                // If the card was thought discarded but wasn't.
                else if(maybe(i, Game.OUT))
                {
                    locs[i] -= 1 << Game.OUT;
                    cards[Game.OUT][Game.intToSuit(i)] -= 1 << Game.intToRank(i);
                }
            }
            
            // See if card location newly confirmed.
            confirm(i);
        }
    }
    
    /** Create a clone of an existing BeliefState. */
    public BeliefState(BeliefState old)
    {
        viewer = old.viewer;
        locs = Arrays.copyOf(old.locs, Game.DECK_SIZE);
        
        cards = new int[4][4];
        for(int i = 0; i < 4; i++) cards[i] = Arrays.copyOf(old.cards[i], 5);
    }
    
    /** Update the belief when a card is played. */
    public void cardPlayed(Card move, int player, Card lead)
    {
        int i = Game.cardToInt(move);
        int s = Game.intToSuit(i);
        int r = move.rank - 2;
        
        // The card is now known to be out of that hand.
        cards[player][s] -= 1 << r;
        
        if(player == viewer) locs[i] = 1 << Game.OUT;
        // If the viewer has received new knowledge.
        else
        {
            // If the card wasn't known to be in that player's hand, there is
            // now one less unknown card there.
            if(maybe(i, TBC))
            {
                cards[player][UNKNOWN]--;
                locs[i] = 1 << Game.OUT;
            }

            // If the player didn't follow the lead suit, infer that they have
            // none of that suit.
            if(move.suit != Game.TRUMP && lead != null && move.suit != lead.suit)
            {
                cards[player][Game.intToSuit(i)] = 0;
                
                // Iterate through cards in the lead suit.
                int ls = Game.intToSuit(Game.cardToInt(lead));
                
                for(int j = ls*Game.SUIT_SIZE; j < (ls+1)*Game.SUIT_SIZE; j++)
                {
                    // If the card hasn't already been invalidated.
                    if(maybe(j, player))
                    {
                        // Invalidate the card from this player's hand.
                        locs[j] -= 1 << player;
                        confirm(j);
                    }
                }
            }
        }
    }
    
    /** Derive a sample game state from the belief. */
    public int[] sampleState()
    {
        Random gen = new Random();
        int[] state = new int[Game.DECK_SIZE];
        
        // Assign a location to each card in the deck.
        for(Card c : Card.values())
        {
            int i = Game.cardToInt(c);
            double p = gen.nextDouble();
            double upperBound;
            double lowerBound = 0.0;
            
            // Consider the probability of the card being in each location.
            for(int loc = 0; loc < 4; loc++)
            {
                // We want to avoid imprecise comparisons of doubles when we
                // know the location for certain.
                if(certainly(loc, i))
                {
                    state[i] = loc;
                    break;
                }
                // We can skip ahead if this is an invalid location.
                else if(maybe(i, loc))
                {
                    upperBound = lowerBound + chance(c, loc);

                    // We have already ruled out p < lowerBound, so if
                    // p < upperBound it falls between the two bounds.
                    if(p < upperBound)
                    {
                        state[i] = loc;
                        break;
                    }
                    else lowerBound = upperBound;
                }
            }
        }
        
        return state;
    }
    
    /** Returns true if the card might be in the given location. */
    public boolean maybe(int card, int loc) { return (locs[card] & (1 << loc)) != 0; }
    
    /** Return true if the given card is in the given location. */
    public boolean certainly(int loc, int card) { return locs[card] == (1 << loc); }
    
    /**
     * Return true if the specified player might have a higher ranked card in
     * the same suit of the given card.
     */
    public boolean higher(Card c, int player) { return cards[player][Game.suitToInt(c.suit)] > (1 << (c.rank-2)); }
    
    /** Return the probability of a card being in a location. */
    private double chance(Card c, int loc)
    {
        double numerator = 0.0;
        double denominator = 0.0;
        
        // Scan through the locations.
        for(int j = 0; j < 4; j++)
        {
            if(j == loc) numerator = (double)cards[j][UNKNOWN];
            // If j is a locs location for the card.
            // We only want to consider the unknown cards in a location
            // if the card in question might be located there.
            if(maybe(Game.cardToInt(c), j)) denominator += (double)cards[j][UNKNOWN];
        }

        return numerator / denominator;
    }
    
    /** See if new knowledge allows confirming a card's location. */
    private void confirm(int card)
    {
        // If the card was already confirmed, skip the checks.
        if((locs[card] & TBC) != 0)
        {
            switch(locs[card] - TBC)
            {
                case Game.LEADER:
                    locs[card] = Game.LEADER;
                    cards[Game.LEADER][UNKNOWN]--;
                    break;
                case 1 << Game.LEFT:
                    locs[card] = 1 << Game.LEFT;
                    cards[Game.LEFT][UNKNOWN]--;
                    break;
                case 1 << Game.RIGHT:
                    locs[card] = 1 << Game.RIGHT;
                    cards[Game.RIGHT][UNKNOWN]--;
                    break;
                case 1 << Game.OUT:
                    locs[card] = 1 << Game.OUT;
                    cards[Game.OUT][UNKNOWN]--;
                    break;
            }
        }
    }
}
