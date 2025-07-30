package com.inqwise.walker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ObjectWalkingEvent functionality.
 */
class ObjectWalkingEventTest {

    private ObjectWalkingEventImpl event;
    private IndicatedItem indicatedItem;
    private ObjectWalk walk;
    private AtomicReference<ObjectWalker> walkerReference;
    private TestObjectWalker testWalker;

    @BeforeEach
    void setUp() {
        indicatedItem = new IndicatedItem("testValue");
        walkerReference = new AtomicReference<>();
        testWalker = new TestObjectWalker();
        walkerReference.set(testWalker);
        
        // Create a mock ObjectWalk using the factory method
        walk = ObjectWalk.walk(
            java.util.Collections.singletonList(indicatedItem).iterator(),
            new ObjectWalkingContextImpl("test", com.google.common.collect.HashBiMap.create(), java.util.Collections.emptySet()),
            indicatedItem
        );
        
        event = new ObjectWalkingEventImpl(indicatedItem, walk, walkerReference);
    }

    @Test
    void testIndicatedObject() {
        // When & Then
        assertEquals("testValue", event.indicatedObject());
    }

    @Test
    void testMeta() {
        // Given
        indicatedItem.put("path", ".test.path");
        indicatedItem.put("level", 1);
        
        // When
        Map<String, Object> meta = event.meta();
        
        // Then
        assertNotNull(meta);
        assertEquals(".test.path", meta.get("path"));
        assertEquals(1, meta.get("level"));
    }

    @Test
    void testLevelIndex() {
        // When & Then
        assertEquals(0, event.levelIndex());
    }

    @Test
    void testLevel() {
        // When & Then
        assertNotNull(event.level());
        assertEquals(walk, event.level());
    }

    @Test
    void testContext() {
        // When & Then
        assertNotNull(event.context());
        assertTrue(event.context() instanceof ObjectWalkingContextImpl);
    }

    @Test
    void testGetWalker() {
        // When & Then
        assertEquals(testWalker, event.getWalker());
    }

    @Test
    void testHasWalker() {
        // When & Then
        assertTrue(event.hasWalker());
        
        // When walker is null
        walkerReference.set(null);
        assertFalse(event.hasWalker());
    }

    @Test
    void testSetWalker() {
        // Given
        TestObjectWalker newWalker = new TestObjectWalker();
        
        // When
        event.setWalker(newWalker);
        
        // Then
        assertEquals(newWalker, event.getWalker());
        assertTrue(event.hasWalker());
    }

    @Test
    void testEnd() {
        // Given
        ObjectWalkingContext context = event.context();
        
        // When
        event.end();
        
        // Then
        assertTrue(context.ended());
    }

    @Test
    void testToString() {
        // Given
        indicatedItem.put("path", ".test.path");
        
        // When
        String result = event.toString();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("ObjectWalkingEventImpl"));
        assertTrue(result.contains("indicatedObject="));
        assertTrue(result.contains("meta="));
        assertTrue(result.contains("levelIndex="));
    }

    @Test
    void testEventWithNullWalker() {
        // Given
        walkerReference.set(null);
        
        // When & Then
        assertNull(event.getWalker());
        assertFalse(event.hasWalker());
    }

    @Test
    void testEventWithNullIndicatedObject() {
        // Given
        IndicatedItem nullItem = new IndicatedItem(null);
        ObjectWalkingEventImpl nullEvent = new ObjectWalkingEventImpl(nullItem, walk, walkerReference);
        
        // When & Then
        assertNull(nullEvent.indicatedObject());
    }

    @Test
    void testEventWithComplexObject() {
        // Given
        TestData complexData = new TestData("complex", 100);
        IndicatedItem complexItem = new IndicatedItem(complexData);
        complexItem.put("path", ".complex.field");
        
        ObjectWalkingEventImpl complexEvent = new ObjectWalkingEventImpl(complexItem, walk, walkerReference);
        
        // When & Then
        assertEquals(complexData, complexEvent.indicatedObject());
        assertEquals(".complex.field", complexEvent.meta().get("path"));
    }

    @Test
    void testEventWithEmptyMeta() {
        // Given
        IndicatedItem emptyItem = new IndicatedItem("value");
        ObjectWalkingEventImpl emptyEvent = new ObjectWalkingEventImpl(emptyItem, walk, walkerReference);
        
        // When & Then
        assertNotNull(emptyEvent.meta());
        assertTrue(emptyEvent.meta().isEmpty());
    }

    @Test
    void testEventWithMultipleMetaEntries() {
        // Given
        indicatedItem.put("path", ".test.path");
        indicatedItem.put("level", 1);
        indicatedItem.put("type", "string");
        indicatedItem.put("nullable", true);
        
        // When
        Map<String, Object> meta = event.meta();
        
        // Then
        assertEquals(4, meta.size());
        assertEquals(".test.path", meta.get("path"));
        assertEquals(1, meta.get("level"));
        assertEquals("string", meta.get("type"));
        assertEquals(true, meta.get("nullable"));
    }

    @Test
    void testEventWithNullMetaValues() {
        // Given
        indicatedItem.put("nullValue", null);
        indicatedItem.put("emptyString", "");
        
        // When
        Map<String, Object> meta = event.meta();
        
        // Then
        assertNull(meta.get("nullValue"));
        assertEquals("", meta.get("emptyString"));
    }

    @Test
    void testEventWithDifferentWalkerTypes() {
        // Given
        TestArrayWalker arrayWalker = new TestArrayWalker();
        
        // When
        event.setWalker(arrayWalker);
        
        // Then
        assertEquals(arrayWalker, event.getWalker());
        assertTrue(event.hasWalker());
    }

    @Test
    void testEventContextInteraction() {
        // Given
        ObjectWalkingContext context = event.context();
        
        // When
        context.put("testKey", "testValue");
        
        // Then
        assertEquals("testValue", context.get("testKey"));
        assertEquals("testValue", event.context().get("testKey"));
    }

    @Test
    void testEventLevelInteraction() {
        // Given
        ObjectWalk level = event.level();
        
        // When
        level.put("levelKey", "levelValue");
        
        // Then
        assertEquals("levelValue", level.get("levelKey"));
        assertEquals("levelValue", event.level().get("levelKey"));
    }

    // Test implementation classes
    private static class TestObjectWalker extends ObjectWalker {
        
        public TestObjectWalker() {
            super(null);
        }

        @Override
        protected Class<?> type() {
            return String.class;
        }

        @Override
        protected java.util.Iterator<IndicatedItem> createObjectIterator(IndicatedItem indicatedItem, ObjectWalkingContext context) {
            return java.util.Collections.emptyIterator();
        }
    }

    private static class TestArrayWalker extends ObjectWalker {
        
        public TestArrayWalker() {
            super(null);
        }

        @Override
        protected Class<?> type() {
            return String[].class;
        }

        @Override
        protected java.util.Iterator<IndicatedItem> createObjectIterator(IndicatedItem indicatedItem, ObjectWalkingContext context) {
            return java.util.Collections.emptyIterator();
        }
    }

    private static class TestData {
        private final String name;
        private final int value;

        public TestData(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestData testData = (TestData) obj;
            return value == testData.value && java.util.Objects.equals(name, testData.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, value);
        }

        @Override
        public String toString() {
            return "TestData{name='" + name + "', value=" + value + "}";
        }
    }
} 