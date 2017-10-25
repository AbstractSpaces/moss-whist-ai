package mossai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Encapsulation of the games state at a point in time. */
class GameState
{
    /** The agent's place relative to the leader. */
    private static int pos;
    
    /** The indices of players in their turn order for this trick. */
    private int[] order;
    
    /** The player currently taking their turn, having not yet played their card. */
    private int turn;
    
    /** The cards so far played by each player this trick. */
    private final Card[] table;
    
    /** Running tally of the scores. */
    private final int[] scores;
    
    /** The beliefs held by the agent and the two simulated opponents. */
    private final BeliefState[] beliefs;
    
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
        order = new int[] {leader, (leader+1)%3, (leader+2)%3};
        turn = order[0];
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
            {
                int p = (pos + i) % 3;
                beliefs[i] = new BeliefState(p, old.beliefs[p], cards);
            }
        }
        else
        {
            for(int i = 1; i < 3; i++)
            {
                int p = (pos + i) % 3;
                beliefs[i] = new BeliefState(old.beliefs[p]);
            }
        }
        
        playthroughs = 0.0;
        wins = 0.0;
        children = null;
    }
    
    /**
     * Use a fixed, greedy strategy to determine the active player's move from
     * this state.
     */
    Card greedyEval()
    {   
        // Evaluation for the first player.
        if(turn == order[0])
        {
            
        }
        else
        {
            Card winning = table[order[0]];
            
            // Evaluation for the second player.
            if(turn == order[1])
            {
                
            }
            // Evaluation for the third player.
            else
            {
                
            }
        }
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
    
    int[] getScores() { return Arrays.copyOf(scores, 3); }
    
    /** Move the state forward by one turn. */
    void advance(Card played)
    {
        for(BeliefState b : beliefs)
			b.cardPlayed(played, turn, table[0]);
		
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
            order[0] = best;
            order[1] = (best + 1) % 3;
            order[2]= (best + 2) % 3;
        }
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
						// If the agent has this card.
						if(beliefs[pos].here(Game.intToCard(i)))
						{
							// Simulate a legal move.
							GameState child = new GameState(this, false);
							child.advance(Game.intToCard(i));
							
							// Simulate predicted opponent moves.
							while(child.turn != pos)
								child.advance(child.greedyEval());
							
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
		return p == order[0] || s == table[0].suit || !beliefs[p].here(table[0].suit);
	}
 }
