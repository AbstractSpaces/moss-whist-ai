package mossai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Encapsulation of the games state at a point in time. */
public class GameState
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
    public GameState(int leader, List<Card> deal)
    {
        pos = leader;
        order = new int[] {leader, (leader+1)%3, (leader+2)%3};
        turn = order[0];
        table = new Card[3];
        scores = new int[] {-8, -4, -4};
        
        // Convert the hand into an integer array.
        int[] hand = new int[Game.DECK_SIZE];
        Arrays.fill(hand, -1);
        for(Card c : deal) hand[Game.cardToInt(c)] = pos;
        
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
    public Card greedyEval()
    {
        ArrayList<Card>[] hand = beliefs[turn].getHand();
        
        // Evaluation for the first player.
        if(turn == order[0])
        {
            // As a leader, we want to play the highest card to keep on wining
            
            // if we think we can beat p2 && p3
            // else
                // if risk worth taking 
            
            // If we think that p2 or p3 have a higher card in that same suit
            //return findLowestCard(handH, handC, handD, handS);
            // else we can play
            return findHighestCard(hand);
        }
        else
        {
            Card winning = table[order[0]];
            Suit leadSuit = winning.suit;
            
            List<Card> selectedSuit = hand[Game.suitToInt(leadSuit)];
            
            // Evaluation for the second player.
            if(turn == order[1])
            {
                //hasCard = 100% certain
                
                // trashold ? chance, positive negative
                
                // checking for higher card =
                    // loop from my high card to highest card 
                        // check with chance(card, player)
                
                // check for suit
                
                System.out.println("I'm player 2, table: " + table.length + " handH: " + hand[0].size() + " handC: " + hand[1].size() + " handD: " + hand[2].size() + " handS: " + hand[3].size());
                
                // If we have to obey the suit
                if(!selectedSuit.isEmpty())
                {
                    System.out.println("I'm obeying the suit.");
                    // If we think that p3 has to obey too
                    if(opponentHasSuit(leadSuit) == true)
                    {
                        System.out.println("I think p3 will also play this suit");
                        // Get p3s highest card
                        Card p3 = getOpponentsHighestCardSuit(leadSuit);
                        // Find out which is higher
                        Card highestCardPlayed = table[0].rank > p3.rank ? table[0] : p3;
                        // Play a higher card or the lowest card if can't win
                        return playCardSuit(selectedSuit, highestCardPlayed);
                    }
                    // If we think that p3 doesn't have to obey
                    else
                    {
                        // If we think that p3 is going to trump the obeyed suit
                        if(leadSuit != Suit.SPADES && opponentHasSuit(Suit.SPADES) == true)
                        {
                            System.out.println("I think p3 will play a trump");
                            // We can't win against a spade, play the lowest card
                            return selectedSuit.get(0);
                        }
                        // If p3 is not going to play a spade
                        else
                        {
                            System.out.println("I think p3 doesn't have this suit and he can't trump");
                            // Play a higher card or the lowest card of the suit
                            return playCardSuit(selectedSuit, table[0]);
                        }
                    }
                }
                // We don't have to obey the suit
                else
                {
                    // If spades were played but we don't have any
                    if(leadSuit == Suit.SPADES)
                    {
                        // We can't win, get rid of the smallest card
                        return findLowestCard(hand);
                    }
                    // If spades are not the tick suit
                    else
                    {
                        // If we think that p3 has to obey
                        if(opponentHasSuit(leadSuit) == true)
                        {
                            // If we can play a spade
                            if(hand[Game.suitToInt(Game.TRUMP)].size() > 0)
                            {
                                // Play the lowest spade to win the tick
                                return hand[Game.suitToInt(Game.TRUMP)].get(0);
                            }
                            // We don't have to obey and we don't have any spades
                            else
                            {
                                // We can't win, get rid of the smallest card
                                return findLowestCard(hand);
                            }
                        }
                        // If we think p3 doesn't have to obey the suit too
                        else
                        {
                            // If we think that p3 has spades
                            if(opponentHasSuit(Game.TRUMP) == true)
                            {
                                // If we can play a spade
                                if(hand[Game.suitToInt(Game.TRUMP)].size() > 0)
                                {
                                    // get p3s highest trump
                                    Card p3 = getOpponentsHighestCardSuit(Game.TRUMP);
                                    // play a higher spade or the lowest card we have
                                    return playCardTrump(hand);
                                }
                                // We can't win, get rid of the smallest card.
                                else return findLowestCard(hand);
                            }
                            // If we think that p3 doesn't have a spade
                            else
                            {
                                // If we can play a spade, play the lowest spade to win the trick.
                                if(hand[Game.suitToInt(Game.TRUMP)].size() > 0) return hand[Game.suitToInt(Game.TRUMP)].get(0); 
                                // We can't win, get rid of the smallest card.
                                else return findLowestCard(hand);
                            }
                        }
                    }
                }
            }
            // Evaluation for the third player.
            else
            {
                System.out.println("I'm player 3, table: " + table.length + " handH: " + hand[0].size() + " handC: " + hand[1].size() + " handD: " + hand[2].size() + " handS: " + hand[3].size());
                // If we have to obey the suit
                if(!selectedSuit.isEmpty())
                {
                    Card highestCardPlayed;
                    
                    // If p2 obeyed the suit, select the one with highest rank
                    if(table[1].suit == leadSuit)
                    {
                        if(table[0].rank > table[1].rank)
                        {
                            highestCardPlayed = table[0];
                        }
                        else
                        {
                            highestCardPlayed = table[1];
                        }
                    }
                    // If p2 didn't obey the suit
                    else
                    {
                        // If player 2 played a trump
                        if(table[1].suit == Game.TRUMP)
                        {
                            // p2 is trumping p1s card
                            highestCardPlayed = table[1];
                        }
                        // Otherwise it played a card outside of the suit that's not a trump
                        else
                        {
                            // p2 can't win, highest card is by p1
                            // scenario: trumps played by p2 didn't have any or p2 doesn't have the tick suit and didn't play any trumps
                            highestCardPlayed = table[0];
                        }
                    }
                    
                    System.out.println("I'm obeying the suit. Highest Card on the table: " + highestCardPlayed.toString());
                    
                    // If we have to play, but a spade has been played outside of its suit
                    if(leadSuit != Game.TRUMP && highestCardPlayed.suit == Game.TRUMP)
                    {
                        System.out.println("I can't win playing lowest obey suit");
                        // We can't win, play the lowest card
                        return selectedSuit.get(0);
                    }
                    else
                    {
                        System.out.println("Play higher or lowest suit card");
                        // Play a higher card of the obeyed suit or lowest
                        return playCardSuit(selectedSuit, highestCardPlayed);
                    }
                }
                // We don't have the suit
                else
                {
                    // If spades were played but we don't have any
                    if(leadSuit == Game.TRUMP)
                    {
                        // We can't win, get rid of the smallest card
                        return findLowestCard(hand); // handS not needed
                    }
                    // If we don't have to obey and the played suit is not spades
                    else
                    {
                        // We can't win, get rid of the smallest card.
                        if(hand[Game.suitToInt(Game.TRUMP)].isEmpty()) return findLowestCard(hand); // handS not needed
                        // If we don't have to obey and we can play a spade
                        else
                        {
                            // If player 2 played a trump, play winning trump or the smallest card we have
                            if(table[1].suit == Game.TRUMP) return playCardTrump(hand, table[1]); // want to avoid handS for findLowestCard?
                            // We don't have to obey and spades were not played.
                            else return hand[Game.suitToInt(Game.TRUMP)].get(0);
                        }
                    }
                }   
            }
        }
    }
    
    private static Card findLowestCard(List<Card> handH, List<Card> handC, List<Card> handD, List<Card> handS)
    {
        Card[] lowestCards = new Card[4];
        int index = 0;
        
        if(handH.size() > 0)
        {
            lowestCards[index] = handH.get(0);
            index++;
        }
        if(handC.size() > 0)
        {
            lowestCards[index] = handC.get(0);
            index++;
        }
        if(handD.size() > 0)
        {
            lowestCards[index] = handD.get(0);
            index++;
        }
        if(handS.size() > 0)
        {
            lowestCards[index] = handS.get(0);
            index++;
        }
        
        Card lowestCard = lowestCards[0];
        for(int i = 1; i < index; i++)
        {
            if(lowestCard.rank > lowestCards[i].rank)
            {
                lowestCard = lowestCards[i];
            }
        }
        System.out.println("lowest: " + lowestCard.toString());
        return lowestCard;
    }
    
    private static Card findHighestCard(List<Card> handH, List<Card> handC, List<Card> handD, List<Card> handS)
    {
        Card[] highestCards = new Card[4];
        int index = 0;
        
        if(handH.size() > 0)
        {
            highestCards[index] = handH.get(handH.size()-1);
            index++;
        }
        if(handC.size() > 0)
        {
            highestCards[index] = handC.get(handC.size()-1);
            index++;
        }
        if(handD.size() > 0)
        {
            highestCards[index] = handD.get(handD.size()-1);
            index++;
        }
        if(handS.size() > 0)
        {
            highestCards[index] = handS.get(handS.size()-1);
            index++;
        }
        
        Card highestCard = highestCards[0];
        for(int i = 1; i < index; i++)
        {
            if(highestCard.rank < highestCards[i].rank)
            {
                highestCard = highestCards[i];
            }
        }
        System.out.println("highest: " + highestCard.toString());
        return highestCard;
    }
    
    private static Card playCardSuit(List<Card> selectedSuit, Card highestCardPlayed)
    {
        // Go through each one of the cards of that suit that we have 
        for(int i = 0; i < selectedSuit.size() ; i++)
        {
            // If we have a card that's better than the highest card played
            if(selectedSuit.get(i).rank > highestCardPlayed.rank)
            {
                // Play the higher card (doesn't have to be the highest
                return selectedSuit.get(i);
            }
        }
        // If we can't beat what's on the table, play the lowest card
        return selectedSuit.get(0);
    }
    
    private static Card playCardTrump(List<Card> handH, List<Card> handC, List<Card> handD, List<Card> handS, Card highestSpadeCard)
    {
        // Check if we can beat the spade
        for(int i = 0; i < handS.size(); i++)
        {
            // If we can beat the spade, play our spade
            if(handS.get(i).rank > highestSpadeCard.rank)
            {
                return handS.get(i);
            }
        }
        // We can't beat the spade, play the smallest card
        return findLowestCard(handH, handC, handD, handS);    
    }
    
    /*
     * Heuristic for evaluating what is in the opponents hand.
     * Right now it is always paranoid.
     */
    private static Card getOpponentsHighestCardSuit(Suit suit)
    {
        if(suit == Suit.HEARTS)
            return Card.ACE_H;
        if(suit == Suit.CLUBS)
            return Card.ACE_C;        
        if(suit == Suit.DIAMONDS)
            return Card.ACE_D;
        return Card.ACE_S;
    }
    
    /** Use Monte Carlo tree search to evaluate the best move from this state. */
    public Card monteCarlo()
    {
        GameState root = new GameState(this, true);
        root.expand();
        
        // Run the search.
        for(int i = 0; i < Raptor.MC_SAMPLES; i++) root.playOut();
        
        // Identify the best card.
        GameState best = root.children.get(0);
        
        for(GameState child : root.children)
        {
            if(child.wins / child.playthroughs > best.wins / best.playthroughs) best = child;
        }
        return best.table[pos];
    }
    
    public int[] getScores() { return Arrays.copyOf(scores, 3); }
    
    /** Move the state forward by one turn. */
    public void advance(Card played)
    {
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
                
                if(beat) best = p;
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
            ArrayList<Card>[] hand = beliefs[pos].getHand();

            // Determine the suits from which cards can be played.
            int[] legal;
            int follow = -1;
            
            if(pos != order[0])
            {
                follow = Game.suitToInt(table[order[0]].suit);
                if(hand[follow].isEmpty()) follow = -1;
            }

            if(follow == -1) legal = new int[] {0, 1, 2, 3};
            else legal = new int[] {follow};

            for(int s : legal)
            {
                for(Card c : hand[s])
                {
                    GameState child = new GameState(this, false);
                    child.advance(c);
                    // Simulate predicted opponent moves.
                    child.advance(child.greedyEval());
                    child.advance(child.greedyEval());
                    children.add(child);
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

    private double UTC(double p)
    {
        return wins / playthroughs + Raptor.BIAS * Math.sqrt(Math.log(p) / playthroughs);
    }
 }
