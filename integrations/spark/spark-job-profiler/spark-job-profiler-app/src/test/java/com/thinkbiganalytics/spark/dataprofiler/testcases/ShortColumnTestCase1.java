package com.thinkbiganalytics.spark.dataprofiler.testcases;

import com.thinkbiganalytics.spark.dataprofiler.columns.ColumnStatistics;
import com.thinkbiganalytics.spark.dataprofiler.columns.ShortColumnStatistics;
import com.thinkbiganalytics.spark.dataprofiler.core.ProfilerTest;
import com.thinkbiganalytics.spark.dataprofiler.topn.TopNDataItem;
import com.thinkbiganalytics.spark.dataprofiler.topn.TopNDataList;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.TreeSet;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


/**
 * Short Column Statistics Test Case
 * @author jagrut sharma
 *
 */
public class ShortColumnTestCase1 extends ProfilerTest {
	private static ColumnStatistics columnStats;
	private static long nullCount;
	private static long totalCount;
	private static long uniqueCount;
	private static double percNullValues;
	private static double percUniqueValues;
	private static double percDuplicateValues;
	private static TopNDataList topNValues;
	private static short max;
	private static short min;
	private static long sum;
	private static double mean;
	private static double stddev;
	private static double variance;

    @Before
    public void setUp() {
        super.setUp();

        columnStats = columnStatsMap.get(11);	//credits
        nullCount = 2L;
        totalCount = 10L;
        uniqueCount = 6L;
        percNullValues = 20.0d;
        percUniqueValues = 60.0d;
        percDuplicateValues = 40.0d;
        topNValues = columnStats.getTopNValues();
        max = (short)5000;
        min = (short)0;
        sum = 8600L;
        mean = 1075d;
        stddev = 1579.359047d;
        variance = 2494375d;
        
	}
	
	@Test
    public void testShortNullCount() {
    	assertEquals(nullCount, columnStats.getNullCount());
    }
    
    
    @Test
    public void testShortTotalCount() {
		assertEquals(totalCount, columnStats.getTotalCount());
    }
    
    
    @Test
    public void testShortUniqueCount() {
		assertEquals(uniqueCount, columnStats.getUniqueCount());
    }
    
    
    @Test
    public void testShortPercNullValues() {
		assertEquals(percNullValues, columnStats.getPercNullValues(), ProfilerTest.epsilon);
    }
    
    
    @Test
    public void testShortPercUniqueValues() {
		assertEquals(percUniqueValues, columnStats.getPercUniqueValues(), ProfilerTest.epsilon);
    }
    
    
    @Test
    public void testShortPercDuplicateValues() {
		assertEquals(percDuplicateValues, columnStats.getPercDuplicateValues(), ProfilerTest.epsilon);
    }
    
    @Test
    public void testShortTopNValues() {
        TreeSet<TopNDataItem> items = topNValues.getTopNDataItemsForColumn();
        Iterator<TopNDataItem> iterator = items.descendingIterator();

        //Verify that there are 3 items
        assertEquals(3, items.size());

        //Verify the top 3 item counts
        int index = 1;
        while (iterator.hasNext()) {
            TopNDataItem item = iterator.next();
            if (index == 1) {
                assertEquals((short) 100, item.getValue());
                assertEquals(Long.valueOf(3L), item.getCount());
            }
            else if (index == 2) {
                // tie for count 2
                assertThat(String.valueOf(item.getValue()),
                        anyOf(is(String.valueOf((short) 1400)), is("null")));
                assertEquals(Long.valueOf(2L), item.getCount());
            }
            else if (index == 3) {
                // tie for count 2
                assertThat(String.valueOf(item.getValue()),
                        anyOf(is(String.valueOf((short) 1400)), is("null")));
                assertEquals(Long.valueOf(2L), item.getCount());
            }

            index++;
        }
    }
    
    
    @Test
    public void testShortMax() {
    	assertEquals(max, ((ShortColumnStatistics)columnStats).getMax());
    }
    
    
    @Test
    public void testShortMin() {
    	assertEquals(min, ((ShortColumnStatistics)columnStats).getMin());
    }
    
    
    @Test
    public void testShortSum() {
    	assertEquals(sum, ((ShortColumnStatistics)columnStats).getSum());
    }
    
    
    @Test
    public void testShortMean() {
    	assertEquals(mean, ((ShortColumnStatistics)columnStats).getMean(), ProfilerTest.epsilon);
    }
    
    
    @Test
    public void testShortStddev() {
    	assertEquals(stddev, ((ShortColumnStatistics)columnStats).getStddev(), ProfilerTest.epsilon);
    }
    
    
    @Test
    public void testShortVariance() {
    	assertEquals(variance, ((ShortColumnStatistics)columnStats).getVariance(), ProfilerTest.epsilon);
    }
    
    
    @AfterClass
    public static void tearDownClass() {
    	System.out.println("\t*** Completed run for ShortColumnTestCase1 ***");
    }
}   