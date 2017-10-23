package mossai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Encapsulation of the uncertain game state.
 * @author Dylan Johnson
 */
public class BeliefState
{
    /** Bit masks for each of the above locations. */
    private static final byte[] MASKS = new byte[] {1, 2, 4, 8};
    
    /** The fifth bit is set 1 until a card's location is confirmed. */
    private static final byte TBC = 16;
    
    /** Macro for setting a card's location completely unknown. */
    private static final byte ANY = 31;
    
    /**
     * The player to which this belief belongs.
     * Set -1 if the viewer is external to all players.
     */
    public final int viewer;
    
    /** The cards held by the viewer, collected and pre sorted for convenience. */
    private ArrayList<Card>[] hand;
    
    /**
     * Represents the valid potential locations each card might occupy.
     * The bit corresponding to that location is set 1 if the card might be there.
     */
    private final byte[] valid;
    
    /** Number of uncertain cards in each location. */
    private final int[] unknowns;
    
    /** Create a blank belief for a new game. */
    public BeliefState()
    {
        viewer = -1;
        hand = null;
        valid = new byte[Game.DECK_SIZE];
        Arrays.fill(valid, ANY);
        unknowns = new int[] {Game.DEAL, Game.DEAL, Game.DEAL, Game.DISCARDS};
    }
    
    /**
     * Incorporate a player's knowledge into an objective history to form a
     * belief for them.
     */
    public BeliefState(int v, BeliefState history, int[] state)
    {
        viewer = v;
        hand = new ArrayList[4];
        for(int i = 0; i < 4; i++) hand[i] = new ArrayList();
        valid = Arrays.copyOf(history.valid, Game.DECK_SIZE);
        unknowns = Arrays.copyOf(history.unknowns, 4);
        unknowns[viewer] = 0;
        if(viewer == Game.LEADER) unknowns[Game.OUT] = 0;
        
        // With new knowledge we can perform some invalidations and fill in the hand.
        for(int i = 0; i < Game.DECK_SIZE; i++)
        {
            // If the card is in the hand.
            if(state[i] == viewer)
            {
                valid[i] = MASKS[viewer];
                hand[i / Game.SUIT_SIZE].add(Game.intToCard(i));
            }
            // If the hand was a valid location but now isn't.
            else if(maybe(i, viewer)) valid[i] -= MASKS[i];
            
            // The leader has no uncertainty over discarded cards.
            if(viewer == Game.LEADER)
            {
                if(state[i] == Game.OUT) valid[i] = MASKS[Game.OUT];
                // If the card was thought discarded but wasn't.
                else if(maybe(i, Game.OUT)) valid[i] -= MASKS[Game.OUT];
            }
            
            // See if card location newly confirmed.
            confirm(i);
        }
    }
    
    /** Create a clone of an existing BeliefState. */
    private BeliefState(BeliefState old)
    {
        viewer = old.viewer;
        valid = Arrays.copyOf(old.valid, Game.DECK_SIZE);
        unknowns = Arrays.copyOf(old.unknowns, 4);
    }
    
    /** Update the belief when a card is played. */
    public void cardPlayed(Card c, int player, Card lead)
    {   
        int i = Game.cardToInt(c);
        
        if(player == viewer)
        {
            valid[i] = MASKS[Game.OUT];
            hand[Game.suitToInt(c.suit)].remove(c);
        }
        // If the viewer has received new knowledge.
        else
        {
            // If the card wasn't known to be in that player's hand, there is
            // now one less unknown card there.
            if(valid[i] != MASKS[player]) unknowns[player]--;
            
            // The card is now known to be out of play.
            valid[i] = MASKS[Game.OUT];

            // If the player didn't follow the lead suit, infer that they have
            // none of that suit.
            if(c.suit != Game.TRUMP && lead != null && c.suit != lead.suit)
            {
                // Iterate through cards in the lead suit.
                for(int j : Game.suitRange(lead.suit))
                {
                    // If the card hasn't already been invalidated.
                    if(maybe(j, player))
                    {
                        // Invalidate the card from this player's hand.
                        valid[j] -= MASKS[player];
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
            double upperBound = 0.0;
            double lowerBound = 0.0;
            
            // Consider the probability of the card being in each location.
            for(int loc = 0; loc < 4; loc++)
            {
                // We want to avoid imprecise comparisons of doubles when we
                // know the location for certain.
                if(hasCard(loc, i))
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
    
    public ArrayList<Card>[] getHand() { return hand; }
    
    /** Return true if the given card is in the given location. */
    public boolean hasCard(int loc, int card) { return valid[card] == MASKS[loc]; }
    
    /** Create a copy of a BeliefState for assigning to a tree Node. */
    @Override
    public BeliefState clone() { return new BeliefState(this); }
    
    /** Return the probability of a card being in a location. */
    private double chance(Card c, int loc)
    {
        int i = Game.cardToInt(c);
        double numerator = 0.0;
        double denominator = 0.0;
        
        // Scan through the locations.
        for(int j = 0; j < 4; j++)
        {
            if(j == loc) numerator = (double)unknowns[j];
            // If j is a valid location for the card.
            // We only want to consider the unknown cards in a location
            // if the card in question might be located there.
            if(maybe(Game.cardToInt(c), j)) denominator += (double)unknowns[j];
        }

        return numerator / denominator;
    }
    
    /** Returns true if the card might be in the given location. */
    private boolean maybe(int card, int loc) { return (valid[card] & MASKS[loc]) == MASKS[loc]; }
    
    /** See if new knowledge allows confirming a card's location. */
    private void confirm(int card)
    {
        // If the card was already confirmed, skip the checks.
        if((valid[card] & TBC) == TBC)
        {
            if(valid[card] == MASKS[Game.LEADER] + TBC)
            {
                valid[card] = MASKS[Game.LEADER];
                unknowns[Game.LEADER]--;
            }
            else if(valid[card] == MASKS[Game.LEFT] + TBC)
            {
                valid[card] = MASKS[Game.LEFT];
                unknowns[Game.LEFT]--;
            }
            else if(valid[card] == MASKS[Game.RIGHT] + TBC)
            {
                valid[card] = MASKS[Game.RIGHT];
                unknowns[Game.RIGHT]--;
            }
            else if(valid[card] == MASKS[Game.OUT] + TBC)
            {
                valid[card] = MASKS[Game.OUT];
                unknowns[Game.OUT]--;
            }
        }
    }
}
