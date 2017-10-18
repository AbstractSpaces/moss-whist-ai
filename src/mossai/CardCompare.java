package mossai;
import java.util.*;

/**
 * Class for sorting cards based on rank.
 * @author Dylan Johnson
 */
public class CardCompare implements Comparator<Card>
{
    @Override
    public int compare(Card a, Card b)
    {
        if(a.rank < b.rank) return -1;
        else if(a.rank > b.rank) return 1;
        else return 0;
    }
}
