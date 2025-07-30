package com.inqwise.walker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.vertx.core.json.JsonObject;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Utility class for test operations with common test patterns.
 */
public class TestUtils {
    
    /**
     * Reads a JSON file from the test resources directory.
     * 
     * @param resourcePath The path to the JSON file relative to test resources
     * @return JsonObject containing the parsed JSON data
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public static JsonObject readJson(String resourcePath) {
        try {
            String content = new String(Files.readAllBytes(
                Paths.get("src/test/resources/" + resourcePath)));
            return new JsonObject(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + resourcePath, e);
        }
    }

    /**
     * Common error handling test pattern.
     * 
     * @param walker The walker to test
     * @param testData The data to walk
     * @param eventCount Counter for tracking events
     * @param errorHandlerCount Counter for tracking error handler invocations
     * @param expectedEventCount Expected number of events before error
     * @return The walking context
     */
    public static ObjectWalkingContext testErrorHandling(ObjectWalker walker, Object testData, 
                                                        AtomicInteger eventCount, AtomicInteger errorHandlerCount,
                                                        int expectedEventCount) {
        walker.handler(event -> {
            eventCount.incrementAndGet();
            throw new RuntimeException("Test error");
        });
        
        walker.errorHandler(context -> {
            errorHandlerCount.incrementAndGet();
            assertTrue(context.failed());
            assertNotNull(context.cause());
        });
        
        return walker.handle(testData);
    }

    /**
     * Common pause and resume test pattern.
     * 
     * @param walker The walker to test
     * @param testData The data to walk
     * @param eventCount Counter for tracking events
     * @param pauseAtEventCount Event count at which to pause
     * @return The walking context
     */
    public static ObjectWalkingContext testPauseAndResume(ObjectWalker walker, Object testData,
                                                         AtomicInteger eventCount, int pauseAtEventCount) {
        walker.handler(event -> {
            eventCount.incrementAndGet();
            if (eventCount.get() == pauseAtEventCount) {
                event.context().pause();
                assertTrue(event.context().paused());
                event.context().resume();
            }
        });
        
        return walker.handle(testData);
    }

    /**
     * Common end walking early test pattern.
     * 
     * @param walker The walker to test
     * @param testData The data to walk
     * @param eventCount Counter for tracking events
     * @param endAtEventCount Event count at which to end
     * @return The walking context
     */
    public static ObjectWalkingContext testEndWalkingEarly(ObjectWalker walker, Object testData,
                                                          AtomicInteger eventCount, int endAtEventCount) {
        walker.handler(event -> {
            eventCount.incrementAndGet();
            if (eventCount.get() == endAtEventCount) {
                event.end();
            }
        });
        
        return walker.handle(testData);
    }

    /**
     * Common context data storage test pattern.
     * 
     * @param walker The walker to test
     * @param testData The data to walk
     * @param eventCount Counter for tracking events
     * @return The walking context
     */
    public static ObjectWalkingContext testContextDataStorage(ObjectWalker walker, Object testData,
                                                             AtomicInteger eventCount) {
        walker.handler(event -> {
            eventCount.incrementAndGet();
            event.context().put("testKey", "testValue");
            assertEquals("testValue", event.context().get("testKey"));
        });
        
        ObjectWalkingContext context = walker.handle(testData);
        context.put("testKey", "testValue"); // Store data directly in context
        
        return context;
    }

    /**
     * Common walker type test pattern.
     * 
     * @param walker The walker to test
     * @param expectedType The expected type class
     */
    public static void testWalkerType(ObjectWalker walker, Class<?> expectedType) {
        // Use reflection to access the protected type() method
        try {
            java.lang.reflect.Method typeMethod = walker.getClass().getDeclaredMethod("type");
            typeMethod.setAccessible(true);
            Class<?> actualType = (Class<?>) typeMethod.invoke(walker);
            assertEquals(expectedType, actualType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to test walker type", e);
        }
    }

    /**
     * Common path construction test pattern.
     * 
     * @param walker The walker to test
     * @param testData The data to walk
     * @param eventCount Counter for tracking events
     * @param pathValidator Consumer to validate paths at each event
     * @return The walking context
     */
    public static ObjectWalkingContext testPathConstruction(ObjectWalker walker, Object testData,
                                                           AtomicInteger eventCount, Consumer<String> pathValidator) {
        walker.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            pathValidator.accept(path);
        });
        
        return walker.handle(testData);
    }
}
