package com.inqwise.walker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

import io.vertx.core.json.JsonArray;

/**
 * Unit tests for JsonArrayWalker functionality.
 */
class JsonArrayWalkerTest {

    private JsonArrayWalker walker;
    private AtomicInteger eventCount;
    private AtomicInteger endHandlerCount;
    private AtomicInteger errorHandlerCount;

    @BeforeEach
    void setUp() {
        walker = new JsonArrayWalker();
        eventCount = new AtomicInteger(0);
        endHandlerCount = new AtomicInteger(0);
        errorHandlerCount = new AtomicInteger(0);
    }

    @Test
    void testBasicJsonArrayWalking() {
        // Given
        JsonArray jsonArray = new JsonArray()
            .add("item1")
            .add("item2")
            .add("item3");
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            assertNotNull(event.indicatedObject());
            assertNotNull(event.meta().get(ObjectWalker.Keys.PATH));
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            assertTrue(path.contains("[") && path.contains("]"));
        });
        
        walker.endHandler(context -> {
            endHandlerCount.incrementAndGet();
            assertTrue(context.success());
        });
        
        ObjectWalkingContext context = walker.handle(jsonArray);
        
        // Then
        assertEquals(3, eventCount.get());
        assertEquals(1, endHandlerCount.get());
        assertTrue(context.success());
    }

    @Test
    void testJsonArrayWithNestedObjects() {
        // Given
        JsonArray jsonArray = new JsonArray()
            .add("item1")
            .add(new io.vertx.core.json.JsonObject().put("name", "John"))
            .add("item3");
        
        Set<ObjectWalker> childWalkers = Sets.newHashSet(
            new JsonObjectWalker(),
            new JsonArrayWalker()
        );
        JsonArrayWalker walkerWithChildren = new JsonArrayWalker(childWalkers);
        
        // When
        walkerWithChildren.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            assertTrue(path.contains("[") && path.contains("]"));
        });
        
        ObjectWalkingContext context = walkerWithChildren.handle(jsonArray);
        
        // Then
        assertEquals(3, eventCount.get()); // item1, object.name, item3 (object container doesn't fire event)
        assertTrue(context.success());
    }

    @Test
    void testJsonArrayWithNestedArrays() {
        // Given
        JsonArray nestedArray = new JsonArray().add("nested1").add("nested2");
        JsonArray jsonArray = new JsonArray()
            .add("item1")
            .add(nestedArray)
            .add("item3");
        
        Set<ObjectWalker> childWalkers = Sets.newHashSet(
            new JsonObjectWalker(),
            new JsonArrayWalker()
        );
        JsonArrayWalker walkerWithChildren = new JsonArrayWalker(childWalkers);
        
        // When
        walkerWithChildren.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            assertTrue(path.contains("[") && path.contains("]"));
        });
        
        ObjectWalkingContext context = walkerWithChildren.handle(jsonArray);
        
        // Then
        assertEquals(4, eventCount.get()); // item1, nestedArray[0], nestedArray[1], item3 (nested array container doesn't fire event)
        assertTrue(context.success());
    }

    @Test
    void testPathConstruction() {
        // Given
        JsonArray jsonArray = new JsonArray()
            .add("first")
            .add("second")
            .add("third");
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            
            switch (eventCount.get()) {
                case 1:
                    assertEquals(".[0]", path);
                    break;
                case 2:
                    assertEquals(".[1]", path);
                    break;
                case 3:
                    assertEquals(".[2]", path);
                    break;
            }
        });
        
        ObjectWalkingContext context = walker.handle(jsonArray);
        
        // Then
        assertEquals(3, eventCount.get());
        assertTrue(context.success());
    }

    @Test
    void testEmptyJsonArray() {
        // Given
        JsonArray emptyArray = new JsonArray();
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
        });
        
        ObjectWalkingContext context = walker.handle(emptyArray);
        
        // Then
        assertEquals(0, eventCount.get());
        assertTrue(context.success());
    }

    @Test
    void testJsonArrayWithNullValues() {
        // Given
        JsonArray jsonArray = new JsonArray()
            .add("item1")
            .add(null)
            .add("item3");
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            assertNotNull(event.meta().get(ObjectWalker.Keys.PATH));
        });
        
        ObjectWalkingContext context = walker.handle(jsonArray);
        
        // Then
        assertEquals(3, eventCount.get());
        assertTrue(context.success());
    }

    @Test
    void testJsonArrayWithComplexTypes() {
        // Given
        JsonArray jsonArray = new JsonArray()
            .add("string")
            .add(42)
            .add(true)
            .add(null);
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            Object value = event.indicatedObject();
            assertTrue(value instanceof String || value instanceof Number || 
                      value instanceof Boolean || value == null);
        });
        
        ObjectWalkingContext context = walker.handle(jsonArray);
        
        // Then
        assertEquals(4, eventCount.get()); // All 4 items fire events as they have no dedicated walkers
        assertTrue(context.success());
    }

    @Test
    void testNestedArrayPathConstruction() {
        // Given
        JsonArray nestedArray = new JsonArray().add("nested1").add("nested2");
        JsonArray jsonArray = new JsonArray().add(nestedArray);
        
        Set<ObjectWalker> childWalkers = Sets.newHashSet(new JsonArrayWalker());
        JsonArrayWalker walkerWithChildren = new JsonArrayWalker(childWalkers);
        
        // When
        walkerWithChildren.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            
            switch (eventCount.get()) {
                case 1:
                    assertEquals(".[0][0]", path);
                    break;
                case 2:
                    assertEquals(".[0][1]", path);
                    break;
            }
        });
        
        ObjectWalkingContext context = walkerWithChildren.handle(jsonArray);
        
        // Then
        assertEquals(2, eventCount.get()); // container doesn't fire, only nested items
        assertTrue(context.success());
    }

    @Test
    void testErrorHandling() {
        // Given
        JsonArray jsonArray = new JsonArray().add("item1").add("item2");
        
        // When
        ObjectWalkingContext context = TestUtils.testErrorHandling(walker, jsonArray, eventCount, errorHandlerCount, 1);
        
        // Then
        assertEquals(1, eventCount.get());
        assertEquals(1, errorHandlerCount.get());
        assertTrue(context.failed());
    }

    @Test
    void testEndWalkingEarly() {
        // Given
        JsonArray jsonArray = new JsonArray()
            .add("item1")
            .add("item2")
            .add("item3");
        
        // When
        ObjectWalkingContext context = TestUtils.testEndWalkingEarly(walker, jsonArray, eventCount, 2);
        
        // Then
        assertEquals(2, eventCount.get());
        assertTrue(context.success());
    }

    @Test
    void testPauseAndResume() {
        // Given
        JsonArray jsonArray = new JsonArray()
            .add("item1")
            .add("item2");
        
        // When
        ObjectWalkingContext context = TestUtils.testPauseAndResume(walker, jsonArray, eventCount, 1);
        
        // Then
        assertEquals(2, eventCount.get());
        assertTrue(context.success());
        assertFalse(context.paused());
    }

    @Test
    void testContextDataStorage() {
        // Given
        JsonArray jsonArray = new JsonArray().add("item1");
        
        // When
        ObjectWalkingContext context = TestUtils.testContextDataStorage(walker, jsonArray, eventCount);
        
        // Then
        assertEquals(1, eventCount.get());
        assertEquals("testValue", context.get("testKey"));
    }

    @Test
    void testTypeMethod() {
        // When & Then
        TestUtils.testWalkerType(walker, JsonArray.class);
    }

    @Test
    void testArrayWithMixedTypes() {
        // Given
        JsonArray jsonArray = new JsonArray()
            .add("string")
            .add(123)
            .add(new io.vertx.core.json.JsonObject().put("key", "value"))
            .add(new JsonArray().add("nested"));
        
        Set<ObjectWalker> childWalkers = Sets.newHashSet(
            new JsonObjectWalker(),
            new JsonArrayWalker()
        );
        JsonArrayWalker walkerWithChildren = new JsonArrayWalker(childWalkers);
        
        // When
        walkerWithChildren.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            assertTrue(path.contains("[") && path.contains("]"));
        });
        
        ObjectWalkingContext context = walkerWithChildren.handle(jsonArray);
        
        // Then
        assertEquals(4, eventCount.get()); // string, 123, object.key, array[0] (containers don't fire events)
        assertTrue(context.success());
    }
} 