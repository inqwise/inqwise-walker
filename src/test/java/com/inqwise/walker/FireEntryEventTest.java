package com.inqwise.walker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class FireEntryEventTest {

    @Test
    void testFireEntryEvent() {
        // Given
        var fireEntryWalker = new JsonObjectWalker(Set.of(
            new JsonObjectWalker() {
                @Override
                protected boolean fireEntryEvent() {
                    return true;
                }
            },
            new JsonArrayWalker() {
                @Override
                protected boolean fireEntryEvent() {
                    return true;
                }
            }
        ));

        var noFireEntryWalker = new JsonObjectWalker(Set.of(
            new JsonObjectWalker(),
            new JsonArrayWalker()
        ));

        var complexJson = createComplexJsonStructure();
        var eventCount = new AtomicInteger(0);

        // When
        fireEntryWalker.handler(event -> {
            eventCount.incrementAndGet();
        }).handle(complexJson);

        // Then - should fire events for all leaf nodes AND container nodes
        // Leaf nodes: name(1), age(1), address.street(1), address.city(1), hobbies[0](1), 
        //            hobbies[1](1), hobbies[2](1), phones[0].type(1), phones[0].number(1), phones[1].type(1), phones[1].number(1)
        // Container nodes: address(1), hobbies(1), phones(1), phones[0](1), phones[1](1)
        // Total: 11 leaf + 5 container = 16
        assertEquals(16, eventCount.get());

        // When - test default behavior (no entry events for containers)
        eventCount.set(0);
        noFireEntryWalker.handler(event -> {
            eventCount.incrementAndGet();
        }).handle(complexJson);

        // Then - should only fire events for leaf nodes (no container events)
        // Only leaf nodes: name(1), age(1), address.street(1), address.city(1), hobbies[0](1), 
        //                 hobbies[1](1), hobbies[2](1), phones[0].type(1), phones[0].number(1), phones[1].type(1), phones[1].number(1)
        // Total: 11 leaf nodes only
        assertEquals(11, eventCount.get());
    }

    private JsonObject createComplexJsonStructure() {
        var address = new JsonObject()
            .put("street", "123 Main St")
            .put("city", "New York");

        var hobbies = new JsonArray()
            .add("reading")
            .add("swimming")
            .add("coding");

        var phones = new JsonArray()
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
}

