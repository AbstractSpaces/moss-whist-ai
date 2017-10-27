/**
 * @author  Martin Porebski, 21498791
 *          Dylan Johnson
 * @date    30/09/2017
 */
package mossai;

import java.util.*;
import java.io.*;

public class Run
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        String friendlyAI = "Clever Girl";
        int friendlyWins = 0;
        int games = 20;
        for(int i = 0; i < games; i ++)
        {
            MossSideWhist game = new MossSideWhist(new RandomAgent(), new Raptor(), new RandomAgent());
            game.playGame(1, System.out);
            
            friendlyWins += areYouWinning(friendlyAI, game.scoreboard) ? 1 : -1;
        }
        
        System.out.println(friendlyAI + " won " + friendlyWins + " out of " + games + " games.");
    }
    
    public static boolean areYouWinning(String player, Map<String, Integer> scoreboard)
    {
        String winningPlayer = scoreboard.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
        return winningPlayer.equals(player);
    }
    
}
