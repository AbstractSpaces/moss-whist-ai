package mossai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Encapsulation of the games state at a point in time. */
class GameState
{
    /** The agent's place relative to the leader. */
    public static int pos;
    
    // The indices of players in their turn order for this trick.
    private int[] order;
    
    /** The player currently taking their turn, having not yet played their card. */
    private int turn;
    
    /** The cards so far played by each player this trick. */
    private final Card[] table;
    
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
	
	int currentTurn() { return turn; }
    
    /**
     * Use a fixed, greedy strategy to determine the active player's move from
     * this state.
     */
    Card greedyEval()
    {
        Card play;
        // Evaluation for the first player.
        if(turn == order[0])
        {
            // As a leader, we want to play the highest card to keep on winin', to keep on leadin'
            for(Suit s : Suit.values())
			{
				Card challenge1 = beliefs[turn].highest(s, order[1]);
				Card challenge2 = beliefs[turn].highest(s, order[2]);
				Card contest;
				
				if(challenge1.rank > challenge2.rank)
					contest = beliefs[turn].higherThan(challenge1, turn);
				else
					contest = beliefs[turn].higherThan(challenge2, turn);
				
				if(contest != null)
					return contest;
				else
					return beliefs[turn].lowest(turn);
			}
            
            // If we think that we can't beat either p2 or p3, play the lowest card we got
            return beliefs[turn].lowest(turn);
        }
        else
        {
            Card leading = table[order[0]];
            
            // Evaluation for the second player.
            if(turn == order[1])
            {
                System.out.println("I'm player 2, table:" + table[order[0]]);
                
                // If we have to obey the suit
                if(beliefs[turn].has(leading.suit, turn))
                {
                    System.out.println("I'm obeying the suit.");
                    
                    // If we think that p3 has to obey too
                    if(beliefs[turn].has(leading.suit, order[2]) == true)
                    {
                        System.out.println("I think p3 will also play this suit");
                        // Get p3s highest card
                        Card p3 = beliefs[turn].highest(leading.suit, order[2]);
                        // Find out which is higher
                        Card highestCardPlayed = table[order[0]].rank > p3.rank ? table[order[0]] : p3;
                        // Everyone is playing the same suit
                        // Play a higher card or the lowest card if can't win
                        play = followSuit(highestCardPlayed);
                    }
                    // If we think that p3 doesn't have to obey
                    else
                    {
                        // If we think that p3 is going to trump the obeyed suit
                        if(leading.suit != Suit.SPADES && beliefs[turn].has(Suit.SPADES, order[2]) == true)
                        {
                            System.out.println("I think p3 will play a trump");
                            // We can't win against a spade, but we have to play the suit, play the lowest card
                            play = beliefs[turn].lowest(leading.suit, turn);
                        }
                        // If p3 is not going to play a spade
                        else
                        {
                            System.out.println("I think p3 doesn't have this suit and he can't trump");
                            // Play a higher card or the lowest card of the suit
                            play = followSuit(table[order[0]]);
                        }
                    }
                }
                // We don't have to obey the suit
                else
                {
                    // If spades were played but we don't have any
                    if(leading.suit == Suit.SPADES)
                    {
                        System.out.println("Spades were played but I don't have any");
                        play = beliefs[turn].lowest(turn);
                    }
                    // If spades are not the suit in this trick
                    else
                    {
                        // If we think that p3 has to obey
                        if(beliefs[turn].has(leading.suit, order[2]))
                        {
                            System.out.println("I think p3 will obey this suit");
                            // If we can play a spade
                            if(beliefs[turn].has(Suit.SPADES, turn))
                                // Play the lowest spade to win the tick
                                play = beliefs[turn].lowest(Suit.SPADES, turn);
                            // We don't have to obey and we don't have any spades
                            else
                                // We can't win, get rid of the smallest card
                                play = beliefs[turn].lowest(turn);
                        }
                        // If we think p3 doesn't have to obey the suit too
                        else
                        {
                            // If we think that p3 has spades
                            if(beliefs[turn].has(Suit.SPADES, order[2]) == true)
                                // If we can play a spade
                                if(beliefs[turn].has(Suit.SPADES, turn))
                                {
                                    // Get p3s highest trump
                                    Card challenge = beliefs[turn].highest(Game.TRUMP, order[2]);
                                    // Play a higher spade or the lowest card we have
                                    Card contest = beliefs[turn].higherThan(challenge, turn);
									
									if(contest != null)
										return contest;
									else
										return beliefs[turn].lowest(turn);
                                }
                                else
                                    // We can't win, get rid of the smallest card
                                    play = beliefs[turn].lowest(turn);
                            // If spades were not played && we think that p3 doesn't have a spade
                            else
                                // If we can play a spade
                                if(beliefs[turn].has(Suit.SPADES, turn))
                                    // Play the lowest spade to win the tick
                                    play = beliefs[turn].lowest(Suit.SPADES, turn);
                                else
                                    // We can't win, get rid of the smallest card
                                    play = beliefs[turn].lowest(turn);
                        }
                    }
                }
            }
            // Evaluation for the third player.
            else
            {
                System.out.println("I'm player 3, table:" + table[order[0]] + ", " + table[order[1]]); 
                // If we have to obey the suit
                if(beliefs[turn].has(leading.suit, turn))
                {
                    // Find the highest card played between p1 and p2
                    Card highestCardPlayed = leading;
                    // If p2 obeyed the suit, select the one with highest rank
                    if(table[order[1]].suit == leading.suit)
                        if(table[order[0]].rank > table[order[1]].rank)
                            highestCardPlayed = table[order[0]];
                        else
                            highestCardPlayed = table[order[1]];
                    // If p2 didn't obey the suit
                    else
                        // If player 2 played a trump
                        if(table[order[1]].suit == Suit.SPADES)
                            // p2 is trumping p1s card
                            highestCardPlayed = table[order[1]];
                        // Otherwise it played a card outside of the suit that's not a trump
                        else
                            // p2 can't win, highest card is by p1
                            highestCardPlayed = table[order[0]];
                    
                    System.out.println("I'm obeying the suit. Highest Card on the table: " + highestCardPlayed.toString());
                    
                    // If we have to obey the suit, but a spade has been played outside of it's suit
                    if(leading.suit != Suit.SPADES && highestCardPlayed.suit == Suit.SPADES)
                    {
                        System.out.println("I can't win playing lowest out of the leading suit");
                        // We can't win, play the lowest card
                        play = beliefs[turn].lowest(leading.suit, turn);
                    }
                    else
                    {
                        System.out.println("Play higher or lowest suit card");
                        // Play a higher card of the obeyed suit or lowest
                        play = followSuit(highestCardPlayed);
                    }
                }
                // We don't have the suit
                else
                {
                    System.out.println("We don't have to obey the leading suit.");
                    // If spades were played but we don't have any
                    if(leading.suit == Suit.SPADES)
                        // We can't win, get rid of the smallest card
                        play = beliefs[turn].lowest(turn);
                    // If we don't have to obey and the played suit is not spades
                    else
                        // We don't have any spades either
                        if(!beliefs[turn].has(Suit.SPADES, turn))
                            // We can't win, get rid of the smallest card
                            play = beliefs[turn].lowest(turn);
                        // If we don't have to obey and we can play a spade
                        else
                            // If player 2 played a trump
                            if(table[order[1]].suit == Suit.SPADES)
                                // Play winning trump or the smallest card we have
                                play = challenge(table[order[1]]); // want to avoid handS for findLowestCard?
                            else
                                // We don't have to obey and spades were not played
                                play = beliefs[turn].lowest(Suit.SPADES, turn);
                }
            }
        }
        if(play != null)
            System.out.println(play);
        else
            System.out.println("null!");
        return play;
    }
	
	private Card player2Eval(Card lead)
	{
		// If p2 has to obey the suit.
		if(beliefs[turn].has(lead.suit, turn))
		{
			// Fetch the best card available to p3.
			Card challenge = beliefs[turn].highest(lead.suit, order[2]);
			
			// If p3 must follow.
			if(challenge != null)
			{
				// If p3 can beat the lead.
				if(challenge.rank > lead.rank)
				{
					// Attempt to beat p3's best.
					Card contest = beliefs[turn].higherThan(challenge, turn);

					if(contest != null)
						return contest;
				}
			}
			// If p2 thinks that p3 can't trump the lead.
			else if(!beliefs[turn].has(Game.TRUMP, order[2]))
			// The lead card is still the best contender, so p2 attempts to beat
			// it.
				return beliefs[turn].higherThan(lead, turn);
		}
		// p2 doesn't have to follow the suit.
		else
		{
			// If the lead isn't a trump.
			if(lead.suit != Game.TRUMP)
			{
				// If we think that p3 has to follow.
				if(beliefs[turn].has(lead.suit, order[2]))
				{
					// If we can play a trump.
					if(beliefs[turn].has(Suit.SPADES, turn))
						// Play the lowest spade to win the trick
						return beliefs[turn].lowest(Suit.SPADES, turn);
				}
				// If we think p3 doesn't have to follow either.
				else
				{
					// If we think that p3 has a trump.
					if(beliefs[turn].has(Game.TRUMP, order[2]))
					{
						// If we can contest their trump.
						if(beliefs[turn].has(Suit.SPADES, turn))
						{
							Card theirs = beliefs[order[2]].highest(Game.TRUMP, order[2]);
							Card mine = beliefs[turn].higherThan(theirs, turn);
							
							// If our trump beats theirs.
							if(mine != null)
								return mine;
						}
						// If spades were not played && we think that p3 doesn't have a spade
						else
							return beliefs[turn].lowest(Suit.SPADES, turn);
					}
				}
			}
		}
		
		// p2 can't win, throw away a card.
		return beliefs[turn].lowest(turn);
    }

	/**
	 * Given the best card on the table, attempt to play a better card from the
	 * same suit.
	 */
    private Card followSuit(Card c)
    {
        // Go through each one of the cards of that suit that we have, above the highest
        for(int i = Game.cardToInt(c); i <= Game.suitEnds(c.suit); i++)
            // If anything higher than that is found, play that card (doesn't have to be the highest)
            if(beliefs[turn].has(Game.intToCard(i), turn))
                return Game.intToCard(i);

        // If we can't beat what's on the table, play the lowest card
        return beliefs[turn].lowest(c.suit, turn);
    }
    
    private Card challenge(Card c)
    {
        Card contest = beliefs[turn].higherThan(c, turn);
		
		if(contest != null)
			return contest;
		else
			return beliefs[turn].lowest(turn);
    }
	
    /** Use Monte Carlo tree search to evaluate the best move from this state. */
    public Card monteCarlo()
    {
		System.out.println("Monte Carlo called for player: " + turn);
        GameState root = new GameState(this, true);
		System.out.println(" Sample root clone is same as true state: " + (turn == root.turn));
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
			System.out.println("Tree node is for player: " + turn);
            children = new ArrayList();

            for(Suit s : Suit.values())
            {
                // If the agent is allowed to play from this suit.
                if(legal(pos, s))
                {
                    for(int i = Game.suitBegins(s); i <= Game.suitEnds(s); i++)
                    {
                        // If the agent has this card.
                        if(beliefs[pos].has(Game.intToCard(i), pos))
                        {
							// Simulate a legal move.
							GameState child = new GameState(this, false);
							System.out.println("Cloned child is for player: " + child.turn);
							child.advance(Game.intToCard(i));
							System.out.println("Child turn made.");

							// Simulate predicted opponent moves.
							while(child.turn != pos)
							{
								System.out.println("Simulating turn: " + child.turn);
								Card toPlay = child.greedyEval();
								System.out.println("Card chosen: " + toPlay);
								child.advance(toPlay);
								System.out.println("Next turn: " + child.turn); 
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
			System.out.println("Evaluating UTC during agent turn:" + (turn == pos));
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
		return p == order[0] || s == table[order[0]].suit || !beliefs[p].has(table[order[0]].suit, pos);
	}
 }
