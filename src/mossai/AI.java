/*
 *
 */
package mossai;

import java.util.*;

// git pull

// git pull -s ours

// git puls -s theirs

// git add .
// git commit
// git push origin master

public class AI implements MSWAgent
{
    /** An encapsulation of our knowledge/beliefs about an opposing agent. */
    private class Opponent
    {
        int pos;
        
        private String name;
        
        /**
         * Each belief element maps a card to the probability of the opponent
         * holding that card.
         */
        private HashMap<Card, Double> belief;
        
        // The number of unknown cards in the opponent's hand.
        private double unknowns;
        
        private Opponent() { belief = new HashMap(52); }
        
        /**
         * Initialise the belief probabilities using what we know about our own
         * hand.
         */
        private void init(List<Card> hand, int p)
        {
            pos = p;
            
            // Cards not in our hand have an even chance of being in this opponent's hand.
            if(pos == LEADER) unknowns = 20.0;
            else unknowns = 16.0;
            
            for(Card c : Card.values()) belief.put(c, unknowns/pool);
            for(Card h : hand) belief.put(h, 0.0);
        }

    }
    
    // Map for indexing into our hand array.
    private HashMap<Suit, Integer> suits;
    
    private String name;
    
    // Opponents are counted from our left.
    private Opponent[] opponents;
    
    // Our position in the order.
    private int pos;
    
    // Cards in our hand.
    private ArrayList<Card>[] hand;
    
    // Cards played so far this turn.
    private Card[] table;
    
    // The number of cards with uncertain location.
    private double pool;

    public AI()
    {
        suits = new HashMap();
        suits.put(Suit.HEARTS, 0);
        suits.put(Suit.CLUBS, 1);
        suits.put(Suit.DIAMONDS, 2);
        suits.put(Suit.SPADES, 3);
        
        name = "Halo";
        opponents = new Opponent[2];
        table = new Card[3];
        
        hand = new ArrayList[4];
        for(ArrayList suit : hand) suit = new ArrayList(13);
        pool = 52.0;
    }

    /**
     * Tells the agent the names of the competing agents, and their relative position.
     */
    public void setup(String agentLeft, String agentRight)
    {
        opponents[0].name = agentLeft;
        opponents[1].name = agentRight;
    }

    /**
     * Starts the round with a deal of the cards.
     * The agent is told the cards they have (16 cards, or 20 if they are the leader)
     * and the order they are playing (0 for the leader, 1 for the left of the leader, and 2 for the right of the leader).
     */
    public void seeHand(List<Card> deal, int order)
    {
        pos = order;
        pool -= deal.size();
        
        // Initialise beliefs about opponent hands.
        for(int i = 0; i < 2; i++) opponents[i].init(deal, (order+i)%3);
        
        // Sort cards based on rank before assigning them to hand.
        deal.sort(new CardCompare());
        
        // Insert ordered cards into hand.
        for(Card c : deal) hand[suits.get(c.suit)].add(c);
    }
    
    /**
     * This method will be called on the leader agent, after the deal.
     * If the agent is not the leader, it is sufficient to return an empty array.
     */
    public Card[] discard()
    {
        Card[] picks = new Card[4];
        int i = 0;
        
        if(pos == LEADER)
        {
            while(i < 4)
            {
                picks[i] = evalDiscard();
                hand[suits.get(picks[i].suit)].remove(picks[i]);
                i--;
            }
        }
        
        return picks;
    }
    
    /**
     * Evaluate cards in the hard for the best to discard.
     * @return The chosen card.
     */
    private Card evalDiscard()
    {
        // TODO: Write a formula than can improve through learning.
        return null;
    }

    // -------------------------------------------------------------------------
    /**
     * Agent returns the card they wish to play.
     * A 200 ms timelimit is given for this method
     * @return the Card they wish to play.
     */
    public Card playCard()
    {
        return null;
    }
    
    private void createStateTree()
    {
        // Create the tree
        GameGraph GameTree = new GameGraph();
        
        // root node
        Node parentNode = new Node();

        // Add the root node
        GameTree.AddNode(parentNode);
        
        expandTree(GameTree, 0);
    }
    
    private void expandTree(GameGraph GameTree, int parent)
    {
        // set up the root
        Node parentNode = GameTree.GetNode(parent);
        parentNode.Parent = null;
        
        // loop control
        int maxDepth = 5;
        int depth = 0;
        int traverseBack = 0;
        int nextVisit = 0;
        
        long startTime = System.currentTimeMillis();
        
        while((System.currentTimeMillis()-startTime) < 180)
        {
            if(traverseBack > 0)
            {
                while(traverseBack != 0)
                {  
                    // check if made it back to the root node
                    if(parentNode.Parent == null)
                    {
                        traverseBack = 0;
                        depth = 0;
                        break;
                    }
                    
                    // save child's score into the parent node
                    parentNode.Parent.Ratio += parentNode.Ratio;

                    // get the parent of the current node
                    parentNode = parentNode.Parent;
                    
                    traverseBack--;
                    depth--;
                }
            }
            
            // create a node for the game state
            branch(GameTree, parentNode);
            
            depth++;

            if(depth >= maxDepth || parentNode.ChildrenSize() > 0)
            {
                // traverse back one level
                traverseBack = 1;
               
                // if win == true
                parentNode.Ratio++;
                // else
                parentNode.Ratio--;
            }
            else
            {
                // pick which node to expand next
                nextVisit = evaluateNexVisit(parentNode);
                
                // traverse to that node
                if(nextVisit >= 0)
                {
                    parentNode = parentNode.GetChild(nextVisit);
                }
                else
                {
                    // no more nodes, traverse back one level
                    traverseBack = Math.abs(nextVisit);

                    // if win == true
                    parentNode.Ratio++;
                    // else
                    parentNode.Ratio--;
                }
            }
        }
    }

    // -n if it should go back n levels
    private int evaluateNexVisit(Node parentNode)
    {   
        // go to the next one
        parentNode.LastVisitedChild++;
        if(parentNode.LastVisitedChild < parentNode.ChildrenSize())
        {
            return parentNode.LastVisitedChild;
        }
        else
        {
            return -1;
        }
    }
    
    private Card greedyTurn(Card[] table, Card[] hand)
    {
        // if there are cards on the table (played in this tick)
        if(table.length > 0)
        {
            Card lowestCard = hand[0];
            Card lowestCardTrump = null;
            Card highestCardTrump = null;
            Card lowestCardSuit = null;
            Card highestCardSuit = null;
        
            // we have to obey the suit played by the leader
            Suit tickSuit = table[0].suit;
            
            // for each card in our hand
            for(int i = 0; i < hand.length; i++)
            {
                // if the card matches the played suit
                if(hand[i].suit == tickSuit)
                {
                    // if it is not the first card of that stuit that we found
                    if(highestCardSuit != null && lowestCardSuit != null)
                    {
                        // if it's the highest card of that suit in our hand
                        if(hand[i].rank > highestCardSuit.rank)
                        {
                            highestCardSuit = hand[i];
                        }
                        
                        // if it is the lowest card of that suit in our hand
                        if(hand[i].rank < lowestCardSuit.rank)
                        {
                            lowestCardSuit = hand[i];
                        }
                    }
                    else
                    {
                        highestCardSuit = hand[i];
                        lowestCardSuit = hand[i];
                    }
                }
                else
                {
                    // if it is a trump card
                    if(hand[i].suit == Suit.SPADES)
                    {
                        // if it's the first trump we found
                        if(highestCardTrump != null && lowestCardTrump != null)
                        {
                            // if it is the highest trump card
                            if(hand[i].rank > highestCardTrump.rank)
                            {
                                highestCardTrump = hand[i];
                            }
                            // if it is the lowest trump card
                            if(hand[i].rank < lowestCardTrump.rank)
                            {
                                lowestCardTrump = hand[i];
                            }
                        }
                        else
                        {
                            // assign the first found trump card
                            lowestCardTrump = hand[i];
                            highestCardTrump = hand[i];
                        }
                    }
                    else
                    {
                        // find the lowest card that is not a trump
                        if(hand[i].rank < lowestCard.rank)
                        {
                            lowestCard = hand[i];
                        }
                    }
                }
            }
            // assuming we have to play the suit
            if(lowestCardSuit != null)
            {
                // check if we can beat what's on the table
                for(int i = 0; i < table.length; i++)
                {
                    // if our card of the same suit can't beat what's already on the table
                    if(highestCardSuit.rank < table[i].rank || (tickSuit != Suit.SPADES && table[i].suit == Suit.SPADES))
                    {
                        return lowestCardSuit;
                    }
                }
                // otherwise we think that we can beat what is on the table
                return highestCardSuit;
            }
            // if we don't have to play the suit
            else
            {
                // if the suit we have to obey is trumps and we don't have it, we can't win no matter what
                if(tickSuit == Suit.SPADES)
                {
                    return lowestCard;
                }
                else
                {
                    // if we are going last
                    if(table.length == 2)
                    {
                        for(int i = 0; i < table.length; i++)
                        {
                            //if()
                        }
                    }
                }
                // check if we can play the low trump
                    // if we are going third = no other trumps or
                    // or if we care certain that the opponent is deffs playing the suit or doesnt have trumps
                
                // check if we can play the high trump
                    // if we are going third,
                    // or if we are certain that opponent is playing a trump and we can beat him
                
                // otherwise we get rid of the lowest card
                return null;
            }
        }
        else
        {
            Card highestCard = hand[0];
            Card highestCardNotTrump = hand[0];
            
            for(int i = 0; i < hand.length; i++)
            {
                if(hand[i].rank > highestCard.rank)
                {
                    highestCard = hand[i];
                }
                if(hand[i].suit != Suit.SPADES)
                {
                    
                }
            }
            return highestCard;
        }
    }
    
    private void branch(GameGraph tree, Node parentNode)
    {
    
    }
    
    /**
     * Sees an Agent play a card.
     * A 50 ms timelimit is given to this function.
     * @param card, the Card played.
     * @param agent, the name of the agent who played the card.
     */
    public void seeCard(Card card, String agent)
    {
        
    }

    /**
     * See the result of the trick. 
     * A 50 ms timelimit is given to this method.
     * This method will be called on each eagent at the end of each trick.
     * @param winner, the player who played the winning card.
     */
    public void seeResult(String winner)
    {
    }
    
    /**
     * See the score for each player.
     * A 50 ms timelimit is givien to this method
     * @param scoreboard, a Map from agent names to their score.
     */
    public void seeScore(Map<String, Integer> scoreboard)
    {
    }

    /**
     * @return the Agents name.
     * A 10ms timelimit is given here.
     * This method will only be called once.
     */
    public String sayName()
    {
        return names[0];
    }
    
    // ------------------------------------------------------------------------- 
     
    private void copyHand(List<Card> a, List<Card> b)
    {
        for(int i = 0; i < a.size(); i++)
        {
            b.add(a.get(i));
        }
    }
    
    private String printHand(List<Card> cards)
    {
        StringBuilder output = new StringBuilder();
        
        for(int i = 0; i < cards.size(); i++)
        {
            output.append(cards.get(i).toString() + ", ");
        }
        
        return output.toString();
    }
    
    // -------------------------------------------------------------------------    Data Structure
    
    private class GameGraph
    {
        public List<Node> Nodes = new ArrayList<Node>();
        
        public int Size()
        {
            return Nodes.size();
        }
        
        public void AddNode(Node node)
        {
            Nodes.add(node);
        }
        
        public Node GetNode(int i)
        {
            return Nodes.get(i);
        }      
    }
    
    private class Node
    {
        public Node Parent = null;
        public List<Node> Children = new ArrayList<Node>();
        public List<Card> MyHand = new ArrayList<Card>();
        public int LastVisitedChild = 0;

        public Card[] Opponent1 = new Card[52];
        public Card[] Opponent2 = new Card[52];
        
        public int MyScore = 0;
        public int Opscore1 = 0;
        public int Opscore2 = 0;
        
        public int Ratio = 0;
        
        public int ChildrenSize()
        {
            return Children.size();
        }
        
        public Node GetChild(int i)
        {
            return Children.get(i);
        }  
        
        public int HandSize()
        {
            return MyHand.size();
        }

    }

}
