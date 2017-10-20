/**
 *
 * @author Martin
 */
package mossai;

import java.util.*;

public class GreedyAi
{
    public Card minCard(Card a, Card b)
    {
        return a.rank > b.rank ? b : a;
    }
    public Card maxCard(Card a, Card b)
    {
        return a.rank < b.rank ? b : a;
    }
    
    public Card findLowestCard(List<Card> handH, List<Card> handC, List<Card> handD, List<Card> handS)
    {
        int index = 0;
        Card[] lowestCards = new Card[4];
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
            if(lowestCard.rank > lowestCards[index].rank)
            {
                lowestCard = lowestCards[index];
            }
        }
        return lowestCard;
    }

    public Card playCardSuit(List<Card> selectedSuit, Card highestCardPlayed)
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
    
    public Card playCardTrump(List<Card> handH, List<Card> handC, List<Card> handD, List<Card> handS, Card highestCard)
    {
        // Check if we can beat the spade
        for(int i = 0; i < handS.size(); i++)
        {
            // If we can beat the spade, play our spade
            if(handS.get(i).rank > highestCard.rank)
            {
                return handS.get(i);
            }
        }
        // We can't beat the spade, play the smallest card
        return findLowestCard(handH, handC, handD, handS);    
        
    }
    
    public Card greedyTurn(Card[] table, List<Card> handH, List<Card> handC, List<Card> handD, List<Card> handS)
    {
        // if there are cards on the table (played in this tick)
        if(table.length > 0)
        {
            // We have to obey the suit played by the leader
            Suit tickSuit = table[0].suit;
            List<Card> selectedSuit = handH;   
            switch (tickSuit)
            {
                case HEARTS:
                {
                    selectedSuit = handH;
                    break;
                }
                case CLUBS:
                {
                    selectedSuit = handC;
                    break;
                }
                case DIAMONDS:
                {
                    selectedSuit = handD;
                    break;
                }
                case SPADES:
                {
                    selectedSuit = handS;
                    break;
                }
            }
            
            int selectedSuitSize = selectedSuit.size();
            
            // If we are player 3
            if(table.length == 2)
            {
                // If we have to obey the suit
                if(selectedSuitSize > 0)
                {
                    Card highestCardPlayed = table[0];
                    
                    // If p2 obeyed the suit, select the one with highest rank
                    if(table[1].suit == tickSuit)
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
                    
                    // If we have to play, but a spade has been played outside of it's suit
                    if(tickSuit != Suit.SPADES && highestCardPlayed.suit == Suit.SPADES)
                    {
                        // We can't win, play the lowest card
                        return selectedSuit.get(0);
                    }
                    else
                    {
                        // Play a higher card of the obeyed suit or lowest
                        return playCardSuit(selectedSuit, highestCardPlayed);
                    }
                }
                // We don't have the suit
                else
                {
                    // If spades were played but we don't have any
                    if(tickSuit == Suit.SPADES)
                    {
                        // We can't win, get rid of the smallest card
                        return findLowestCard(handH, handC, handD, handS); // handS not needed
                    }
                    // If we don't have to obey and the played suit is not spades
                    else
                    {
                        // We don't have any spades either
                        if(handS.size() != 0)
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
            // If we are player 2 (p2)
            else
            {
                boolean pobey = true;
                
                // If we have to obey the suit
                if(selectedSuitSize > 0)
                {   
                    // If we think that p3 has to obey too
                    if(pobey = true)
                    {
                        // get his highest card
                        // Card highestCardPlayed = table[0].rank > p3.rank ? table[0] : p3;
                        // return playCardSuit(selectedSuit, highestCardPlayed);
                    }
                    // If we think that p3 doesn't have to obey
                    else
                    {
                        /* 
                        if we think that p3 is going to play a spade
                        {
                            // We can't win against a spade, play the lowest card
                            return selectedSuit.get(0);
                        }
                        // If p3 is not going to play a spade
                        else
                        {
                            // play a higher card or lowest card of the suit
                            return playCardSuit(selectedSuit, table[0]);
                        }
                        */
                    }
                }
                // We don't have to obey the suit
                else
                {
                    // If spades were played but we don't have any
                    if(tickSuit == Suit.SPADES)
                    {
                        // We can't win, get rid of the smallest card
                        return findLowestCard(handH, handC, handD, handS);
                    }
                    else
                    {
                        // If we think that p3 has to obey
                        if(pobey = true)
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
                            /*
                            If we think that p3 has spades
                            {
                                // If we can play a spade
                                if(handS.size() > 0)
                                {
                                    // If his spades are higher
                                }
                                else
                                {
                                    // We can't win, get rid of the smallest card
                                    return findLowestCard(handH, handC, handD, handS);
                                }
                            }
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
                            */
                        }
                    }
                }
                return null;
            }
        }
        // We are the leader
        else
        {
            return null;
        }
    }
            /*
            // if we are the last to go
            if(table.length == 2)
            {
                
            }
            // we are in the middle
            else
            {
            }
            Card lowestCard = hand[0];
            Card lowestCardTrump = null;
            Card highestCardTrump = null;
            Card lowestCardSuit = null;
            Card highestCardSuit = null;
        
            
            
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
                // if we re looking for a card, go through hand, if we can beat what's on the table that's highest if not check for lowest
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
            return highestCard;*/


}
