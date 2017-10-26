


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
                beliefs[(pos+i)%3] = new BeliefState((pos+i)%3, old.beliefs[(pos+i)%3], cards);
        }
        else
            for(int i = 1; i < 3; i++)
                beliefs[(pos+i)%3] = new BeliefState(old.beliefs[(pos+i)%3]);
        
        playthroughs = 0.0;
        wins = 0.0;
        children = null;
    }
    
    /**
     * Use a fixed, greedy strategy to determine the active player's move from
     * this state.
     */
    public Card greedyEval()
    {
        // Evaluation for the first player.
        if(turn == order[0])
        {
            // As a leader, we want to play the highest card to keep on winin', to keep on leadin'
            for(Suit suit : Suit.values())
                if(!opponentHigherCardSuit(suit, turn, order[1], beliefs[turn]) && !opponentHigherCardSuit(suit, turn, order[2], beliefs[turn]))
                    return beliefs[turn].highest(suit, turn);
            
            // If we think that we can't beat either p2 or p3, play the lowest card we got
            return beliefs[turn].lowestCardInHand(turn);
        }
        else
        {
            Card winning = table[order[0]];
            
            // Evaluation for the second player.
            if(turn == order[1])
            {
                //System.out.println("I'm player 2, table: " + table.length + " handH: " + handH.size() + " handC: " + handC.size() + " handD: " + handD.size() + " handS: " + handS.size());
                
                // If we have to obey the suit
                if(beliefs[turn].here(winning.suit) == true)
                {
                    System.out.println("I'm obeying the suit.");
                    // If we think that p3 has to obey too
                    if(beliefs[turn].maybeHas(winning.suit, order[2]) == true)
                    {
                        System.out.println("I think p3 will also play this suit");
                        // Get p3s highest card
                        Card p3 = getOpponentsHighestCardSuit(winning.suit, order[2]);
                        // Find out which is higher
                        Card highestCardPlayed = table[order[0]].rank > p3.rank ? table[order[0]] : p3;
                        // Play a higher card or the lowest card if can't win
                        return playCardSuit(highestCardPlayed);
                    }
                    // If we think that p3 doesn't have to obey
                    else
                    {
                        // If we think that p3 is going to trump the obeyed suit
                        if(winning.suit != Suit.SPADES && beliefs[turn].maybeHas(Suit.SPADES, order[2]) == true)
                        {
                            System.out.println("I think p3 will play a trump");
                            // We can't win against a spade, play the lowest card
                            return beliefs[turn].lowest(winning.suit, turn);
                        }
                        // If p3 is not going to play a spade
                        else
                        {
                            System.out.println("I think p3 doesn't have this suit and he can't trump");
                            // Play a higher card or the lowest card of the suit
                            return playCardSuit(table[order[0]]);
                        }
                    }
                }
                // We don't have to obey the suit
                else
                {
                    // If spades were played but we don't have any
                    if(winning.suit == Suit.SPADES)
                        return beliefs[turn].lowestCardInHand(turn);
                    // If spades are not the tick suit
                    else
                        // If we think that p3 has to obey
                        if(beliefs[turn].maybeHas(winning.suit, order[2]) == true)
                            // If we can play a spade
                            if(beliefs[turn].maybeHas(Suit.SPADES, turn))
                                // Play the lowest spade to win the tick
                                return beliefs[turn].lowest(Suit.SPADES, turn);
                            // We don't have to obey and we don't have any spades
                            else
                                // We can't win, get rid of the smallest card
                                return beliefs[turn].lowestCardInHand(turn);
                        // If we think p3 doesn't have to obey the suit too
                        else
                            // If we think that p3 has spades
                            if(beliefs[turn].maybeHas(Suit.SPADES, order[2]) == true)
                                // If we can play a spade
                                if(beliefs[turn].maybeHas(Suit.SPADES, turn))
                                {
                                    // get p3s highest trump
                                    Card p3 = getOpponentsHighestCardSuit(Suit.SPADES, order[2]);
                                    
                                    // play a higher spade or the lowest card we have
                                    return playCardTrump(p3);
                                }
                                else
                                    // We can't win, get rid of the smallest card
                                    return beliefs[turn].lowestCardInHand(turn);
                            // If we think that p3 doesn't have a spade
                            else
                                // If we can play a spade
                                if(beliefs[turn].here(Suit.SPADES))
                                    // Play the lowest spade to win the tick
                                    return beliefs[turn].lowest(Suit.SPADES, turn);
                                else
                                    // We can't win, get rid of the smallest card
                                    return beliefs[turn].lowestCardInHand(turn);
                }
            }
            // Evaluation for the third player.
            else
            {
                System.out.println("I'm player 3, table:" + table[order[0]] + ", " + table[order[1]]); 
                //System.out.println("I'm player 3, table: " + table.length + " handH: " + handH.size() + " handC: " + handC.size() + " handD: " + handD.size() + " handS: " + handS.size());
                // If we have to obey the suit
                if(beliefs[turn].here(winning.suit))
                {
					Card highestCardPlayed;
                    // If p2 obeyed the suit, select the one with highest rank
                    if(table[order[1]].suit == winning.suit)
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
                            // scenario: trumps played by p2 didn't have any or p2 doesn't have the tick suit and didn't play any trumps
                            highestCardPlayed = table[order[0]];
                    
                    System.out.println("I'm obeying the suit. Highest Card on the table: " + highestCardPlayed.toString());
                    
                    // If we have to play, but a spade has been played outside of it's suit
                    if(winning.suit != Suit.SPADES && highestCardPlayed.suit == Suit.SPADES)
                    {
                        System.out.println("I can't win playing lowest obey suit");
                        // We can't win, play the lowest card
                        return beliefs[turn].lowest(winning.suit, turn);
                    }
                    else
                    {
                        System.out.println("Play higher or lowest suit card");
                        // Play a higher card of the obeyed suit or lowest
                        return playCardSuit(highestCardPlayed);
                    }
                }
                // We don't have the suit
                else
                    // If spades were played but we don't have any
                    if(winning.suit == Suit.SPADES)
                        // We can't win, get rid of the smallest card
                        return beliefs[turn].lowestCardInHand(turn);
                    // If we don't have to obey and the played suit is not spades
                    else
                        // We don't have any spades either
                        if(!beliefs[turn].here(Suit.SPADES))
                            // We can't win, get rid of the smallest card
                            return beliefs[turn].lowestCardInHand(turn);
                        // If we don't have to obey and we can play a spade
                        else
                            // If player 2 played a trump
                            if(table[order[1]].suit == Suit.SPADES)
                                // Play winning trump or the smallest card we have
                                return playCardTrump(table[order[1]]); // want to avoid handS for findLowestCard?
                            else
                                // We don't have to obey and spades were not played
                                return beliefs[turn].lowest(Suit.SPADES, turn);
            }
        }
    }

    
    public Card playCardSuit(Card c)
    {
        // Go through each one of the cards of that suit that we have, above the highest
        for(int i = Game.cardToInt(c); i <= Game.suitEnds(c.suit); i++)
            // If anything higher than that is found, play that card (doesn't have to be the highest)
            if(beliefs[turn].here(Game.intToCard(i)))
                return Game.intToCard(i);
        
        // If we can't beat what's on the table, play the lowest card
        return beliefs[turn].lowest(c.suit, turn);
    }
    
    private Card playCardTrump(Card highestSpadeCard)
    {
        for(int i = Game.suitBegins(Suit.SPADES)+highestSpadeCard.rank; i <= Game.suitEnds(Suit.SPADES); i++)
            if(beliefs[turn].here(Game.intToCard(i)))
                return Game.intToCard(i);
        
        // We can't beat the spade, play the smallest card
        return beliefs[turn].lowestCardInHand(turn);    
    }
        
    /*
        Heuristic for evaluating what is in the opponents hand. 
        Right now it is always paranoid.
    */
    private Card getOpponentsHighestCardSuit(Suit s, int opponent)
    {
        int i = Game.suitBegins(s);
        Card highest = Game.intToCard(i);
        Card opponentCard;
        
        for(i = Game.suitBegins(s)+1; i <= Game.suitEnds(s); i++)
        {
            opponentCard = Game.intToCard(i);
            
            // If we think that there is a good chance that they have the card
            if(beliefs[0].chance(opponentCard, opponent) > Raptor.POSITIVE)
                // If it is a higher card
                if(opponentCard.rank > highest.rank)
                    highest = opponentCard;
        }
        
        //return myState.highest(suit, opponent);
        return highest;
    }

    private boolean opponentHigherCardSuit(Suit s, int loc, int opponent, BeliefState myState)
    {
        Card myHighestCard = myState.highest(s, loc);
        Card opponentCard;
        
        for(int i = Game.suitBegins(s)+myHighestCard.rank; i <= Game.suitEnds(s); i++)
        {
            opponentCard = Game.intToCard(i);
            
            // If we think that there is a good chance that the opponent has a higher card
            if(myState.chance(opponentCard, opponent) > Raptor.POSITIVE)
                //return opponentCard;
                return true;
        }
        
        return false;
    }

    
    /** Use Monte Carlo tree search to evaluate the best move from this state. */
    public Card monteCarlo()
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
			System.out.println("Expanding node during agent turn: " + (turn == pos));
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
							System.out.println("Cloned state for child, agent move: " + (child.turn == pos));
							child.advance(Game.intToCard(i));
							System.out.println("Agent move made, next turn: " + child.turn + ", is agent: " + (child.turn == pos));

							// Simulate predicted opponent moves.
							while(child.turn != pos)
							{
								System.out.println("Simulating turn: " + child.turn + ", is agent: " + (child.turn == pos));
								child.advance(child.greedyEval());
								System.out.println("Next turn: " + child.turn + ", is agent: " + (child.turn == pos));
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
		return p == order[0] || s == table[order[0]].suit || !beliefs[p].here(table[order[0]].suit);
	}
 }
