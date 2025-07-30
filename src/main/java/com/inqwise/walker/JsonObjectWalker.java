package com.inqwise.walker;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import io.vertx.core.json.JsonObject;

/**
 * Walker implementation for Vert.x JsonObject instances.
 * 
 * <p>This walker traverses through all key-value pairs in a JsonObject, maintaining
 * path information for each field. It can handle nested JsonObjects and JsonArrays
 * by delegating to appropriate walkers.</p>
 * 
 * <p>Path information is maintained in the format "parent.child.field" where each
 * level represents a nested object structure.</p>
 * 
 * @author Alex Misyuk
 */
public class JsonObjectWalker extends ObjectWalker {
	
	/**
	 * Constructs a new JsonObjectWalker with no child walkers.
	 * 
	 * <p>This constructor creates a walker that will only process JsonObject instances
	 * without delegating to other walkers for nested objects.</p>
	 */
	public JsonObjectWalker() {
		super(null);
	}

	/**
	 * Constructs a new JsonObjectWalker with the specified child walkers.
	 * 
	 * <p>Child walkers are used to handle nested objects of specific types.
	 * For example, a JsonArrayWalker can be provided to handle nested arrays.</p>
	 * 
	 * @param walkers Set of child walkers to use for nested objects
	 */
	public JsonObjectWalker(Set<ObjectWalker> walkers) {
		super(walkers);
	}

	/**
	 * Returns the JsonObject class that this walker handles.
	 * 
	 * @return JsonObject.class
	 */
	@Override
	protected Class<?> type() {
		return JsonObject.class;
	}

	/**
	 * Removes trailing dots from path strings.
	 * 
	 * <p>This utility method cleans up path strings by removing trailing dots
	 * that might be added during path construction.</p>
	 * 
	 * @param path The path string to clean
	 * @return The cleaned path string without trailing dots
	 */
	private static String removeEndDot(String path) {
	    if (path != null && path.endsWith(".")) {
	        return path.substring(0, path.length() - 1);
	    }
	    return path;
	}
	
	/**
	 * Creates an iterator for all key-value pairs in the JsonObject.
	 * 
	 * <p>This method iterates through all entries in the JsonObject, creating
	 * IndicatedItem instances for each value with appropriate path information.
	 * The path is constructed by appending the field name to the parent path.</p>
	 * 
	 * @param indicatedItem The current item containing the JsonObject
	 * @param context The walking context
	 * @return Iterator of IndicatedItem objects for each field in the JsonObject
	 */
	@Override
	protected Iterator<IndicatedItem> createObjectIterator(IndicatedItem indicatedItem, ObjectWalkingContext context) {
		String path = Optional.ofNullable(indicatedItem.meta().get(Keys.PATH)).map(Object::toString).orElse(".");
		
		var json = (JsonObject)indicatedItem.value();
		
		return
		json.stream().map(entry ->
			indicatedItem.newSubItem(entry.getValue()).put(Keys.PATH, removeEndDot(path) + "." + entry.getKey())
		).iterator();
		
	}
	
	/**
	 * Registers an event handler for this JsonObjectWalker.
	 * 
	 * <p>This method overrides the parent handler method to provide type-safe
	 * method chaining for JsonObjectWalker instances.</p>
	 * 
	 * @param event The event handler to register
	 * @return This JsonObjectWalker instance for method chaining
	 */
	@Override
	public JsonObjectWalker handler(Consumer<ObjectWalkingEvent> event) {
		super.handler(event);
		return this;
	}
	
	/**
	 * Creates a JsonObjectWalker instance with built-in support for nested objects and arrays.
	 * 
	 * <p>This factory method creates a JsonObjectWalker that can handle nested JsonObjects
	 * and JsonArrays by including appropriate child walkers.</p>
	 * 
	 * @return JsonObjectWalker instance with JsonObject and JsonArray child walkers
	 */
	public static JsonObjectWalker instance() {
		return new JsonObjectWalker(Set.of(new JsonObjectWalker(), new JsonArrayWalker()));
	}
}