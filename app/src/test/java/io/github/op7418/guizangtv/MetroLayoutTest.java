package io.github.op7418.guizangtv;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class MetroLayoutTest {
    @Test public void threeBigAndTwoSmallFillOneStrip(){
        List<MetroLayout.Cell> cells=MetroLayout.place(new boolean[]{true,true,true,false,false});
        assertEquals(5,cells.size());
        assertEquals(0,cells.get(0).left);assertEquals(0,cells.get(0).top);
        assertEquals(234,cells.get(0).width);assertEquals(234,cells.get(0).height);
        assertEquals(246,cells.get(1).left);
        assertEquals(492,cells.get(2).left);
        assertEquals(738,cells.get(3).left);assertEquals(0,cells.get(3).top);
        assertEquals(738,cells.get(4).left);assertEquals(123,cells.get(4).top);
        assertEquals(849,MetroLayout.width(cells));
        assertEquals(234,MetroLayout.height(cells));
    }
    @Test public void smallTilesStackAndBigTilesNeverOverlap(){
        List<MetroLayout.Cell> cells=MetroLayout.place(new boolean[]{false,false,true,true});
        assertEquals(0,cells.get(0).left);assertEquals(0,cells.get(0).top);
        assertEquals(0,cells.get(1).left);assertEquals(123,cells.get(1).top);
        assertEquals(123,cells.get(2).left);assertEquals(234,cells.get(2).width);
        assertEquals(369,cells.get(3).left);
        assertEquals(603,MetroLayout.width(cells));
        for(int i=0;i<cells.size();i++)for(int j=i+1;j<cells.size();j++)assertFalse(overlap(cells.get(i),cells.get(j)));
    }
    private static boolean overlap(MetroLayout.Cell a,MetroLayout.Cell b){
        return a.left<b.left+b.width&&b.left<a.left+a.width&&a.top<b.top+b.height&&b.top<a.top+a.height;
    }
}
