package mossai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * Encapsulation of the concrete knowledge about the game's state at a point in time.
 * @author dj
 */
public class GameState
{
    private static Raptor agent;
    
    /** The indices of players in their turn order for this trick. */
    private int[] order;
    
    /** The player currently taking their turn, having not yet played their card. */
    private int turn;
    
    /** The cards so far played by each player this trick. */
    private final Card[] table;
    
    /** Running tally of the scores. */
    private final int[] scores;
    
    /** Construct a blank state for a new game. */
    public GameState(Raptor ai, int leader)
    {
        agent= ai;
        order = new int[] {leader, (leader+1)%3, (leader+2)%3};
        turn = order[0];
        table = new Card[3];
        scores = new int[] {-8, -4, -4};
    }
    
    /** Copy a state. */
    private GameState(GameState old)
    {
        order= Arrays.copyOf(old.order, 3);
        turn = old.turn;
        table = Arrays.copyOf(old.table, 3);
        scores = Arrays.copyOf(old.scores, 3);
    }
    
    /** Use a fixed, greedy strategy to determine the active player's move from a state. */
    public static Card greedyEval(GameState state, BeliefState belief)
    {
        
        // If the player has to follow suit.
        /*if(!hand[Game.suitToInt(leadSuit)].isEmpty())
        {

        }*/
        
        ArrayList<Card>[] hand = belief.getHand();
        List<Card> handH = hand[0];
        List<Card> handC = hand[1];
        List<Card> handD = hand[2];
        List<Card> handS = hand[3];
        
        // Evaluation for the first player.
        if(state.turn == state.order[0])
        {
            // As a leader, we want to play the highest card to keep on wining
            
            // if we think we can beat p2 && p3
            // else
                // if risk worth taking 
            
            // If we think that p2 or p3 have a higher card in that same suit
            //return findLowestCard(handH, handC, handD, handS);
            // else we can play
            return findHighestCard(handH, handC, handD, handS);
        }
        else
        {
            Card[] table = state.table;
            Card winning = state.table[state.order[0]];
            Suit leadSuit = winning.suit;
            
            List<Card> selectedSuit = hand[Game.suitToInt(leadSuit)];
            
            // Evaluation for the second player.
            if(state.turn == state.order[1])
            {
                //hasCard = 100% certain
                
                // trashold ? chance, positive negative
                
                // checking for higher card =
                    // loop from my high card to highest card 
                        // check with chance(card, player)
                
                // check for suit
                
                System.out.println("I'm player 2, table: " + table.length + " handH: " + handH.size() + " handC: " + handC.size() + " handD: " + handD.size() + " handS: " + handS.size());
                
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
                        return findLowestCard(handH, handC, handD, handS); // handS not needed
                    }
                    // If spades are not the tick suit
                    else
                    {
                        // If we think that p3 has to obey
                        if(opponentHasSuit(leadSuit) == true)
                        {
                            // If we can play a spade
                            if(handS.size() > 0)
                            {
                                // Play the lowest spade to win the tick
                                return handS.get(0);
                            }
                            // We don't have to obey and we don't have any spades
                            else
                            {
                                // We can't win, get rid of the smallest card
                                return findLowestCard(handH, handC, handD, handS);
                            }
                        }
                        // If we think p3 doesn't have to obey the suit too
                        else
                        {
                            // If we think that p3 has spades
                            if(opponentHasSuit(Suit.SPADES) == true)
                            {
                                // If we can play a spade
                                if(handS.size() > 0)
                                {
                                    // get p3s highest trump
                                    Card p3 = getOpponentsHighestCardSuit(Suit.SPADES);
                                    
                                    // play a higher spade or the lowest card we have
                                    return playCardTrump(handH, handC, handD, handS, p3);
                                }
                                else
                                {
                                    // We can't win, get rid of the smallest card
                                    return findLowestCard(handH, handC, handD, handS);
                                }
                            }
                            // If we think that p3 doesn't have a spade
                            else
                            {
                                // If we can play a spade
                                if(handS.size() > 0)
                                {
                                    // Play the lowest spade to win the tick
                                    return handS.get(0);
                                }
                                else
                                {
                                    // We can't win, get rid of the smallest card
                                    return findLowestCard(handH, handC, handD, handS);
                                }
                            }
                        }
                    }
                }
            }
            // Evaluation for the third player.
            else
            {
                System.out.println("I'm player 3, table: " + table.length + " handH: " + handH.size() + " handC: " + handC.size() + " handD: " + handD.size() + " handS: " + handS.size());
                // If we have to obey the suit
                if(!selectedSuit.isEmpty())
                {
                    Card highestCardPlayed = table[0];
                    
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
                        if(table[1].suit == Suit.SPADES)
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
                    
                    // If we have to play, but a spade has been played outside of it's suit
                    if(leadSuit != Suit.SPADES && highestCardPlayed.suit == Suit.SPADES)
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
                    if(leadSuit == Suit.SPADES)
                    {
                        // We can't win, get rid of the smallest card
                        return findLowestCard(handH, handC, handD, handS); // handS not needed
                    }
                    // If we don't have to obey and the played suit is not spades
                    else
                    {
                        // We don't have any spades either
                        if(handS.size() == 0)
                        {
                            // We can't win, get rid of the smallest card
                            return findLowestCard(handH, handC, handD, handS); // handS not needed
                        }
                        // If we don't have to obey and we can play a spade
                        else
                        {
                            // If player 2 played a trump
                            if(table[1].suit == Suit.SPADES)
                            {
                                // Play winning trump or the smallest card we have
                                return playCardTrump(handH, handC, handD, handS, table[1]); // want to avoid handS for findLowestCard?
                            }
                            else
                            {
                                // We don't have to obey and spades were not played
                                return handS.get(0);
                            }
                        }
                    }
                }   
            }
        }
    }
    public static Card minCard(Card a, Card b)
    {
        return a.rank > b.rank ? b : a;
    }
    public static Card maxCard(Card a, Card b)
    {
        return a.rank < b.rank ? b : a;
    }
    
    public static Card findLowestCard(List<Card> handH, List<Card> handC, List<Card> handD, List<Card> handS)
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

    
    public static Card findHighestCard(List<Card> handH, List<Card> handC, List<Card> handD, List<Card> handS)
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
    
    public static Card playCardSuit(List<Card> selectedSuit, Card highestCardPlayed)
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
    
    
    
    // eval functions
    
    public static Card playCardTrump(List<Card> handH, List<Card> handC, List<Card> handD, List<Card> handS, Card highestSpadeCard)
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
    
    public static boolean opponentHasSuit(Suit suit)
    {
        return false;
    }
    
    
    /*
        Heuristic for evaluating what is in the opponents hand. 
        Right now it is always paranoid.
    */
    public static Card getOpponentsHighestCardSuit(Suit suit)
    {
        if(suit == Suit.HEARTS)
            return Card.ACE_H;
        if(suit == Suit.CLUBS)
            return Card.ACE_C;        
        if(suit == Suit.DIAMONDS)
            return Card.ACE_D;
        return Card.ACE_S;
    }
    
    
    
    
    
    
    
    
    
    
    /** Use Monte Carlo tree search to evaluate the best move from a state. */
    public static Card monteCarlo(GameState state, BeliefState belief, BeliefState history)
    {
        int[] sample = belief.sampleState();
        
        Simulation root = state.new Simulation(sample, belief, history);
        root.expand();
        
        // Run the search.
        for(int i = 0; i < agent.MCsamples; i++) root.playOut();
        
        // Identify the best card.
        Simulation best = root.children.get(0);
        for(Simulation child : root.children)
        {
            if(child.wins / child.playthroughs > best.wins / best.playthroughs) best = child;
        }
        return best.prevMove;
    }
    
    public GameState clone() { return new GameState(this); }
    
    public int getPlayer(int o) { return order[o]; }
    
    public int[] getScores() { return Arrays.copyOf(scores, 3); }
    
    public Card getLead() { return table[order[0]]; }

    
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
                {
                    if(table[p].suit == Game.TRUMP && table[p].rank > table[best].rank) beat = true;
                }
                else if(table[p].suit == table[best].suit && table[p].rank > table[best].rank) beat = true;
                
                if(beat) best = p;
            }
            
            Arrays.fill(table, null);
            scores[best]++;
            order[0] = best;
            order[1] = (best + 1) % 3;
            order[2]= (best + 2) % 3;
        }
    }
    
   /**
    * Encapsulates the state of a simulated game for use with Monte Carlo.
    * @author Dylan Johnson
    */
    private class Simulation
    {
        /** The move that lead from the parent node to this one. */
        private final Card prevMove;
        
        private GameState state;
        
        /** The beliefs held by the agent and the two simulated opponents. */
        private BeliefState[] beliefs;

        private double playthroughs;
        private double wins;
        private ArrayList<Simulation> children;

        /** Start a new tree from a sampled state. */
        private Simulation(int[] sCards, BeliefState belief, BeliefState history)
        {
            prevMove = null;
            state = GameState.this;
            beliefs = new BeliefState[3];

            for(int i = 0; i < 3; i++)
            {
                if(i == agent.pos) beliefs[i] = belief.clone();
                else beliefs[i] = new BeliefState(i, history, sCards);
            }

            playthroughs = 0.0;
            wins = 0.0;
            children = null;
       }
        
        /** Clone a previous node's state and enact a move by the agent. */
        private Simulation(Simulation prev, Card move)
        {
            prevMove = move;
            state = prev.state.clone();
            state.advance(move);
            
            beliefs = new BeliefState[3];
            for(int i = 0; i < 3; i++)
            {
                beliefs[i] = prev.beliefs[i].clone();
                beliefs[i].cardPlayed(move, agent.pos, prev.state.table[prev.state.order[0]]);
            }
            
            playthroughs = 0.0;
            wins = 0.0;
            children = null;
        }

       /** Generate the children of a node on the tree. */
       private void expand()
       {
           // Enumerate legal moves, initialise child array to correct size.
           // Clone the node and modify it with the move represented by each child.
           // Predict the two opponent moves after that and modify the child.
           // with those moves.
           // Update score and table as appropriate.
           if(children == null)
           {
               children = new ArrayList();
               ArrayList<Card>[] hand = beliefs[agent.pos].getHand();
               
               // Determine the suits from which cards can be played.
               int[] legal;
               
               int follow = -1;
               if(agent.pos != state.order[0])
               {
                   follow = Game.suitToInt(state.table[state.order[0]].suit);
                   if(hand[follow].isEmpty()) follow = -1;
               }
               
               if(follow == -1) legal = new int[] {0, 1, 2, 3};
               else legal = new int[] {follow};
               
               for(int s : legal)
               {
                   for(Card c : hand[s])
                   {
                       Simulation child = new Simulation(this, c);
                       
                       // Simulate predicted opponent moves.
                       for(int i = 1; i < 3; i++)
                       {
                           int opponent = (agent.pos + i) % 3;
                           child.state.advance(GameState.greedyEval(child.state, child.beliefs[opponent]));
                       }
                       
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
               if(agent.drawWins) return scores[agent.pos] >= scores[(agent.pos+1)%3] && scores[agent.pos] >= scores[(agent.pos+2)%3];
               else return scores[agent.pos] > scores[(agent.pos+1)%3] && scores[agent.pos] > scores[(agent.pos+2)%3];
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
           return wins / playthroughs + agent.bias * Math.sqrt(Math.log(p) / playthroughs);
       }
    }
}
