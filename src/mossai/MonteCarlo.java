
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
