package com.inqwise.walker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonArray;

/**
 * Simple test to isolate JsonArrayWalker issue
 */
class SimpleJsonArrayTest {

    @Test
    void testSimpleArrayWalking() {
        // Given
        JsonArray jsonArray = new JsonArray()
            .add("item1")
            .add("item2")
            .add("item3");
        
        JsonArrayWalker walker = new JsonArrayWalker();
        AtomicInteger eventCount = new AtomicInteger(0);
        
        // When - simplified handler without assertions
        walker.handler(event -> {
            System.out.println("Event " + eventCount.incrementAndGet() + ": " + event.indicatedObject());
        });
        
        ObjectWalkingContext context = walker.handle(jsonArray);
        
        // Then
        System.out.println("Total events: " + eventCount.get());
        assertEquals(3, eventCount.get());
        assertTrue(context.success());
    }
}
