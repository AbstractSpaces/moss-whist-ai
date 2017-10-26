package mossai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Encapsulation of the games state at a point in time. */
class GameState
{
    /** The agent's place relative to the leader. */
    public static int pos;
    
    /** The indices of players in their turn order for this trick. */
    public int[] order;
    
    /** The player currently taking their turn, having not yet played their card. */
    private int turn;
    
    /** The cards so far played by each player this trick. */
    public final Card[] table;
    
    /** Running tally of the scores. */
    private final int[] scores;
    
    /** The beliefs held by the agent and the two simulated opponents. */
    public final BeliefState[] beliefs;
    
    /** Number of Monte Carlo simulations that have used this state. */
    private double playthroughs;
    
    /** Number of winning Monte Carlo simulations that have used this state. */
    private double wins;
    
    /** Number of successor states in a Monte Carlo simulation. */
    private ArrayList<GameState> children;
    
    /** Construct a blank state for a new game. */
    GameState(int leader, List<Card> deal)
    {
        pos = leader;
        order = new int[] {0, 1, 2};
        turn = 0;
        table = new Card[3];
        scores = new int[] {-8, -4, -4};
        
        // Convert the hand into an integer array.
        int[] hand = new int[Game.DECK_SIZE];
        Arrays.fill(hand, -1);
        
        for(Card c : deal)
            hand[Game.cardToInt(c)] = pos;
        
        beliefs = new BeliefState[3];
        beliefs[pos] = new BeliefState(pos, new BeliefState(), hand);
        beliefs[(pos+1)%3] = new BeliefState();
        beliefs[(pos+2)%3] = new BeliefState();
        
        playthroughs = 0.0;
        wins = 0.0;
        children = null;
    }
    
    /** Copy a state, filling out the opponents beliefs with sample data if asked to. */
    private GameState(GameState old, boolean sample)
    {
        order = Arrays.copyOf(old.order, 3);
        turn = old.turn;
        table = Arrays.copyOf(old.table, 3);
        scores = Arrays.copyOf(old.scores, 3);
        
        beliefs = new BeliefState[3];
        beliefs[pos] = new BeliefState(old.beliefs[pos]);
        
        if(sample)
        {
            int[] cards = beliefs[pos].sampleState();
            
            for(int i = 1; i < 3; i++)
                beliefs[(pos+i)%3] = new BeliefState((pos+i)%3, old.beliefs[(pos+i)%3], cards);
        }
        else
            for(int i = 1; i < 3; i++)
                beliefs[(pos+i)%3] = new BeliefState(old.beliefs[(pos+i)%3]);
        
        playthroughs = 0.0;
        wins = 0.0;
        children = null;
    }
	
	/** Return the player whose turn this state represents. */
	int active() { return turn; }
	
	/** Return the running score tally. */
	int[] getScores() { return Arrays.copyOf(scores, 3); }
	
    /**
     * Use a fixed, greedy strategy to determine the active player's move from
     * this state.
     */
    Card greedyEval()
    {
		// This is just to aid readability.
		BeliefState active = beliefs[turn];
		
		// The card currently in the running to win.
		Card contested;
		
		// The card attempting to beat the contested.
		Card challenger;
		
        // Evaluation for the first player.
        if(turn == order[0])
        {
			// Compare the available cards of each suit, choose the first one
			// with a good chance at winning.
            for(Suit s : Suit.values())
			{
				contested = active.highest(s);
				
                if(!active.otherHasHigher(contested, left()) && !active.otherHasHigher(contested, right()))
                    return contested;
			}
        }
        else
        {
            contested = table[order[0]];
            
            // Evaluation for the second player.
            if(turn == order[1])
            {
				// If the third player is expected to follow suit.
				if(active.otherHas(contested.suit, left()))
				{
					challenger = active.highestInOther(contested.suit, left());

					if(challenger.rank > contested.rank)
						contested = challenger;
				}
				// If the third player is expected to trump.
				else if(contested.suit != Game.TRUMP && active.otherHas(Game.TRUMP, left()))
					contested = active.highestInOther(Game.TRUMP, left());
            }
            // Evaluation for the third player.
            else
            {
				
                // See if the second player beat the lead.
				challenger = table[right()];
				
				if((challenger.suit == Game.TRUMP && contested.suit != Game.TRUMP) || challenger.rank > contested.rank)
					contested = challenger;
            }
			
			// Whether playing second or third, attempt to beat the odds-on favourite.
			if(active.hasHigher(contested))
			{
				challenger = active.highest(contested.suit);

				if(challenger != null)
					return challenger;
			}
        }
		
        // If winning is unattainable, throw away a low value card.
		return active.lowest();
    }
    
    /** Use Monte Carlo tree search to evaluate the best move from this state. */
    Card monteCarlo()
    {
        GameState root = new GameState(this, true);
        root.expand();
        
        // Run the search.
        for(int i = 0; i < Raptor.MC_SAMPLES; i++)
            root.playOut();
        
        // Identify the best card.
        GameState best = root.children.get(0);
        
        for(GameState child : root.children)
            if(child.wins / child.playthroughs > best.wins / best.playthroughs)
                best = child;
        
        return best.table[pos];
    }
	
	/** Pick the lowest ranked card for discarding. */
	Card discardLow()
	{
		Card d = beliefs[pos].lowest();
		beliefs[pos].cardPlayed(d, pos, null);
		return d;
	}
    
    /** Move the state forward by one turn. */
    void advance(Card played)
    {
        for(BeliefState b : beliefs)
			b.cardPlayed(played, turn, table[order[0]]);
		
        table[turn] = played;
        turn = (turn + 1) % 3;
        
        // If trick over, update score, clear table, set order. 
        if(turn == order[0])
        {
            int best = order[0];
            
            for(int i = 1; i < 3; i++)
            {
                int p = order[i];
                boolean beat = false;
                
                if(table[best].suit == Game.TRUMP)
                    if(table[p].suit == Game.TRUMP && table[p].rank > table[best].rank)
                        beat = true;
                else if(table[p].suit == table[best].suit && table[p].rank > table[best].rank)
                    beat = true;
                
                if(beat)
                    best = p;
            }
            
            Arrays.fill(table, null);
            scores[best]++;
            turn = best;
            order[0] = turn;
            order[1] = left();
            order[2]= right();
        }
    }
	
	/** Return the player to the left of (next in the order) of the active. */
	private int left() {return (turn + 1) % 3;}
	
	/** Return the player to the right of (before in the order) of the active. */
	private int right() {return (turn + 2) % 3;}

    /** Generate the children of a node on the tree. */
    private void expand()
    {
        // Skip the whole process if the node is already expanded.
        if(children == null)
        {
            children = new ArrayList();

            for(Suit s : Suit.values())
            {
                // If the agent is allowed to play from this suit.
                if(legal(pos, s))
                {
                    for(int i = Game.suitBegins(s); i <= Game.suitEnds(s); i++)
                    {
                        // If the agent otherHas this card.
                        if(beliefs[pos].otherHas(Game.intToCard(i), pos))
                        {
                            // Simulate a legal move.
                            GameState child = new GameState(this, false);
                            child.advance(Game.intToCard(i));

                            // Simulate predicted opponent moves.
                            while(child.turn != pos)
                            {
                                Card toPlay = child.greedyEval();
                                child.advance(toPlay);
                            }

                            children.add(child);
                        }
                    }
                }
            }
        }
    }

    /** Choose a child node as the next in a play out. */
    private boolean playOut()
    {   
        // Game over, roll back up the tree.
        if(children.isEmpty())
        {
            // See if the agent won the game.
            if(Raptor.DRAW_WINS) return scores[pos] >= scores[(pos+1)%3] && scores[pos] >= scores[(pos+2)%3];
            else return scores[pos] > scores[(pos+1)%3] && scores[pos] > scores[(pos+2)%3];
        }
        // Continue the play out.
        else
        {
            int best = 0;
            double max = 0;

            for(int i = 0; i < children.size(); i++)
            {
                double u = children.get(i).UTC(playthroughs);

                if(u > max)
                {
                    max = u;
                    best = i;
                }
            }

            children.get(best).expand();

            if(children.get(best).playOut())
            {
                wins++;
                return true;
            }
            else return false;
        }
    }
	
	/** Function for balancing exploitation and exploration when searching the tree. */
    private double UTC(double parentPT)
    {
        return wins / playthroughs + Raptor.BIAS * Math.sqrt(Math.log(parentPT) / playthroughs);
    }
	
	/**
	 * Returns true if a given player knows they are allowed to play a card
	 * from a certain suit.
	 * NOTE: Do not try using this to inform one player about another player's
	 * hand. Will not work.
	 */
	private boolean legal(int p, Suit s)
	{
		return p == order[0] || s == table[order[0]].suit || !beliefs[p].otherHas(table[order[0]].suit, pos);
	}
 }
