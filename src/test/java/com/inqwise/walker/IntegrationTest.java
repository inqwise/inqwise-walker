package com.inqwise.walker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Integration tests for the complete walking functionality.
 */
class IntegrationTest {

    private JsonObjectWalker walker;
    private AtomicInteger eventCount;
    private AtomicInteger endHandlerCount;
    private AtomicInteger errorHandlerCount;

    @BeforeEach
    void setUp() {
        Set<ObjectWalker> childWalkers = Sets.newHashSet(
            new JsonObjectWalker(),
            new JsonArrayWalker()
        );
        walker = new JsonObjectWalker(childWalkers);
        eventCount = new AtomicInteger(0);
        endHandlerCount = new AtomicInteger(0);
        errorHandlerCount = new AtomicInteger(0);
    }

    @Test
    void testComplexJsonStructure() {
        // Given
        JsonObject complexJson = createComplexJsonStructure();
        
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
        
        ObjectWalkingContext context = walker.handle(complexJson);
        
        // Then
        assertTrue(eventCount.get() == 11); // Total number of leaf values (objects with walkers are not counted)
        assertEquals(1, endHandlerCount.get());
        assertTrue(context.success());
    }

    @Test
    void testPathTracking() {
        // Given
        JsonObject json = createComplexJsonStructure();
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            
            // Verify specific paths
            switch (eventCount.get()) {
                case 1:
                    assertEquals(".name", path);
                    break;
                case 2:
                    assertEquals(".age", path);
                    break;
                case 3:
                    assertEquals(".address.street", path);
                    break;
                case 4:
                    assertEquals(".address.city", path);
                    break;
                case 5:
                    assertEquals(".hobbies[0]", path);
                    break;
                case 6:
                    assertEquals(".hobbies[1]", path);
                    break;
                case 7:
                    assertEquals(".hobbies[2]", path);
                    break;
                case 8:
                    assertEquals(".phones[0].type", path);
                    break;
                case 9:
                    assertEquals(".phones[0].number", path);
                    break;
                case 10:
                    assertEquals(".phones[1].type", path);
                    break;
                case 11:
                    assertEquals(".phones[1].number", path);
                    break;
            }
        });
        
        ObjectWalkingContext context = walker.handle(json);
        
        // Then
        assertTrue(eventCount.get() == 11);
        assertTrue(context.success());
    }

    @Test
    void testFlowControl() {
        // Given
        JsonObject json = createComplexJsonStructure();
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            
            // End walking after processing the address
            if (path.equals(".address.city")) {
                event.end();
            }
        });
        
        ObjectWalkingContext context = walker.handle(json);
        
        // Then
        assertEquals(4, eventCount.get()); // name, age, address.street, address.city (address container doesn't fire event)
        assertTrue(context.success());
    }

    @Test
    void testPauseAndResume() {
        // Given
        JsonObject json = createComplexJsonStructure();
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            
            // Pause after processing the first hobby
            if (path.equals(".hobbies[0]")) {
                event.context().pause();
                assertTrue(event.context().paused());
                event.context().resume();
            }
        });
        
        ObjectWalkingContext context = walker.handle(json);
        
        // Then
        assertEquals(11, eventCount.get());
        assertTrue(context.success());
        assertFalse(context.paused());
    }

    @Test
    void testErrorHandling() {
        // Given
        JsonObject json = createComplexJsonStructure();
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            
            // Throw error when processing the second hobby
            if (path.equals(".hobbies[1]")) {
                throw new RuntimeException("Test error during walking");
            }
        });
        
        walker.errorHandler(context -> {
            errorHandlerCount.incrementAndGet();
            assertTrue(context.failed());
            assertNotNull(context.cause());
            assertEquals("Test error during walking", context.cause().getMessage());
        });
        
        ObjectWalkingContext context = walker.handle(json);
        
        // Then
        assertEquals(6, eventCount.get()); // Up to hobbies[1]
        assertEquals(1, errorHandlerCount.get());
        assertTrue(context.failed());
    }

    @Test
    void testContextDataSharing() {
        // Given
        JsonObject json = createComplexJsonStructure();
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            
            // Store data in context
            if (path.equals(".name")) {
                event.context().put("userName", event.indicatedObject());
            } else if (path.equals(".age")) {
                event.context().put("userAge", event.indicatedObject());
            }
        });
        
        ObjectWalkingContext context = walker.handle(json);
        
        // Then
        assertTrue(eventCount.get() == 11);
        assertEquals("John Doe", context.get("userName"));
        assertEquals(30, (int)context.get("userAge"));
        assertTrue(context.success());
    }

    @Test
    void testWalkerDelegation() {
        // Given
        JsonObject json = createComplexJsonStructure();
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            
            // Verify that appropriate walkers are used
            if (event.hasWalker()) {
                Object value = event.indicatedObject();
                if (value instanceof JsonObject) {
                    assertTrue(event.getWalker() instanceof JsonObjectWalker);
                } else if (value instanceof JsonArray) {
                    assertTrue(event.getWalker() instanceof JsonArrayWalker);
                }
            }
        });
        
        ObjectWalkingContext context = walker.handle(json);
        
        // Then
        assertEquals(11, eventCount.get());
        assertTrue(context.success());
    }

    @Test
    void testEmptyAndNullHandling() {
        // Given
        JsonObject json = new JsonObject()
            .put("emptyObject", new JsonObject())
            .put("emptyArray", new JsonArray())
            .put("nullValue", null)
            .put("stringValue", "test");
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            assertNotNull(event.meta().get(ObjectWalker.Keys.PATH));
        });
        
        ObjectWalkingContext context = walker.handle(json);
        
        // Then
        assertEquals(2, eventCount.get()); // nullValue, stringValue (empty containers don't fire events)
        assertTrue(context.success());
    }

    @Test
    void testDeepNesting() {
        // Given
        JsonObject deepJson = createDeepNestedJson();
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            assertTrue(path.split("\\.").length <= 6); // Max depth of 6 parts (including root)
        });
        
        ObjectWalkingContext context = walker.handle(deepJson);
        
        // Then
        assertEquals(1, eventCount.get()); // Only the leaf value fires an event
        assertTrue(context.success());
    }

    @Test
    void testLargeArrayHandling() {
        // Given
        JsonArray largeArray = new JsonArray();
        for (int i = 0; i < 100; i++) {
            largeArray.add("item" + i);
        }
        
        JsonObject json = new JsonObject().put("largeArray", largeArray);
        
        // When
        walker.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            assertTrue(path.startsWith(".largeArray["));
        });
        
        ObjectWalkingContext context = walker.handle(json);
        
        // Then
        assertTrue(eventCount.get() == 100); // 100 items (array container doesn't fire an event)
        assertTrue(context.success());
    }

    // Helper methods
    private JsonObject createComplexJsonStructure() {
        JsonObject address = new JsonObject()
            .put("street", "123 Main St")
            .put("city", "New York");
        
        JsonArray hobbies = new JsonArray()
            .add("reading")
            .add("swimming")
            .add("coding");
        
        JsonArray phones = new JsonArray()
            .add(new JsonObject()
                .put("type", "mobile")
                .put("number", "555-1234"))
            .add(new JsonObject()
                .put("type", "home")
                .put("number", "555-5678"));
        
        return new JsonObject()
            .put("name", "John Doe")
            .put("age", 30)
            .put("address", address)
            .put("hobbies", hobbies)
            .put("phones", phones);
    }

    private JsonObject createDeepNestedJson() {
        return new JsonObject()
            .put("level1", new JsonObject()
                .put("level2", new JsonObject()
                    .put("level3", new JsonObject()
                        .put("level4", new JsonObject()
                            .put("level5", "deepValue")))));
    }
} 