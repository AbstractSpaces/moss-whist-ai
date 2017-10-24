/**
 * @author  Martin Porebski, 21498791
 *          Dylan Johnson
 * @date    30/09/2017
 */
package mossai;


public class Run
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        MossSideWhist game = new MossSideWhist(new RandomAgent(), new Raptor(), new RandomAgent());
        game.playGame(1, System.out);
    }
    
}
