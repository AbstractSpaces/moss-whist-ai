/*
 *
 */
package mossai;

import java.util.*;

public class AI implements MSWAgent
{
    // Associate each card with an integer for easy reference.
    // Cards are ordered by rank, then by suit.
    private HashMap<Suit, Integer> suits;
    private HashMap<Card, Integer> deck;
    // Each belief[i] refers to the hand of the player at position i.
    // Each belief[i][j] is the probability of that player holding that card,
    // represented as an integer from 0-52. Divide this number by 52 to get the
    // probability.
    private int[][] belief;
    // Names are stored left to right from our position.
    private String names[];
    // Our position in the play order;
    private int pos;
    
    public AI()
    {
        suits = new HashMap(4);
        suits.put(Suit.HEARTS, 0);
        suits.put(Suit.CLUBS, 1);
        suits.put(Suit.DIAMONDS, 2);
        suits.put(Suit.SPADES, 3);
        
        deck = new HashMap(52);
        // Ranks start at 2, so we subtract 2 to index from 0.
        for(Card c : Card.values()) deck.put(c, c.rank - 2 + suits.get(c.suit));
        
        names = new String[] {"Me", "", ""};
        pos = -1;
    }
    
    /**
     * Tells the agent the names of the competing agents, and their relative position.
     */
    public void setup(String agentLeft, String agentRight)
    {
        names[1] = agentLeft;
        names[2] = agentRight;
    }

    /**
     * Starts the round with a deal of the cards.
     * The agent is told the cards they have (16 cards, or 20 if they are the leader)
     * and the order they are playing (0 for the leader, 1 for the left of the leader, and 2 for the right of the leader).
     */
    public void seeHand(List<Card> hand, int order)
    {
        pos = order;
        // Update the belief with our card.
        for(Card c : hand) belief[pos][deck.get(c)] = 52;
        // Initialise the probability distribution of unknown cards.
        int evenDist = 52 - hand.size();
        
        for(int i = 0; i < 52; i++)
        {
            if(belief[pos][i] == 0)
            {
                belief[(pos+1)%3][i] = evenDist;
                belief[(pos+2)%3][i] = evenDist;
            }
        }
    }
    // -------------------------------------------------------------------------    Discarding
    /**
     * This method will be called on the leader agent, after the deal.
     * If the agent is not the leader, it is sufficient to return an empty array.
     */
    public Card[] discard()
    {   
        if(pos == LEADER)
        {
            
        }
        else return new Card[4];
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
        if(table.length > 0)
        {
            Card lowestCard = hand[0];
            Card highestCard = hand[0];
            Card lowestCardSuit = null;
            Card highestCardSuit = null;
        
            Suit tickSuit = table[0].suit;
            
            for(int i = 0; i < hand.length; i++)
            {
                if(hand[i].suit == tickSuit)
                {
                    if(highestCardSuit != null)
                    {
                        if(hand[i].rank > highestCardSuit.rank)
                        {
                            highestCardSuit = hand[i];
                        }
                    }
                    else
                    {
                        highestCardSuit = hand[i];
                    }
                    
                    if(lowestCardSuit != null)
                    {
                        if(hand[i].rank < lowestCardSuit.rank)
                        {
                            lowestCardSuit = hand[i];
                        }
                    }
                    else
                    {
                        lowestCardSuit = hand[i];
                    }
                }
                
                if(hand[i].rank > highestCard.rank)
                {
                    highestCard = hand[i];
                }
                if(hand[i].rank < lowestCard.rank)
                {
                    lowestCard = hand[i];
                }
            }
            return highestCard;
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
