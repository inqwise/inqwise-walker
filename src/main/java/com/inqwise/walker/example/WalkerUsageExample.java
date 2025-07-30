package com.inqwise.walker.example;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Sets;
import com.inqwise.walker.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Complete example demonstrating how to implement and use the inqwise-walker library.
 * 
 * This example covers:
 * 1. Basic usage with built-in walkers
 * 2. Creating custom walkers
 * 3. Event handling and flow control
 * 4. Error handling
 * 5. Context data sharing
 */
public class WalkerUsageExample {

    public static void main(String[] args) {
        System.out.println("=== Inqwise Walker Usage Examples ===\n");
        
        // Example 1: Basic JSON walking
        basicJsonWalking();
        
        // Example 2: Custom walker implementation
        customWalkerExample();
        
        // Example 3: Advanced event handling
        advancedEventHandling();
        
        // Example 4: Flow control (pause/resume/end)
        flowControlExample();
        
        // Example 5: Error handling
        errorHandlingExample();
    }

    /**
     * Example 1: Basic JSON Object Walking
     * Shows how to walk through a JSON structure with built-in walkers
     */
    public static void basicJsonWalking() {
        System.out.println("1. Basic JSON Walking:");
        
        // Create a sample JSON structure
        JsonObject person = new JsonObject()
            .put("name", "John Doe")
            .put("age", 30)
            .put("address", new JsonObject()
                .put("street", "123 Main St")
                .put("city", "New York"))
            .put("hobbies", new JsonArray()
                .add("reading")
                .add("swimming")
                .add("coding"));

        // Create walker with nested object/array support
        JsonObjectWalker walker = JsonObjectWalker.instance();
        
        // Add event handler to process each visited object
        walker.handler(event -> {
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            Object value = event.indicatedObject();
            System.out.printf("  Path: %-20s Value: %s%n", path, value);
        });
        
        // Add completion handler
        walker.endHandler(context -> {
            System.out.println("  Walking completed successfully!");
        });
        
        // Start walking
        ObjectWalkingContext context = walker.handle(person);
        System.out.println("  Final success: " + context.success() + "\n");
    }

    /**
     * Example 2: Custom Walker Implementation
     * Shows how to create a custom walker for your own data types
     */
    public static void customWalkerExample() {
        System.out.println("2. Custom Walker Implementation:");
        
        // Sample custom data structure
        class Person {
            String name;
            int age;
            List<String> skills;
            
            Person(String name, int age, List<String> skills) {
                this.name = name;
                this.age = age;
                this.skills = skills;
            }
        }
        
        // Custom walker for Person objects
        class PersonWalker extends ObjectWalker {
            public PersonWalker() {
                super(null);
            }
            
            @Override
            protected Class<?> type() {
                return Person.class;
            }
            
            @Override
            protected Iterator<IndicatedItem> createObjectIterator(IndicatedItem indicatedItem, ObjectWalkingContext context) {
                Person person = (Person) indicatedItem.value();
                String basePath = indicatedItem.meta().getOrDefault(Keys.PATH, ".").toString();
                
                return List.of(
                    indicatedItem.newSubItem(person.name).put(Keys.PATH, basePath + ".name"),
                    indicatedItem.newSubItem(person.age).put(Keys.PATH, basePath + ".age"),
                    indicatedItem.newSubItem(person.skills).put(Keys.PATH, basePath + ".skills")
                ).iterator();
            }
        }
        
        // Create sample data
        Person person = new Person("Alice", 25, List.of("Java", "Python", "JavaScript"));
        
        // Create walker
        PersonWalker walker = new PersonWalker();
        walker.handler(event -> {
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            Object value = event.indicatedObject();
            System.out.printf("  Path: %-15s Value: %s%n", path, value);
        });
        
        // Walk the object
        ObjectWalkingContext context = walker.handle(person);
        System.out.println("  Custom walker completed: " + context.success() + "\n");
    }

    /**
     * Example 3: Advanced Event Handling
     * Shows event filtering, context data sharing, and walker delegation
     */
    public static void advancedEventHandling() {
        System.out.println("3. Advanced Event Handling:");
        
        JsonObject data = new JsonObject()
            .put("users", new JsonArray()
                .add(new JsonObject().put("name", "John").put("role", "admin"))
                .add(new JsonObject().put("name", "Jane").put("role", "user")))
            .put("settings", new JsonObject()
                .put("theme", "dark")
                .put("notifications", true));

        JsonObjectWalker walker = JsonObjectWalker.instance();
        AtomicInteger adminCount = new AtomicInteger(0);
        
        walker.handler(event -> {
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            Object value = event.indicatedObject();
            
            // Filter and count admin users
            if (path.contains("role") && "admin".equals(value)) {
                adminCount.incrementAndGet();
                event.context().put("lastAdmin", path);
            }
            
            // Store settings in context
            if (path.startsWith(".settings.")) {
                String settingName = path.substring(".settings.".length());
                event.context().put("setting_" + settingName, value);
            }
            
            System.out.printf("  Path: %-25s Value: %s%n", path, value);
        });
        
        ObjectWalkingContext context = walker.handle(data);
        
        // Access shared context data
        System.out.println("  Admin count: " + adminCount.get());
        System.out.println("  Last admin at: " + context.get("lastAdmin"));
        System.out.println("  Theme setting: " + context.get("setting_theme"));
        System.out.println("  Notifications: " + context.get("setting_notifications") + "\n");
    }

    /**
     * Example 4: Flow Control
     * Shows how to pause, resume, and early terminate walking
     */
    public static void flowControlExample() {
        System.out.println("4. Flow Control Example:");
        
        JsonObject data = new JsonObject()
            .put("metadata", new JsonObject().put("version", "1.0"))
            .put("content", new JsonArray().add("item1").add("item2").add("item3"))
            .put("footer", new JsonObject().put("copyright", "2024"));

        JsonObjectWalker walker = JsonObjectWalker.instance();
        AtomicInteger processedCount = new AtomicInteger(0);
        
        walker.handler(event -> {
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            Object value = event.indicatedObject();
            processedCount.incrementAndGet();
            
            System.out.printf("  Processing: %-20s Value: %s%n", path, value);
            
            // Pause after processing metadata
            if (path.equals(".metadata.version")) {
                System.out.println("  [PAUSING after metadata]");
                event.context().pause();
                
                // Simulate some processing time
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                
                System.out.println("  [RESUMING]");
                event.context().resume();
            }
            
            // Early termination after processing content array
            if (path.equals(".content[2]")) { // Stop after third item
                System.out.println("  [ENDING EARLY]");
                event.end();
            }
        });
        
        ObjectWalkingContext context = walker.handle(data);
        System.out.println("  Total processed: " + processedCount.get());
        System.out.println("  Completed: " + context.success() + "\n");
    }

    /**
     * Example 5: Error Handling
     * Shows how to handle errors during walking
     */
    public static void errorHandlingExample() {
        System.out.println("5. Error Handling Example:");
        
        JsonObject data = new JsonObject()
            .put("valid", "data")
            .put("problematic", new JsonArray().add("item1").add("item2"))
            .put("more", "data");

        JsonObjectWalker walker = JsonObjectWalker.instance();
        AtomicInteger errorCount = new AtomicInteger(0);
        
        walker.handler(event -> {
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            Object value = event.indicatedObject();
            
            System.out.printf("  Processing: %-20s Value: %s%n", path, value);
            
            // Simulate an error on specific path
            if (path.equals(".problematic[1]")) {
                throw new RuntimeException("Simulated processing error!");
            }
        });
        
        // Error handler
        walker.errorHandler(context -> {
            errorCount.incrementAndGet();
            System.out.println("  ERROR: " + context.cause().getMessage());
            System.out.println("  Walking failed at level: " + context.levelIndex());
        });
        
        // Success handler (won't be called in this case)
        walker.endHandler(context -> {
            if (context.success()) {
                System.out.println("  Walking completed successfully!");
            }
        });
        
        ObjectWalkingContext context = walker.handle(data);
        System.out.println("  Errors encountered: " + errorCount.get());
        System.out.println("  Final state - Success: " + context.success() + ", Failed: " + context.failed());
        
        if (context.failed()) {
            System.out.println("  Error cause: " + context.cause().getMessage());
        }
        System.out.println();
    }

    /**
     * Bonus: Creating a complete walker ecosystem
     * Shows how to combine multiple walkers for complex data structures
     */
    public static void complexWalkerEcosystem() {
        System.out.println("6. Complex Walker Ecosystem:");
        
        // This would demonstrate combining multiple custom walkers
        // with the built-in JSON walkers for complex business objects
        // that contain mixed data types (JSON + custom objects)
    }
}
