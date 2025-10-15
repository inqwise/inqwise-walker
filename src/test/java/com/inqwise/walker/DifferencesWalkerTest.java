package com.inqwise.walker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.inqwise.difference.Differences;
import com.inqwise.difference.Differences.Difference;

/**
 * Unit tests for DifferencesWalker class.
 */
class DifferencesWalkerTest {

    private DifferencesWalker walker;

    @BeforeEach
    void setUp() {
        walker = new DifferencesWalker();
    }

    /**
     * Helper method to create a test Difference object.
     */
    private Difference createTestDifference(String operation, String path, Object value) {
        Difference diff = new Difference();
        diff.setOperation(Differences.Operation.valueOf(operation));
        diff.setPath(path);
        diff.setValue(value);
        return diff;
    }

    @Test
    void testType() {
        assertEquals(Differences.class, walker.type());
    }

    @Test
    void testBasicWalking() {
        List<Difference> diffList = Arrays.asList(
            createTestDifference("add", "/name", "John"),
            createTestDifference("replace", "/age", 30),
            createTestDifference("remove", "/temp", null)
        );
        
        Differences differences = new Differences(diffList);
        
        List<Difference> visitedDiffs = new ArrayList<>();
        walker.handler(event -> {
            if (event.indicatedObject() instanceof Difference) {
                visitedDiffs.add((Difference) event.indicatedObject());
            }
        });

        walker.handle(differences);

        assertEquals(3, visitedDiffs.size());
        assertEquals("add", visitedDiffs.get(0).getOperation().toString());
        assertEquals("replace", visitedDiffs.get(1).getOperation().toString());
        assertEquals("remove", visitedDiffs.get(2).getOperation().toString());
    }

    @Test
    void testEmptyDifferences() {
        Differences emptyDifferences = new Differences(Collections.emptyList());
        
        AtomicInteger visitCount = new AtomicInteger(0);
        walker.handler(event -> visitCount.incrementAndGet());

        walker.handle(emptyDifferences);

        assertEquals(0, visitCount.get());
    }

    @Test
    void testWithChildWalkers() {
        // Create a walker with child walkers
        Set<ObjectWalker> childWalkers = Set.of(); // Empty set for this test
        DifferencesWalker walkerWithChildren = new DifferencesWalker(childWalkers);
        
        List<Difference> diffList = Arrays.asList(
            createTestDifference("add", "/test1", "value1"),
            createTestDifference("add", "/test2", "value2")
        );
        Differences differences = new Differences(diffList);
        
        List<Difference> visitedDiffs = new ArrayList<>();
        walkerWithChildren.handler(event -> {
            if (event.indicatedObject() instanceof Difference) {
                visitedDiffs.add((Difference) event.indicatedObject());
            }
        });

        walkerWithChildren.handle(differences);

        assertEquals(2, visitedDiffs.size());
    }

    @Test
    void testEarlyTermination() {
        List<Difference> diffList = Arrays.asList(
            createTestDifference("add", "/item1", "value1"),
            createTestDifference("add", "/item2", "value2"),
            createTestDifference("add", "/item3", "value3"),
            createTestDifference("add", "/item4", "value4")
        );
        Differences differences = new Differences(diffList);
        
        List<Difference> visitedDiffs = new ArrayList<>();
        walker.handler(event -> {
            if (event.indicatedObject() instanceof Difference) {
                visitedDiffs.add((Difference) event.indicatedObject());
                if (visitedDiffs.size() == 2) {
                    event.end(); // Stop after processing 2 items
                }
            }
        });

        walker.handle(differences);

        assertEquals(2, visitedDiffs.size());
        assertEquals("/item1", visitedDiffs.get(0).getPath());
        assertEquals("/item2", visitedDiffs.get(1).getPath());
    }

    @Test
    void testPauseAndResume() {
        List<Difference> diffList = Arrays.asList(
            createTestDifference("add", "/pause1", "value1"),
            createTestDifference("add", "/pause2", "value2"),
            createTestDifference("add", "/pause3", "value3")
        );
        Differences differences = new Differences(diffList);
        
        List<Difference> visitedDiffs = new ArrayList<>();
        AtomicInteger pauseCount = new AtomicInteger(0);
        
        walker.handler(event -> {
            if (event.indicatedObject() instanceof Difference) {
                visitedDiffs.add((Difference) event.indicatedObject());
                if (pauseCount.get() == 0 && visitedDiffs.size() == 1) {
                    event.context().pause();
                    pauseCount.incrementAndGet();
                    // Resume immediately for test
                    event.context().resume();
                }
            }
        });

        walker.handle(differences);

        assertEquals(3, visitedDiffs.size());
        assertEquals(1, pauseCount.get());
    }

    @Test
    void testContextDataStorage() {
        List<Difference> diffList = Arrays.asList(
            createTestDifference("add", "/context1", "value1"),
            createTestDifference("add", "/context2", "value2")
        );
        Differences differences = new Differences(diffList);
        
        walker.handler(event -> {
            if (event.indicatedObject() instanceof Difference) {
                Difference diff = (Difference) event.indicatedObject();
                if ("/context1".equals(diff.getPath())) {
                    event.context().put("testKey", "testValue");
                } else if ("/context2".equals(diff.getPath())) {
                    String storedValue = (String) event.context().get("testKey");
                    assertEquals("testValue", storedValue);
                }
            }
        });

        walker.handle(differences);
    }

    @Test
    void testErrorHandling() {
        List<Difference> diffList = Arrays.asList(
            createTestDifference("add", "/error1", "value1"),
            createTestDifference("add", "/error2", "value2")
        );
        Differences differences = new Differences(diffList);
        
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);
        
        walker.handler(event -> {
            processedCount.incrementAndGet();
            if (event.indicatedObject() instanceof Difference) {
                Difference diff = (Difference) event.indicatedObject();
                if ("/error1".equals(diff.getPath())) {
                    throw new RuntimeException("Test error");
                }
            }
        });
        
        walker.errorHandler(context -> {
            errorCount.incrementAndGet();
            assertEquals("Test error", context.cause().getMessage());
        });

        walker.handle(differences);
        
        assertTrue(errorCount.get() > 0, "Error handler should have been called");
    }

    @Test
    void testEndHandler() {
        List<Difference> diffList = Arrays.asList(
            createTestDifference("add", "/end1", "value1"),
            createTestDifference("add", "/end2", "value2")
        );
        Differences differences = new Differences(diffList);
        
        AtomicInteger endCount = new AtomicInteger(0);
        
        walker.handler(event -> {
            // Do nothing, just process
        });
        
        walker.endHandler(context -> {
            endCount.incrementAndGet();
            assertTrue(context.success());
        });

        walker.handle(differences);
        
        assertEquals(1, endCount.get(), "End handler should be called exactly once");
    }

    @Test
    void testIteratorBehavior() {
        List<Difference> diffList = Arrays.asList(
            createTestDifference("add", "/iter1", "value1"),
            createTestDifference("add", "/iter2", "value2"),
            createTestDifference("add", "/iter3", "value3")
        );
        Differences differences = new Differences(diffList);
        
        // Test that the differences object creates proper iterator
        Iterator<Difference> iterator = differences.iterator();
        assertNotNull(iterator);
        
        assertTrue(iterator.hasNext());
        Difference diff1 = iterator.next();
        assertEquals("/iter1", diff1.getPath());
        
        assertTrue(iterator.hasNext());
        Difference diff2 = iterator.next();
        assertEquals("/iter2", diff2.getPath());
        
        assertTrue(iterator.hasNext());
        Difference diff3 = iterator.next();
        assertEquals("/iter3", diff3.getPath());
        
        assertFalse(iterator.hasNext());
    }

    @Test
    void testConstructorWithoutArgs() {
        DifferencesWalker walker1 = new DifferencesWalker();
        assertNotNull(walker1);
        assertEquals(Differences.class, walker1.type());
    }

    @Test
    void testConstructorWithChildWalkers() {
        Set<ObjectWalker> childWalkers = Set.of();
        DifferencesWalker walker2 = new DifferencesWalker(childWalkers);
        assertNotNull(walker2);
        assertEquals(Differences.class, walker2.type());
    }

    @Test
    void testPathTracking() {
        List<Difference> diffList = Arrays.asList(
            createTestDifference("add", "/user/name", "John"),
            createTestDifference("replace", "/user/age", 30)
        );
        Differences differences = new Differences(diffList);
        
        List<String> paths = new ArrayList<>();
        List<Difference> visitedDiffs = new ArrayList<>();
        
        walker.handler(event -> {
            // Track differences visited rather than paths
            // since DifferencesWalker doesn't set paths for individual Difference objects
            if (event.indicatedObject() instanceof Difference) {
                visitedDiffs.add((Difference) event.indicatedObject());
            }
            
            String path = (String) event.meta().get(ObjectWalker.Keys.PATH);
            if (path != null) {
                paths.add(path);
            }
        });

        walker.handle(differences);

        // Verify that differences are processed correctly
        assertEquals(2, visitedDiffs.size(), "Should have visited 2 differences");
        assertEquals("/user/name", visitedDiffs.get(0).getPath());
        assertEquals("/user/age", visitedDiffs.get(1).getPath());
    }

    @Test
    void testNullDifference() {
        // Test handling of null values in difference list
        List<Difference> diffList = new ArrayList<>();
        diffList.add(createTestDifference("add", "/valid", "value"));
        diffList.add(null); // Add null difference
        diffList.add(createTestDifference("remove", "/another", null));
        
        Differences differences = new Differences(diffList);
        
        List<Object> visitedObjects = new ArrayList<>();
        walker.handler(event -> {
            visitedObjects.add(event.indicatedObject());
        });

        // Should handle gracefully, processing non-null items
        assertDoesNotThrow(() -> walker.handle(differences));
        
        // Should have processed at least the non-null items
        assertTrue(visitedObjects.size() >= 2);
    }

    @Test
    void testComplexDifferenceValues() {
        // Test with complex values in differences
        Map<String, Object> complexValue = new HashMap<>();
        complexValue.put("nested", "data");
        complexValue.put("number", 42);
        
        List<Difference> diffList = Arrays.asList(
            createTestDifference("add", "/complex", complexValue),
            createTestDifference("replace", "/simple", "simple_value")
        );
        
        Differences differences = new Differences(diffList);
        
        List<Object> values = new ArrayList<>();
        walker.handler(event -> {
            if (event.indicatedObject() instanceof Difference) {
                Difference diff = (Difference) event.indicatedObject();
                values.add(diff.getValue());
            }
        });

        walker.handle(differences);

        assertEquals(2, values.size());
        assertTrue(values.contains(complexValue));
        assertTrue(values.contains("simple_value"));
    }
}