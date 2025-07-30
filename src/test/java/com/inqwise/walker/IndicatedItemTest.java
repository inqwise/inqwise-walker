package com.inqwise.walker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for IndicatedItem functionality.
 */
class IndicatedItemTest {

    private IndicatedItem rootItem;
    private IndicatedItem childItem;
    private TestData testData;

    @BeforeEach
    void setUp() {
        testData = new TestData("test", 42);
        rootItem = new IndicatedItem(testData);
        childItem = rootItem.newSubItem("childValue");
    }

    @Test
    void testConstructor() {
        // Given & When
        IndicatedItem item = new IndicatedItem(testData);
        
        // Then
        assertEquals(testData, item.value());
        assertNotNull(item.meta());
        assertTrue(item.meta().isEmpty());
    }

    @Test
    void testValue() {
        // When & Then
        assertEquals(testData, rootItem.value());
        assertEquals("childValue", childItem.value());
    }

    @Test
    void testMeta() {
        // Given
        rootItem.put("key1", "value1");
        rootItem.put("key2", 123);
        
        // When
        Map<String, Object> meta = rootItem.meta();
        
        // Then
        assertNotNull(meta);
        assertEquals("value1", meta.get("key1"));
        assertEquals(123, meta.get("key2"));
        assertEquals(2, meta.size());
    }

    @Test
    void testNewSubItem() {
        // When
        IndicatedItem subItem = rootItem.newSubItem("subValue");
        
        // Then
        assertEquals("subValue", subItem.value());
        assertNotNull(subItem.meta());
        assertTrue(subItem.meta().isEmpty());
    }

    @Test
    void testPut() {
        // Given
        IndicatedItem item = new IndicatedItem(testData);
        
        // When
        IndicatedItem result = item.put("testKey", "testValue");
        
        // Then
        assertEquals(item, result); // Method chaining
        assertEquals("testValue", item.meta().get("testKey"));
    }

    @Test
    void testPutMultipleValues() {
        // Given
        IndicatedItem item = new IndicatedItem(testData);
        
        // When
        item.put("key1", "value1")
            .put("key2", 42)
            .put("key3", true);
        
        // Then
        Map<String, Object> meta = item.meta();
        assertEquals("value1", meta.get("key1"));
        assertEquals(42, meta.get("key2"));
        assertEquals(true, meta.get("key3"));
        assertEquals(3, meta.size());
    }

    @Test
    void testPutOverwrite() {
        // Given
        IndicatedItem item = new IndicatedItem(testData);
        item.put("key", "original");
        
        // When
        item.put("key", "updated");
        
        // Then
        assertEquals("updated", item.meta().get("key"));
    }

    @Test
    void testToString() {
        // Given
        IndicatedItem item = new IndicatedItem(testData);
        item.put("key1", "value1");
        item.put("key2", 42);
        
        // When
        String result = item.toString();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("IndicatedItem"));
        assertTrue(result.contains("value="));
        assertTrue(result.contains("meta="));
    }

    @Test
    void testMetaLazyInitialization() {
        // Given
        IndicatedItem item = new IndicatedItem(testData);
        
        // When
        Map<String, Object> meta1 = item.meta();
        Map<String, Object> meta2 = item.meta();
        
        // Then
        assertNotNull(meta1);
        assertNotNull(meta2);
        assertSame(meta1, meta2); // Same instance
    }

    @Test
    void testSubItemIndependence() {
        // Given
        IndicatedItem parent = new IndicatedItem("parent");
        IndicatedItem child1 = parent.newSubItem("child1");
        IndicatedItem child2 = parent.newSubItem("child2");
        
        // When
        child1.put("key1", "value1");
        child2.put("key2", "value2");
        
        // Then
        assertEquals("value1", child1.meta().get("key1"));
        assertNull(child1.meta().get("key2"));
        assertEquals("value2", child2.meta().get("key2"));
        assertNull(child2.meta().get("key1"));
        assertTrue(parent.meta().isEmpty());
    }

    @Test
    void testNullValue() {
        // Given & When
        IndicatedItem item = new IndicatedItem(null);
        
        // Then
        assertNull(item.value());
        assertNotNull(item.meta());
    }

    @Test
    void testComplexObjectValue() {
        // Given
        TestData complexData = new TestData("complex", 100);
        IndicatedItem item = new IndicatedItem(complexData);
        
        // When
        item.put("path", ".complex.field");
        
        // Then
        assertEquals(complexData, item.value());
        assertEquals(".complex.field", item.meta().get("path"));
    }

    @Test
    void testMetaWithNullValues() {
        // Given
        IndicatedItem item = new IndicatedItem(testData);
        
        // When
        item.put("nullKey", null);
        item.put("emptyKey", "");
        
        // Then
        assertNull(item.meta().get("nullKey"));
        assertEquals("", item.meta().get("emptyKey"));
    }

    @Test
    void testMultipleSubItems() {
        // Given
        IndicatedItem root = new IndicatedItem("root");
        
        // When
        IndicatedItem child1 = root.newSubItem("child1");
        IndicatedItem child2 = root.newSubItem("child2");
        IndicatedItem grandchild = child1.newSubItem("grandchild");
        
        // Then
        assertEquals("root", root.value());
        assertEquals("child1", child1.value());
        assertEquals("child2", child2.value());
        assertEquals("grandchild", grandchild.value());
    }

    @Test
    void testMetaPersistence() {
        // Given
        IndicatedItem item = new IndicatedItem(testData);
        
        // When
        item.put("persistent", "value");
        Map<String, Object> meta1 = item.meta();
        Map<String, Object> meta2 = item.meta();
        
        // Then
        assertSame(meta1, meta2);
        assertEquals("value", meta1.get("persistent"));
        assertEquals("value", meta2.get("persistent"));
    }

    // Test data class
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