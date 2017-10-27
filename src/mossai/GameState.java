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
    public final int[] scores;
    
    /** The beliefs held by the agent and the two simulated opponents. */
    public final BeliefState[] beliefs;
    
    /** Number of Monte Carlo simulations that have used this state. */
    private double playthroughs;
    
    /** Number of winning Monte Carlo simulations that have used this state. */
    private double wins;
    
    /** Number of successor states in a Monte Carlo simulation. */
    private ArrayList<GameState> children;
    
    /** Construct a blank state for a new game. */
    GameState(int p, List<Card> deal)
    {
        pos = p;
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
    
    /** Return the agent's order position. */
    int getPos() { return pos; }
	
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

        // The cards played or expected to be played by each player.
        Card c0 = null;
        Card cLeft;
        Card cRight;
		
        // Evaluation for the first player.
        if(turn == order[0])
        {
            // Compare the available cards of each suit, choose the first one
            // with a good chance at winning.
            // Starting the loop with trumps encourages other players to use
            // their trumps early, lessening their chances to trump us later on.
            for(int s = Game.NUM_SUITS-1; s >= 0; s--)
            {
                c0 = active.highest(Game.suitIntToSuit(s));
				
                if(c0 != null && !active.otherHasHigher(c0, left()) && !active.otherHasHigher(c0, right()))
                    break;
            }
        }
        else
        {
            // The card in the running to win.
            Card contested;

            // Evaluation for the second player.
            if(turn == order[1])
            {
                cRight = table[order[0]];
                cLeft = null;

                // If the third player is expected to follow suit.
                if(active.otherHas(cRight.suit, left()))
                    cLeft = active.highestInOther(cRight.suit, left());
                // If the third player is expected to trump.
                else if(cRight.suit != Game.TRUMP && active.otherHas(Game.TRUMP, left()))
                    cLeft = active.highestInOther(Game.TRUMP, left());

                if(cLeft != null && challenge(cRight, cLeft))
                    contested = cLeft;
                else
                    contested = cRight;
            }
            // Evaluation for the third player.
            else
            {
                // See if the second player beat the lead.
                cLeft = table[order[0]];
                cRight = table[order[1]];

                if(challenge(cLeft, cRight))
                    contested = cRight;
                else
                    contested = cLeft;
            }

            // Whether playing second or third, attempt to beat the odds-on favourite.
            // See if the active has to follow suit but can beat the contested.
            if(active.has(table[order[0]].suit) && contested.suit == table[order[0]].suit && active.hasHigher(contested))
                c0 = active.beat(contested);
            // See if winning by trump is possible.
            else if(active.has(Game.TRUMP))
            {
                if(contested.suit == Game.TRUMP)
                    c0 = active.beat(contested);
                else
                    // Any trump will win the trick.
                    c0 = active.lowest(Game.TRUMP);
            }
        }
		
        // Play a (hopefully) winning card.
        if(c0 != null)
            return c0;
        // Throw away a card without having to follow suit.
        else if(table[order[0]] == null || !active.has(table[order[0]].suit))
            return active.lowest(true);
        // Throw away a card while being obliged to follow suit.
        else
            return active.lowest(table[order[0]].suit);
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
		Card d = beliefs[pos].lowest(true);
		beliefs[pos].cardPlayed(d, pos, null);
		return d;
	}
    
    /** Move the state forward by one turn. */
    void advance(Card played)
    {
        for(BeliefState b : beliefs)
            b.cardPlayed(played, turn, table[order[0]]);
		
        table[turn] = played;
        turn = left();
        
        // If trick over, update score, clear table, set order. 
        if(turn == order[0])
        {
            int win = order[0];
            
            for(int i = 1; i < 3; i++)
            {
                int p = order[i];
                if(challenge(table[win], table[p]))
                        win = p;
            }
            
            Arrays.fill(table, null);
            turn = win;
            scores[turn]++;
            order[0] = turn;
            order[1] = left();
            order[2]= right();
        }
    }
	
	/** Return the player to the left of (next in the order) of the active. */
	private int left() {return (turn + 1) % 3;}
	
	/** Return the player to the right of (before in the order) of the active. */
	private int right() {return (turn + 2) % 3;}
	
	/** Returns true if the challenger beat the contested. */
	private boolean challenge(Card contested, Card challenger)
	{
		return (challenger.suit == contested.suit && challenger.rank > contested.rank)
			|| (challenger.suit == Game.TRUMP && contested.suit != Game.TRUMP);
	}

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
            playthroughs++;

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
