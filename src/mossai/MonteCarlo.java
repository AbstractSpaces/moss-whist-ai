package mossai;
import java.util.ArrayList;
import java.util.List;

/** Class for generating a state tree and performing a Monte Carlo search on it. */
public class MonteCarlo
{

    private static class GameGraph
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
    private static class Node
    {
        // Node's data related to the tree
        Node Parent;
        List<Node> Children = new ArrayList<Node>(); //Node[] children;        
        int LastVisitedChild = 0;
        
        // Node's data related to the game
        SampleState State;
        int Playthroughs;
        int Wins;
        
        
        /**
         * Generate the children of a node on the tree.
         * Will use state.opponentEval() to predict the two intermediate
         * opponent moves.
         */
        private void expand(GameGraph GameTree)
        {
            
        }
        
        /** Choose a child node as the next in a playout. */
        private int next()
        {
            return 0;
        }
        
        public int ChildrenSize()
        {
            return Children.size();
        }
        
        public Node GetChild(int i)
        {
            return Children.get(i);
        }
    }
    
    /**
     * Run Monte Carlo tree search from a sample state to evaluate the best
     * available move.
     */
    public static Card search(SampleState root)
    {
        // Setup the search tree
        GameGraph GameTree = new GameGraph();
        
        // root node
        Node parentNode = new Node();

        // Add the state
        //parentNode.State
        
        // Add the root node
        GameTree.AddNode(parentNode);
        
        return null;
    }
    
    private void createATree(GameGraph GameTree)
    {
        Node parentNode = GameTree.GetNode(0);
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
                    parentNode.Parent.Wins += parentNode.Wins;

                    // get the parent of the current node
                    parentNode = parentNode.Parent;
                    
                    traverseBack--;
                    depth--;
                }
            }
            
            // create a node for the game state
            parentNode.expand(GameTree);
            
            depth++;

            if(depth >= maxDepth || parentNode.ChildrenSize() > 0)
            {
                // traverse back one level
                traverseBack = 1;
               
                // if win == true
                //parentNode.Ratio++;
                // else
                //parentNode.Ratio--;
            }
            else
            {
                // pick which node to play out next
                nextVisit = parentNode.next();
                
                // playout that game
                traverseBack = playOut(GameTree, GameTree.GetNode(nextVisit));
                
                // if win == true
                //parentNode.Ratio++;
                // else
                //parentNode.Ratio--;
                
            }
        }
    }
    
    
    /** Simulate a game to its end, then back-propagate the result. */
    private int playOut(GameGraph GameTree, Node parent)
    {
        Card playCard1, playCard2, playCard3;
        
        int turnsCount = 0;
        
        boolean gameOver = false;
        
        while(true)
        {            
            // p1 take the turn
            List<Card>[] hand1 = parent.State.hand1;
            playCard1 = GreedyAi.greedyTurn(new Card[0], hand1[0], hand1[1], hand1[2], hand1[3]);
            
            // p2 take the turn
            List<Card>[] hand2 = parent.State.hand2;
            playCard2 = GreedyAi.greedyTurn(new Card[] { playCard1 }, hand2[0], hand2[1], hand2[2], hand2[3]);
            
            // p3 take the turn
            List<Card>[] hand3 = parent.State.hand3;
            playCard3 = GreedyAi.greedyTurn(new Card[] { playCard1, playCard2 }, hand3[0], hand3[1], hand3[2], hand3[3]);
            
            // create node for this turn
            Node node = new Node();
            
            node.State = new SampleState(parent.State, playCard1);
            
            turnsCount++;
            if(gameOver == true)
            {
                break;
            }
        }
        return turnsCount;
    }
}


/*
package mossai;

import java.util.ArrayList;
import java.util.List;

public class MonteCarlo
{
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
    
    
    
    private void branch(GameGraph tree, Node parentNode)
    {
    
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
*/