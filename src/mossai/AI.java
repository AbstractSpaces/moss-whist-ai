/*
 *
 */
package mossai;

import java.util.*;

public class AI implements MSWAgent
{
    // Associate each card with an integer for easy reference.
    private HashMap<Suit, Integer> suits;
    private HashMap<Card, Integer> deck;
    // Each belief[i][j] is the inverse of the probability that player i has card j.
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
        for(Card c : Card.values()) deck.put(c, suits.get(c.suit)*13 + c.rank);
        
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
        for(Card c : hand) belief[pos][deck.get(c)] = 1;
        // Initialise the probability distribution of unknown cards.
        int evenDist = 52 - hand.size();
        
        for(int i = 0; i < 52; i++)
        {
            if(belief[pos][i] != 1)
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
        int toDiscard = 4;
        Card[] cards = new Card[toDiscard];
        
        /*if(myOrder == LEADER)
        {
            //if(myDiamonds.size() < 2)
            // other ideas: check if you can get rid of some suit so that you can use your trumps
            
            // default: discard the lowest cards, avoid the trump
            discardLowestNoTrump(cards, toDiscard);
        }
        */
        return cards;
    }
    
    private void discardLowestNoTrump(Card[] cards, int toDiscard)
    {
        /*
        int discarded = 0, suit = 0;
        Card lowest = gameHand.get(0);
        
        while(discarded < toDiscard)
        {
            for(int i = 1; i < 4; i++)
            {
                if(myHand.get(i).size() > 0)
                {
                    if(lowest.rank > ((Card)myHand.get(i).get(0)).rank)
                    {
                        lowest = (Card)myHand.get(i).get(0);
                        suit = i;
                    }
                }
            }
            cards[discarded] = (Card)myHand.get(suit).remove(0);
            discarded++;
        }*/
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
    
    private void createGameTree()
    {
        // Create the game tree
        GameGraph GameTree = new GameGraph();
        
        // root node
        Node parentNode = new Node();

        // Add the root node
        GameTree.AddNode(parentNode);
        
        // loop control
        int maxDepth = 5;
        int depth = 0;
        boolean traverseBack = false;
        int lastVisted = 0;
        
        while(true)
        {
            if(traverseBack)
            {
                // get the parent of the current node
                parentNode = parentNode.Parent;
                
                depth--;
                
                // check if not made it back to the root node
                if(parentNode != null)
                {
                    
                }
                else
                {
                    break;
                }
            }
            
            branch(GameTree, parentNode);
            
            depth++;

            if(depth >= maxDepth || parentNode.ChildrenSize() > 0)
            {
                traverseBack = true;
            }
            else
            {
                lastVisted = parentNode.LastVisitedChild;
                if(lastVisted < parentNode.ChildrenSize())
                {
                    parentNode.LastVisitedChild++;
                    parentNode = parentNode.GetChild(lastVisted);
                }
                else
                {
                    traverseBack = true;
                }
            }
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
        return "halo";
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
        public int Score = 0;
        public Card[] playedSoFar = new Card[2];
        
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
