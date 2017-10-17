/*
 *
 */
package mossai;

import java.util.*;

public class AI implements MSWAgent
{
    private List<List> myHand = new ArrayList<List>();
    private List<Card> gameHand;
    private final String myName = "Halo";
    private int myOrder = 0;
    private String[] agents = new String[3];
    // -------------------------------------------------------------------------
    
    public AI()
    {
        for(int i = 0; i < 4; i++)
        {
            myHand.add(new ArrayList<Card>());
        }
    }
    
    /**
     * Tells the agent the names of the competing agents, and their relative position.
     */
    public void setup(String agentLeft, String agentRight)
    {
        
    }

    /**
     * Starts the round with a deal of the cards.
     * The agent is told the cards they have (16 cards, or 20 if they are the leader)
     * and the order they are playing (0 for the leader, 1 for the left of the leader, and 2 for the right of the leader).
     */
    public void seeHand(List<Card> hand, int order)
    {
        // seeHand seems to be called only once, at the start of the game
        
        // assign the order
        myOrder = order;
        agents[order] = myName;
        
        gameHand = hand;
        
        // sort the hand in the ascending order in each suit 
        sortMyHand(hand);
        
        System.out.println("initial hand: " + printHand(hand) + "\nspades\t\t" + printHand(myHand.get(0)) + "\ndiamonds\t" + printHand(myHand.get(1)) + "\nclubs\t\t" + printHand(myHand.get(2)) + "\nhearts\t\t" + printHand(myHand.get(3)));
    }
   
    /**
     *  Allocate the cards to the appropriate suit, then sort 
     */
    private void sortMyHand(List<Card> hand)
    {
        for(int i = 0; i < hand.size(); i++)
        {
            switch(hand.get(i).suit)
            {
                case SPADES: 
                {
                    addToHand(myHand.get(0), hand.get(i));
                    break;
                }
                case DIAMONDS: 
                {
                    addToHand(myHand.get(1), hand.get(i));
                    break;
                }
                case CLUBS: 
                {
                    addToHand(myHand.get(2), hand.get(i));
                    break;
                }
                case HEARTS: 
                {
                    addToHand(myHand.get(3), hand.get(i));
                    break;
                }
            }
        }
    }
    
    /**
     *  Allocate the card to the appropriate suit,
     *  sort in the ascending order
     */
    private void addToHand(List<Card> suitHand, Card card)
    {
        int i = 0;
        for(i = 0; i < suitHand.size(); i++)
        {
            // if smaller get the index
            if(card.rank < suitHand.get(i).rank)
            {
                break;
            }
        }
        // add the card to the list
        suitHand.add(i, card);
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
        
        if(myOrder == LEADER)
        {
            //if(myDiamonds.size() < 2)
            // other ideas: check if you can get rid of some suit so that you can use your trumps
            
            // default: discard the lowest cards, avoid the trump
            discardLowestNoTrump(cards, toDiscard);
        }
        
        return cards;
    }
    
    private void discardLowestNoTrump(Card[] cards, int toDiscard)
    {
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
        }
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
        return myName;
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
        public int LastVisitedChild = 0;

        public Card[] Opponent1 = new Card[52];
        public Card[] Opponent2 = new Card[52];
        
        public int Ratio = 0;
        
        public int ChildrenSize()
        {
            return Children.size();
        }
        
        public Node GetChild(int i)
        {
            return Children.get(i);
        }
    }
}
