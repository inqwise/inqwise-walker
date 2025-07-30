package com.inqwise.walker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

import io.vertx.core.json.JsonObject;

/**
 * Unit tests for JsonObjectWalker functionality.
 */
class JsonObjectWalkerTest {

    private JsonObjectWalker walker;
    private AtomicInteger eventCount;
    private AtomicInteger endHandlerCount;
    private AtomicInteger errorHandlerCount;

    @BeforeEach
    void setUp() {
        walker = new JsonObjectWalker();
        eventCount = new AtomicInteger(0);
        endHandlerCount = new AtomicInteger(0);
        errorHandlerCount = new AtomicInteger(0);
    }

    @Test
    void testBasicJsonObjectWalking() {
        // Given
        JsonObject jsonObject = new JsonObject()
            .put("name", "John")
            .put("age", 30)
            .put("city", "New York");
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            assertNotNull(event.indicatedObject());
            assertNotNull(event.meta().get(ObjectWalker.Keys.PATH));
        });
        
        walker.endHandler(context -> {
            endHandlerCount.incrementAndGet();
            assertTrue(context.success());
        });
        
        ObjectWalkingContext context = walker.handle(jsonObject);
        
        // Then
        assertEquals(3, eventCount.get()); // name, age, city - these are the leaf values processed
        assertEquals(1, endHandlerCount.get());
        assertTrue(context.success());
    }

    @Test
    void testJsonObjectWithNestedObjects() {
        // Given
        JsonObject nestedObject = new JsonObject().put("street", "Main St");
        JsonObject jsonObject = new JsonObject()
            .put("name", "John")
            .put("address", nestedObject);
        
        Set<ObjectWalker> childWalkers = Sets.newHashSet(
            new JsonObjectWalker(),
            new JsonArrayWalker()
        );
        JsonObjectWalker walkerWithChildren = new JsonObjectWalker(childWalkers);
        
        // When
        walkerWithChildren.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            assertTrue(path.equals(".name") || path.equals(".address") || path.startsWith(".address."));
        });
        
        ObjectWalkingContext context = walkerWithChildren.handle(jsonObject);
        
        // Then
        assertEquals(2, eventCount.get()); // name, address.street (nested object container doesn't fire event)
        assertTrue(context.success());
    }

    @Test
    void testJsonObjectWithArrays() {
        // Given
        JsonObject jsonObject = new JsonObject()
            .put("name", "John")
            .put("hobbies", new io.vertx.core.json.JsonArray().add("reading").add("swimming"));
        
        Set<ObjectWalker> childWalkers = Sets.newHashSet(
            new JsonObjectWalker(),
            new JsonArrayWalker()
        );
        JsonObjectWalker walkerWithChildren = new JsonObjectWalker(childWalkers);
        
        // When
        walkerWithChildren.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            assertTrue(path.equals(".name") || path.equals(".hobbies") || path.startsWith(".hobbies["));
        });
        
        ObjectWalkingContext context = walkerWithChildren.handle(jsonObject);
        
        // Then
        assertEquals(3, eventCount.get()); // name, hobbies[0], hobbies[1] (array container doesn't fire event)
        assertTrue(context.success());
    }

    @Test
    void testPathConstruction() {
        // Given
        JsonObject jsonObject = new JsonObject()
            .put("level1", new JsonObject()
                .put("level2", new JsonObject()
                    .put("level3", "value")));
        
        Set<ObjectWalker> childWalkers = Sets.newHashSet(new JsonObjectWalker());
        JsonObjectWalker walkerWithChildren = new JsonObjectWalker(childWalkers);
        
        // When
        walkerWithChildren.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            
            // Only the leaf value should fire an event, not the containers
            assertEquals(".level1.level2.level3", path);
        });
        
        ObjectWalkingContext context = walkerWithChildren.handle(jsonObject);
        
        // Then
        assertEquals(1, eventCount.get()); // Only the leaf value fires event
        assertTrue(context.success());
    }

    @Test
    void testEmptyJsonObject() {
        // Given
        JsonObject emptyObject = new JsonObject();
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
        });
        
        ObjectWalkingContext context = walker.handle(emptyObject);
        
        // Then
        assertEquals(0, eventCount.get());
        assertTrue(context.success());
    }

    @Test
    void testJsonObjectWithNullValues() {
        // Given
        JsonObject jsonObject = new JsonObject()
            .put("name", "John")
            .put("middleName", null)
            .put("age", 30);
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            assertNotNull(event.meta().get(ObjectWalker.Keys.PATH));
        });
        
        ObjectWalkingContext context = walker.handle(jsonObject);
        
        // Then
        assertEquals(3, eventCount.get()); // name, middleName, age
        assertTrue(context.success());
    }

    @Test
    void testJsonObjectWithComplexTypes() {
        // Given
        JsonObject jsonObject = new JsonObject()
            .put("string", "value")
            .put("number", 42)
            .put("boolean", true)
            .put("null", null);
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            Object value = event.indicatedObject();
            assertTrue(value instanceof String || value instanceof Number || 
                      value instanceof Boolean || value == null);
        });
        
        ObjectWalkingContext context = walker.handle(jsonObject);
        
        // Then
        assertEquals(4, eventCount.get());
        assertTrue(context.success());
    }

    @Test
    void testJsonObjectWalkerInstance() {
        // Given
        JsonObject jsonObject = new JsonObject()
            .put("name", "John")
            .put("hobbies", new io.vertx.core.json.JsonArray().add("reading"));
        
        JsonObjectWalker instance = JsonObjectWalker.instance();
        
        // When
        instance.handler(event -> {
            eventCount.incrementAndGet();
        });
        
        ObjectWalkingContext context = instance.handle(jsonObject);
        
        // Then
        assertEquals(2, eventCount.get()); // name, hobbies[0] (array container doesn't fire event)
        assertTrue(context.success());
        assertEquals(2, instance.walkers().size()); // JsonObjectWalker and JsonArrayWalker
    }

    @Test
    void testErrorHandling() {
        // Given
        JsonObject jsonObject = new JsonObject().put("name", "John");
        
        // When
        ObjectWalkingContext context = TestUtils.testErrorHandling(walker, jsonObject, eventCount, errorHandlerCount, 1);
        
        // Then
        assertEquals(1, eventCount.get());
        assertEquals(1, errorHandlerCount.get());
        assertTrue(context.failed());
    }

    @Test
    void testEndWalkingEarly() {
        // Given
        JsonObject jsonObject = new JsonObject()
            .put("name", "John")
            .put("age", 30)
            .put("city", "New York");
        
        // When
        ObjectWalkingContext context = TestUtils.testEndWalkingEarly(walker, jsonObject, eventCount, 2);
        
        // Then
        assertEquals(2, eventCount.get());
        assertTrue(context.success());
    }

    @Test
    void testPauseAndResume() {
        // Given
        JsonObject jsonObject = new JsonObject()
            .put("name", "John")
            .put("age", 30);
        
        // When
        ObjectWalkingContext context = TestUtils.testPauseAndResume(walker, jsonObject, eventCount, 1);
        
        // Then
        assertEquals(2, eventCount.get());
        assertTrue(context.success());
        assertFalse(context.paused());
    }

    @Test
    void testContextDataStorage() {
        // Given
        JsonObject jsonObject = new JsonObject().put("name", "John");
        
        // When
        ObjectWalkingContext context = TestUtils.testContextDataStorage(walker, jsonObject, eventCount);
        
        // Then
        assertEquals(1, eventCount.get());
        assertEquals("testValue", context.get("testKey"));
    }

    @Test
    void testTypeMethod() {
        // When & Then
        TestUtils.testWalkerType(walker, JsonObject.class);
    }
} 