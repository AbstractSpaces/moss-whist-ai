/**
 *
 * @author Martin
 */
package mossai;

public class GreedyAi
{
    public Card greedyTurn(Card[] table,Card[] hand, Card[] handH, Card[] handC, Card[] handD, Card[] handS)
    {
        // if there are cards on the table (played in this tick)
        if(table.length > 0)
        {
            Card[] selectedSuit = handH;
            int selectedSuitSize = selectedSuit.length;
            // we have to obey the suit played by the leader
            Suit tickSuit = table[0].suit;
                    
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
            
            // If we are player 3
            if(table.length == 2)
            {
                // If we have to obey the suit
                if(selectedSuitSize > 0)
                {
                    Card highestCardPlayed = table[0];
                    
                    // If they are the same suit, select the one with highest rank
                    if(table[0].suit == table[1].suit)
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
                    else
                    {
                        // Otherwise player 2 could have played a trump
                        if(table[1].suit == Suit.SPADES)
                        {
                            highestCardPlayed = table[1];
                        }
                        else
                        {
                            // Otherwise player 1 played trumps and player 2 didn't play any
                            highestCardPlayed = table[0];
                        }
                    }
                    
                    // If we have to play, but a spade has been played outside of it's suit
                    if(tickSuit != Suit.SPADES && highestCardPlayed.suit == Suit.SPADES)
                    {
                        // We can't win, play the lowest card
                        return selectedSuit[0];
                    }
                    else
                    {
                        // Go through each one of the cards of that suit that we have 
                        for(int i = 0; i < selectedSuitSize; i++)
                        {
                            // If we have a card that's better than the highest card played
                            if(selectedSuit[i].rank > highestCardPlayed.rank)
                            {
                                // Play the higher card (doesn't have to be the highest
                                return selectedSuit[i];
                            }
                        }
                        // If we can't beat what's on the table, play the lowest card
                        return selectedSuit[0];
                    }
                }
                // We don't have the suit
                else
                {
                    // If spades were played but we don't have any
                    if(tickSuit == Suit.SPADES)
                    {
                        Card lowestCard = null;
                        if(handH.length > 0)
                        {
                            lowestCard = handH[0];
                        }
                    }
                }
            }
            
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
            return highestCard;
        }
    }  
}
