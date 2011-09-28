package com.clearspring.thetan.metricCatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.map.util.LRUMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.yammer.metrics.core.CounterMetric;
import com.yammer.metrics.core.HistogramMetric;
import com.yammer.metrics.core.MeterMetric;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.reporting.AbstractReporter;

public class MetricCatcherTest {
    MetricCatcher metricCatcher;
    JSONMetric jsonMetric;
    String metricName;
        
    @Before
    public void setUp() throws Exception {
        AbstractReporter reporter = mock(AbstractReporter.class);
		Map<String, Metric> metricCache = new LRUMap<String, Metric>(10, 10);
        metricCatcher = new MetricCatcher(new DatagramSocket(), reporter, metricCache);
        
        jsonMetric = new JSONMetric();
        jsonMetric.setType("meter");
        // The Metrics class caches created metrics; we want fresh ones
        metricName = "foo.bar.baz.metric" + Math.random();
        jsonMetric.setName(metricName);
        jsonMetric.setTimestamp(((int)System.currentTimeMillis() / 1000));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCreateMetric() {
        Metric metric = metricCatcher.createMetric(jsonMetric);

        assertEquals(MeterMetric.class, metric.getClass());
        
        MeterMetric meterMetric = ((MeterMetric)metric);
        // All metrics are in minutes :-( plz2fix
        assertEquals(TimeUnit.MINUTES, meterMetric.rateUnit());
    }

    @Test
    public void testUpdateMetric() {
        MeterMetric metric = (MeterMetric)metricCatcher.createMetric(jsonMetric);
        
        metricCatcher.updateMetric(metric, 1);
        assertEquals(1, metric.count());
    }

    @Test
    public void testUpdateMetric_MultipleUpdates() {
        MeterMetric metric = (MeterMetric)metricCatcher.createMetric(jsonMetric);
        
        int count = 7;
        for (int x = 0; x < 7; x++)
	        metricCatcher.updateMetric(metric, 1);
        
        assertEquals(count, metric.count());
    }

    @Test
    public void testUpdateMetric_Meter_MarkWithZeroHasNoEffect() {
        MeterMetric metric = (MeterMetric)metricCatcher.createMetric(jsonMetric);
        metricCatcher.updateMetric(metric, 0);
        assertEquals(0, metric.count());
    }

    @Test
    public void testUpdateMetric_Counter_Increment() {
        jsonMetric.setType("counter");
        CounterMetric metric = (CounterMetric)metricCatcher.createMetric(jsonMetric);
        
        metricCatcher.updateMetric(metric, 7);
        assertEquals(7, metric.count());
    }
    
    @Test
    public void testUpdateMetric_Counter_IncrementMultipleTimes() {
        jsonMetric.setType("counter");
        CounterMetric metric = (CounterMetric)metricCatcher.createMetric(jsonMetric);
        
        int count = 7;
        for (int x = 0; x < 7; x++)
	        metricCatcher.updateMetric(metric, 1);
        
        assertEquals(count, metric.count());
    }

    @Test
    public void testUpdateMetric_Counter_Decrement() {
        jsonMetric.setType("counter");
        CounterMetric metric = (CounterMetric)metricCatcher.createMetric(jsonMetric);
        
        metricCatcher.updateMetric(metric, -7);
        assertEquals(-7, metric.count());
    }
    
    @Test
    public void testUpdateMetric_Counter_Clear() {
        jsonMetric.setType("counter");
        CounterMetric metric = (CounterMetric)metricCatcher.createMetric(jsonMetric);
        
        metricCatcher.updateMetric(metric, 1);
        assertEquals(1, metric.count());
        
        metricCatcher.updateMetric(metric, 0);
        assertEquals(0, metric.count());
    }
    
    @Test
    public void testUpdateMetric_Histogram() {
        jsonMetric.setType("histogram");
        jsonMetric.setBiased(false);
        HistogramMetric metric = (HistogramMetric)metricCatcher.createMetric(jsonMetric);
        
        metricCatcher.updateMetric(metric, 1);
        assertEquals(1, metric.count());
    }
    
    @Test
    public void testRun() throws IOException, InterruptedException {
        // Force feed a metric into the catcher
        AbstractReporter reporter = mock(AbstractReporter.class);
		Map<String, Metric> metricCache = new LRUMap<String, Metric>(10, 10);
		DatagramSocket listeningSocket = new DatagramSocket();
        metricCatcher = new MetricCatcher(listeningSocket, reporter, metricCache);
		
		String json = "{\"unique\":\"b21407b0dbd00d9b87e68206c600d0bc1316647776\"," +
		              "\"metrics\":[" +
                      "{\"name\":\"test\",\"value\":1,\"type\":\"counter\",\"timestamp\":1316647781}]}";
		byte[] jsonBytes = json.getBytes();
		DatagramSocket sendingSocket = new DatagramSocket();
		sendingSocket.send(new DatagramPacket(jsonBytes, jsonBytes.length, listeningSocket.getLocalAddress(), listeningSocket.getLocalPort()));
		
		metricCatcher.start();
		Thread.sleep(1000);
		metricCatcher.shutdown();
		
		assertTrue(metricCache.containsKey("test"));
    }

}
