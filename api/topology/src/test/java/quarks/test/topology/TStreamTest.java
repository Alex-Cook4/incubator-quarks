/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package quarks.test.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;
import org.junit.Test;

import quarks.topology.TSink;
import quarks.topology.TStream;
import quarks.topology.TWindow;
import quarks.topology.Topology;
import quarks.topology.tester.Condition;

@Ignore
public abstract class TStreamTest extends TopologyAbstractTest {

    @Test
    public void testFilter() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("a", "b", "c");
        s = s.filter(tuple -> "b".equals(tuple));
        assertStream(t, s);

        Condition<Long> tc = t.getTester().tupleCount(s, 1);
        Condition<List<String>> contents = t.getTester().streamContents(s, "b");
        complete(t, tc);

        assertTrue(contents.valid());
    }

    /**
     * Test Peek. This will only work with an embedded setup.
     * 
     * @throws Exception
     */
    @Test
    public void testPeek() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("a", "b", "c");
        List<String> peekedValues = new ArrayList<>();
		TStream<String> speek = s.peek(tuple -> peekedValues.add(tuple));
		assertSame(s, speek);

		Condition<Long> tc = t.getTester().tupleCount(s, 3);
		Condition<List<String>> contents = t.getTester().streamContents(s, "a", "b", "c");
        complete(t, tc);

        assertTrue(contents.valid());
        assertEquals(contents.getResult(), peekedValues);
    }

	@Test
	public void testMultiplePeek() throws Exception {

		Topology t = newTopology();

		TStream<String> s = t.strings("a", "b", "c");
		List<String> peekedValues = new ArrayList<>();
		TStream<String> speek = s.peek(tuple -> peekedValues.add(tuple + "1st"));
		assertSame(s, speek);

		TStream<String> speek2 = s.peek(tuple -> peekedValues.add(tuple + "2nd"));
		assertSame(s, speek2);
		TStream<String> speek3 = s.peek(tuple -> peekedValues.add(tuple + "3rd"));
		assertSame(s, speek3);

		Condition<Long> tc = t.getTester().tupleCount(s, 3);
		Condition<List<String>> contents = t.getTester().streamContents(s, "a", "b", "c");
		complete(t, tc);

		assertTrue(contents.valid());
        List<String> expected = Arrays.asList("a1st", "a2nd", "a3rd", "b1st", "b2nd", "b3rd", "c1st", "c2nd", "c3rd");
		assertEquals(expected, peekedValues);
	}

    @Test
    public void testMap() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("32", "423", "-746");
        TStream<Integer> i = s.map(Integer::valueOf);
        assertStream(t, i);

        Condition<Long> tc = t.getTester().tupleCount(i, 3);
        Condition<List<Integer>> contents = t.getTester().streamContents(i, 32, 423, -746);
        complete(t, tc);

        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    @Test
    public void testModifyWithDrops() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("32", "423", "-746");
        TStream<Integer> i = s.map(Integer::valueOf);
        i = i.modify(tuple -> tuple < 0 ? null : tuple + 27);
        assertStream(t, i);

        Condition<Long> tc = t.getTester().tupleCount(i, 2);
        Condition<List<Integer>> contents = t.getTester().streamContents(i, 59, 450);
        complete(t, tc);

        assertTrue(contents.getResult().toString(), contents.valid());
    }

    @Test
    public void testModify() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("a", "b", "c");
        TStream<String> i = s.modify(tuple -> tuple.concat("M"));
        assertStream(t, i);

        Condition<Long> tc = t.getTester().tupleCount(i, 3);
        Condition<List<String>> contents = t.getTester().streamContents(i, "aM", "bM", "cM");
        complete(t, tc);

        assertTrue(contents.valid());
    }
    
    @Test
    public void tesFlattMap() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("mary had a little lamb",
                "its fleece was white as snow");
        TStream<String> w = s.flatMap(tuple->Arrays.asList(tuple.split(" ")));
        assertStream(t, w);

        Condition<List<String>> contents = t.getTester().streamContents(w, "mary", "had",
                "a", "little", "lamb", "its", "fleece", "was", "white", "as",
                "snow");
        complete(t, contents);

        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    @Test
    public void tesFlattMapWithNullIterator() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("mary had a little lamb", "NOTUPLES",
                "its fleece was white as snow");
        TStream<String> w = s.flatMap(tuple->tuple.equals("NOTUPLES") ? null : Arrays.asList(tuple.split(" ")));
        assertStream(t, w);

        Condition<List<String>> contents = t.getTester().streamContents(w, "mary", "had",
                "a", "little", "lamb", "its", "fleece", "was", "white", "as",
                "snow");
        complete(t, contents);

        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    @Test
    public void tesFlattMapWithNullValues() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("mary had a little lamb",
                "its fleece was white as snow");
        TStream<String> w = s.flatMap(tuple-> {List<String> values = Arrays.asList(tuple.split(" "));
          values.set(2, null); values.set(4, null); return values;});
        assertStream(t, w);

        Condition<List<String>> contents = t.getTester().streamContents(w, "mary", "had",
                "little", "its", "fleece",  "white",
                "snow");
        complete(t, contents);

        assertTrue(contents.getResult().toString(), contents.valid());
    }

    /**
     * Test split() with no drops.
     */
    @Test
    public void testSplit() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("a1", "b1", "a2", "c1", "e1", "c2", "c3", "b2", "a3", "b3", "d1", "e2");
        List<TStream<String>> splits = s.split(3, tuple -> tuple.charAt(0) - 'a');

        Condition<Long> tc0 = t.getTester().tupleCount(splits.get(0), 4);
        Condition<Long> tc1 = t.getTester().tupleCount(splits.get(1), 5);
        Condition<Long> tc2 = t.getTester().tupleCount(splits.get(2), 3);
        Condition<List<String>> contents0 = t.getTester().streamContents(splits.get(0), "a1", "a2", "a3", "d1");
        Condition<List<String>> contents1 = t.getTester().streamContents(splits.get(1), "b1", "e1", "b2", "b3", "e2");
        Condition<List<String>> contents2 = t.getTester().streamContents(splits.get(2), "c1", "c2", "c3");

        complete(t, t.getTester().and(tc0, tc1, tc2));

        assertTrue(contents0.toString(), contents0.valid());
        assertTrue(contents1.toString(), contents1.valid());
        assertTrue(contents2.toString(), contents2.valid());
    }

    /**
     * Test split() with drops.
     */
    @Test
    public void testSplitWithDrops() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("a1", "b1", "a2", "c1", "e1", "c2", "c3", "b2", "a3", "b3", "d1", "e2");
        List<TStream<String>> splits = s.split(3, tuple -> {
            switch (tuple.charAt(0)) {
            case 'a':
                return 1;
            case 'b':
                return 4;
            case 'c':
                return 8;
            case 'd':
                return -34;
            case 'e':
                return -1;
            default:
                return -1;
            }
        });

        Condition<Long> tc0 = t.getTester().tupleCount(splits.get(0), 0);
        Condition<Long> tc1 = t.getTester().tupleCount(splits.get(1), 6);
        Condition<Long> tc2 = t.getTester().tupleCount(splits.get(2), 3);
        Condition<List<String>> contents0 = t.getTester().streamContents(splits.get(0));
        Condition<List<String>> contents1 = t.getTester().streamContents(splits.get(1), "a1", "b1", "a2", "b2", "a3",
                "b3");
        Condition<List<String>> contents2 = t.getTester().streamContents(splits.get(2), "c1", "c2", "c3");

        complete(t, t.getTester().and(tc0, tc1, tc2));

        assertTrue(contents0.toString(), contents0.valid());
        assertTrue(contents1.toString(), contents1.valid());
        assertTrue(contents2.toString(), contents2.valid());
    }

    /**
     * Test split() zero outputs
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSplitWithZeroOutputs() throws Exception {
        newTopology().strings("a1").split(0, tuple -> 0);
    }

    /**
     * Test split() negative outputs
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSplitWithNegativeOutputs() throws Exception {
        newTopology().strings("a1").split(-28, tuple -> 0);
    }

    /**
     * Test enum data structure
     */
    private enum LogSeverityEnum {
        ALERT(1), CRITICAL(2), ERROR(3), WARNING(4), NOTICE(5), INFO(6), DEBUG(7);

        @SuppressWarnings("unused")
        private final int code;

        LogSeverityEnum(final int code) {
            this.code = code;
        }
    }

    /**
     * Test split(enum) with integer type enum.
     */
    @Test
    public void testSplitWithEnum() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("Log1_ALERT", "Log2_INFO", "Log3_INFO", "Log4_INFO", "Log5_ERROR", "Log6_ERROR", "Log7_CRITICAL");
        TStream<String> i = s.map(String::toString);
        EnumMap<LogSeverityEnum,TStream<String>> splits = i.split(LogSeverityEnum.class, e -> LogSeverityEnum.valueOf(e.split("_")[1]));

        assertStream(t, i);

        Condition<Long> tc0 = t.getTester().tupleCount(splits.get(LogSeverityEnum.ALERT), 1);
        Condition<Long> tc1 = t.getTester().tupleCount(splits.get(LogSeverityEnum.INFO), 3);
        Condition<Long> tc2 = t.getTester().tupleCount(splits.get(LogSeverityEnum.ERROR), 2);
        Condition<Long> tc3 = t.getTester().tupleCount(splits.get(LogSeverityEnum.CRITICAL), 1);
        Condition<Long> tc4 = t.getTester().tupleCount(splits.get(LogSeverityEnum.WARNING), 0);

        Condition<List<String>> contents0 = t.getTester().streamContents(splits.get(LogSeverityEnum.ALERT), "Log1_ALERT");
        Condition<List<String>> contents1 = t.getTester().streamContents(splits.get(LogSeverityEnum.INFO), "Log2_INFO",
            "Log3_INFO", "Log4_INFO");
        Condition<List<String>> contents2 = t.getTester().streamContents(splits.get(LogSeverityEnum.ERROR), "Log5_ERROR",
            "Log6_ERROR");
        Condition<List<String>> contents3 = t.getTester().streamContents(splits.get(LogSeverityEnum.CRITICAL), "Log7_CRITICAL");
        Condition<List<String>> contents4 = t.getTester().streamContents(splits.get(LogSeverityEnum.WARNING));

        complete(t, t.getTester().and(tc0, tc1, tc2, tc3, tc4));


        assertTrue(contents0.toString(), contents0.valid());
        assertTrue(contents1.toString(), contents1.valid());
        assertTrue(contents2.toString(), contents2.valid());
        assertTrue(contents3.toString(), contents3.valid());
        assertTrue(contents4.toString(), contents4.valid());
    }

    private enum EnumClassWithZerosize {
        ;
    }

    /**
     * Test split(enum) with integer type enum.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSplitWithEnumForZeroSizeClass() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("Test");
        s.split(EnumClassWithZerosize.class, e -> EnumClassWithZerosize.valueOf("Test"));
    }

    @Test
    public void testFanout2() throws Exception {

        Topology t = newTopology();
        
        TStream<String> s = t.strings("a", "b", "c");
        TStream<String> sf = s.filter(tuple -> "b".equals(tuple));
        TStream<String> sm = s.modify(tuple -> tuple.concat("fo2"));

        Condition<Long> tsmc = t.getTester().tupleCount(sm, 3);
        Condition<List<String>> tsf = t.getTester().streamContents(sf, "b");
        Condition<List<String>> tsm = t.getTester().streamContents(sm, "afo2", "bfo2", "cfo2");

        complete(t, t.getTester().and(tsm, tsmc));

        assertTrue(tsf.getResult().toString(), tsf.valid());
        assertTrue(tsm.getResult().toString(), tsm.valid());
    }
    @Test
    public void testFanout3() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("a", "b", "cde");
        TStream<String> sf = s.filter(tuple -> "b".equals(tuple));
        TStream<String> sm = s.modify(tuple -> tuple.concat("fo2"));
        TStream<byte[]> st = s.map(tuple -> tuple.getBytes());

        Condition<Long> tsfc = t.getTester().tupleCount(sf, 1);
        Condition<Long> tsmc = t.getTester().tupleCount(sm, 3);
        Condition<Long> tstc = t.getTester().tupleCount(st, 3);
        Condition<List<String>> tsf = t.getTester().streamContents(sf, "b");
        Condition<List<String>> tsm = t.getTester().streamContents(sm, "afo2", "bfo2", "cdefo2");
        Condition<List<byte[]>> tst = t.getTester().streamContents(st, "a".getBytes(), "b".getBytes(), "cde".getBytes());

        complete(t, t.getTester().and(tsfc, tsmc, tstc));

        assertTrue(tsf.valid());
        assertTrue(tsm.valid());

        // Can't use equals on byte[]
        assertEquals(3, tst.getResult().size());
        assertEquals("a", new String(tst.getResult().get(0)));
        assertEquals("b", new String(tst.getResult().get(1)));
        assertEquals("cde", new String(tst.getResult().get(2)));
    }

    @Test
    public void testPeekThenFanout() throws Exception {
        _testFanoutWithPeek(false);
    }

    @Test
    public void testFanoutThenPeek() throws Exception {
        _testFanoutWithPeek(true);
    }

    void _testFanoutWithPeek(boolean after) throws Exception {

        Topology t = newTopology();

        List<Peeked> values = new ArrayList<>();
        values.add(new Peeked(33));
        values.add(new Peeked(-214));
        values.add(new Peeked(9234));
        for (Peeked p : values)
            assertFalse(p.peeked);

        TStream<Peeked> s = t.collection(values);
        if (!after)
            s.peek(tuple -> tuple.peeked = true);

        TStream<Peeked> sf = s.filter(tuple -> tuple.value > 0);
        TStream<Peeked> sm = s.modify(tuple -> new Peeked(tuple.value + 37, tuple.peeked));

        if (after)
            s.peek(tuple -> tuple.peeked = true);

        Condition<Long> tsfc = t.getTester().tupleCount(sf, 2);
        Condition<Long> tsmc = t.getTester().tupleCount(sm, 3);
        Condition<List<Peeked>> tsf = t.getTester().streamContents(sf, new Peeked(33, true), new Peeked(9234, true));
        Condition<List<Peeked>> tsm = t.getTester().streamContents(sm, new Peeked(70, true), new Peeked(-177, true),
                new Peeked(9271, true));

        complete(t, t.getTester().and(tsfc, tsmc));

        assertTrue(tsf.getResult().toString(), tsf.valid());
        assertTrue(tsm.getResult().toString(), tsm.valid());
    }

    public static class Peeked implements Serializable {
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (peeked ? 1231 : 1237);
            result = prime * result + value;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Peeked other = (Peeked) obj;
            if (peeked != other.peeked)
                return false;
            if (value != other.value)
                return false;
            return true;
        }

        private static final long serialVersionUID = 1L;
        final int value;
        boolean peeked;

        Peeked(int value) {
            this.value = value;
        }

        Peeked(int value, boolean peeked) {
            this.value = value;
            this.peeked = true;
        }
    }
    
    /**
     * Test Union with itself.
     * 
     * @throws Exception
     */
    @Test
    public void testUnionWithSelf() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("a", "b", "c");

        assertSame(s, s.union(s));
        assertSame(s, s.union(Collections.emptySet()));
        assertSame(s, s.union(Collections.singleton(s)));
    }
    
    @Test
    public void testUnion2() throws Exception {

        Topology t = newTopology();

        TStream<String> s1 = t.strings("a", "b", "c");
        TStream<String> s2 = t.strings("d", "e");
        TStream<String> su = s1.union(s2);
        assertNotSame(s1, su);
        assertNotSame(s2, su);
        TStream<String> r = su.modify(v -> v.concat("X"));

        Condition<Long> tc = t.getTester().tupleCount(r, 5);
        Condition<List<String>> contents = t.getTester().contentsUnordered(r,
                "aX", "bX", "cX", "dX", "eX");
        complete(t, tc);

        assertTrue(tc.getResult().toString(), tc.valid());
        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    @Test
    public void testUnion4() throws Exception {

        Topology t = newTopology();

        TStream<String> s1 = t.strings("a", "b", "c");
        TStream<String> s2 = t.strings("d", "e");
        TStream<String> s3 = t.strings("f", "g", "h", "i");
        TStream<String> s4 = t.strings("j");
        TStream<String> su = s1.union(new HashSet<>(Arrays.asList(s2, s3, s4)));
        assertNotSame(s1, su);
        assertNotSame(s2, su);
        assertNotSame(s3, su);
        assertNotSame(s4, su);
        TStream<String> r = su.modify(v -> v.concat("Y"));

        Condition<Long> tc = t.getTester().tupleCount(r, 10);
        Condition<List<String>> contents = t.getTester().contentsUnordered(r,
                "aY", "bY", "cY", "dY", "eY", "fY", "gY", "hY", "iY", "jY");
        complete(t, tc);

        assertTrue(tc.getResult().toString(), tc.valid());
        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    @Test
    public void testUnion4WithSelf() throws Exception {

        Topology t = newTopology();

        TStream<String> s1 = t.strings("a", "b", "c");
        TStream<String> s2 = t.strings("d", "e");
        TStream<String> s3 = t.strings("f", "g", "h", "i");
        TStream<String> s4 = t.strings("j");
        TStream<String> su = s1.union(new HashSet<>(Arrays.asList(s1, s2, s3, s4)));
        assertNotSame(s1, su);
        assertNotSame(s2, su);
        assertNotSame(s3, su);
        assertNotSame(s4, su);
        TStream<String> r = su.modify(v -> v.concat("Y"));

        Condition<Long> tc = t.getTester().tupleCount(r, 10);
        Condition<List<String>> contents = t.getTester().contentsUnordered(r,
                "aY", "bY", "cY", "dY", "eY", "fY", "gY", "hY", "iY", "jY");
        complete(t, tc);

        assertTrue(tc.getResult().toString(), tc.valid());
        assertTrue(contents.getResult().toString(), contents.valid());
    }
    
    @Test
    public void testSink() throws Exception {

        Topology t = newTopology();

        TStream<String> s = t.strings("a", "b", "c");
        
        List<String> sinked = new ArrayList<>();
        TSink<String> terminal = s.sink(tuple -> sinked.add(tuple));
        assertNotNull(terminal);
        assertSame(t, terminal.topology());
        assertSame(s, terminal.getFeed());
        TStream<String> s1 = s.filter(tuple -> true);

        Condition<Long> tc = t.getTester().tupleCount(s1, 3);
        complete(t, tc);
        
        assertEquals("a", sinked.get(0));
        assertEquals("b", sinked.get(1));
        assertEquals("c", sinked.get(2));
    }
    
    /**
     * Submit multiple jobs concurrently using ProcessSource.
     */
    @Test
    public void testMultiTopology() throws Exception {

        int executions = 4;
        ExecutorCompletionService<Boolean> completer = new ExecutorCompletionService<>(
                Executors.newFixedThreadPool(executions));
        for (int i = 0; i < executions; i++) {
            completer.submit(() -> {
                Topology t = newTopology();
                TStream<String> s = t.strings("a", "b", "c", "d", "e", "f", "g", "h");
                s.sink((tuple) -> { if ("h".equals(tuple)) System.out.println(tuple);});
                Condition<Long> tc = t.getTester().tupleCount(s, 8);
                complete(t, tc);
                return true;
            });
        }
        waitForCompletion(completer, executions);
    }

    /**
     * Submit multiple jobs concurrently using ProcessSource.
     */
    @Test
    public void testMultiTopologyWithError() throws Exception {

        int executions = 4;
        ExecutorCompletionService<Boolean> completer = new ExecutorCompletionService<>(
                Executors.newFixedThreadPool(executions));
        for (int i = 0; i < executions; i++) {
            completer.submit(() -> {
                Topology t = newTopology();
                TStream<String> s = t.strings("a", "b", "c", "d", "e", "f", "g", "h");
                // Throw on the 8th tuple
                s.sink((tuple) -> { if ("h".equals(tuple)) throw new RuntimeException("Expected Test Exception");});
                // Expect 7 tuples out of 8
                Condition<Long> tc = t.getTester().tupleCount(s, 7);
                complete(t, tc);
                return true;
            });
        }
        waitForCompletion(completer, executions);
    }
    
    /**
     * Submit multiple jobs concurrently using PeriodicSource.
     */
    @Test
    public void testMultiTopologyPollWithError() throws Exception {

        int executions = 4;
        ExecutorCompletionService<Boolean> completer = new ExecutorCompletionService<>(
                Executors.newFixedThreadPool(executions));
        for (int i = 0; i < executions; i++) {
            completer.submit(() -> {
                Topology t = newTopology();
                AtomicLong n = new AtomicLong(0);
                TStream<Long> s = t.poll(() -> n.incrementAndGet(), 10, TimeUnit.MILLISECONDS);
                // Throw on the 8th tuple
                s.sink((tuple) -> { if (8 == n.get()) throw new RuntimeException("Expected Test Exception");});
                // Expect 7 tuples out of 8
                Condition<Long> tc = t.getTester().tupleCount(s, 7);
                complete(t, tc);
                return true;
            });
        }
        waitForCompletion(completer, executions);
    }
    
    @Test
    public void testJoinWithWindow() throws Exception{
        Topology t = newTopology();
        
        List<Integer> ints = new ArrayList<>();
        List<Integer> lookupInts = new ArrayList<>();
        
        // Ints to populate the window
        for(int i = 0; i < 100; i++){
            ints.add(i);
        }
        
        // Ints to lookup partitions in window
        for(int i = 0; i < 10; i++){
            lookupInts.add(i);
        }
        TStream<Integer> intStream = t.collection(ints);
        
        // Wait until the window is populated, and then submit tuples
        TStream<Integer> lookupIntStream = t.source(() -> {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return lookupInts;
        });
        
        TWindow<Integer, Integer> window = intStream.last(10, tuple -> tuple % 10);
        TStream<Integer> joinsHappened = lookupIntStream.join(tuple -> tuple % 10, window, (number, partitionContents) -> {
            assertTrue(partitionContents.size() == 10);
            for(Integer element : partitionContents)
                assertTrue(number % 10 == element % 10);
            
            // Causes an error if two numbers map to the same partition, which shouldn't happen
            partitionContents.clear();
            return 0;
        });
    
        Condition<Long> tc = t.getTester().tupleCount(joinsHappened, 10);
        complete(t, tc);      
    }
    
    @Test
    public void testJoinLastWithKeyer() throws Exception{
        Topology t = newTopology();
        
        List<Integer> ints = new ArrayList<>();
        for(int i = 0; i < 100; i++){
            ints.add(i);
        }
        
        TStream<Integer> intStream = t.collection(ints);
        
        // Wait until the window is populated, and then submit tuples
        TStream<Integer> lookupIntStream = t.source(() -> {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ints;
        });
        
        TStream<String> joinsHappened = lookupIntStream.joinLast(tuple -> tuple, intStream, tuple -> tuple, (a, b) -> {
            assertTrue(a.equals(b));
            return "0";
        });

        Condition<Long> tc = t.getTester().tupleCount(joinsHappened, 100);
        complete(t, tc);      
    }

    private void waitForCompletion(ExecutorCompletionService<Boolean> completer, int numtasks) throws ExecutionException {
        int remainingTasks = numtasks;
        while (remainingTasks > 0) {
            try {
                Future<Boolean> completed = completer.poll(4, TimeUnit.SECONDS);
                if (completed == null) {
                    System.err.println("Completer timed out");
                    throw new RuntimeException(new TimeoutException());
                }
                else {
                    completed.get();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            remainingTasks--;
        }
    }
}
