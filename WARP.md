# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is **inqwise-walker**, a powerful and flexible Java library for traversing complex object hierarchies with event-driven processing. The project implements a micro-kernel architecture with extensible walker implementations for different data types.

### Architecture Principles

- **Event-driven reactive architecture**: All object traversal is handled through event handlers
- **Micro-kernel design with plugin-based providers**: Core `ObjectWalker` base class with specialized walker implementations
- **Extensible walker registry**: Built-in walkers for JSON data structures (JsonObject, JsonArray) plus support for custom walkers
- **Context-aware processing**: Hierarchical context management with path tracking and data sharing between levels
- **Flow control**: Support for pause/resume/terminate operations during traversal

## Build Commands

### Prerequisites
- Java 21+
- Maven 3.6.3+

### Common Maven Commands
```bash
# Compile the project
mvn compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ObjectWalkerTest

# Run a specific test method
mvn test -Dtest=ObjectWalkerTest#testBasicWalking

# Create JAR
mvn package

# Install to local repository
mvn install

# Run the usage examples
mvn exec:java -Dexec.mainClass="com.inqwise.walker.example.WalkerUsageExample"

# Clean build artifacts
mvn clean
```

### Testing
- Uses JUnit 5 for unit testing
- Test resources located in `src/test/resources/`
- Integration tests demonstrate real-world usage patterns
- Test utilities in `TestUtils.java` provide common testing patterns

## Core Architecture

### Key Components

**ObjectWalker (Abstract Base)**
- Foundation class providing walker registry, event management, and flow control
- Manages delegation to specialized walkers based on object types
- Handles error propagation and success/failure callbacks

**Built-in Walker Implementations**
- `JsonObjectWalker`: Traverses Vert.x JsonObject with path tracking (e.g., ".user.name")  
- `JsonArrayWalker`: Processes JsonArray elements with indexed paths (e.g., ".items[0]")
- `DifferencesWalker`: Handles difference objects from inqwise-difference library

**Context Management**
- `ObjectWalkingContext`: Manages traversal state, flow control, and shared data storage
- `ObjectWalkingEvent`: Provides event information and control methods for handlers
- `ObjectWalk`: Represents a single level of traversal with level-specific data
- `IndicatedItem`: Wraps objects with metadata during processing

### Processing Flow
```
ObjectWalker.handle(object)
    ↓
Create ObjectWalkingContext  
    ↓
For each object in hierarchy:
    ↓
Generate ObjectWalkingEvent
    ↓
Call registered event handlers
    ↓
Delegate to appropriate walker
    ↓
Continue until complete/terminated
```

### Event-Driven Processing
- Register event handlers via `walker.handler(event -> {...})`  
- Access object value: `event.indicatedObject()`
- Access path: `event.meta().get(ObjectWalker.Keys.PATH)`
- Control flow: `event.end()`, `event.context().pause()`, `event.context().resume()`
- Share data: `event.context().put(key, value)` and `event.context().get(key)`

## Development Patterns

### Creating Custom Walkers
When implementing new walker types, extend `ObjectWalker` and implement:
- `type()`: Return the Class this walker handles
- `createObjectIterator()`: Define how to iterate through the object structure
- Optional: Override `fireEntryEvent()` for entry object event handling

Example pattern from codebase:
```java
public class CustomWalker extends ObjectWalker {
    @Override
    protected Class<?> type() {
        return MyCustomClass.class;
    }
    
    @Override  
    protected Iterator<IndicatedItem> createObjectIterator(IndicatedItem item, ObjectWalkingContext context) {
        MyCustomClass obj = (MyCustomClass) item.value();
        String basePath = item.meta().getOrDefault(Keys.PATH, ".").toString();
        
        return List.of(
            item.newSubItem(obj.getField()).put(Keys.PATH, basePath + ".field")
        ).iterator();
    }
}
```

### Path Conventions
- Root path is "."
- Object properties: ".propertyName"  
- Nested objects: ".parent.child"
- Array elements: ".arrayName[index]"
- Remove trailing dots with `removeEndDot()` utility

### Error Handling Patterns
```java
walker.handler(event -> {
    // Handler logic that might throw
});

walker.errorHandler(context -> {
    System.err.println("Error: " + context.cause().getMessage());
    System.err.println("Failed at level: " + context.levelIndex());
});

walker.endHandler(context -> {
    if (context.success()) {
        // Handle success
    }
});
```

## Dependencies

The library uses provided scope for most dependencies to avoid version conflicts:
- **Vert.x Core** (4.5.10): JSON processing support  
- **Google Guava** (33.4.0-jre): Collections utilities
- **Apache Log4j** (2.22.1): Logging framework
- **Inqwise Difference** (1.0.0): Difference processing support

## Project Rules & Conventions

Based on user rules, this project:
- Follows Inqwise's event-driven, reactive architecture principles
- Uses Vert.x 5 core tools (note: pom.xml shows 4.5.10, may need updating)
- Implements micro-kernel design with plugin-based provider modules
- Should commit changes on a daily basis for the entire project rather than after individual tasks

## Examples Location

Comprehensive usage examples are available in:
- `src/main/java/com/inqwise/walker/example/WalkerUsageExample.java`

This file demonstrates:
- Basic JSON walking
- Custom walker implementation  
- Advanced event handling with context data
- Flow control (pause/resume/end)
- Error handling patterns