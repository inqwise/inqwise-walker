package com.inqwise.walker;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.MoreObjects;

/**
 * Represents a single level of object walking.
 * 
 * <p>This class manages the iteration through objects at a specific level in the
 * object hierarchy. It provides event handlers for level transitions and data storage
 * for level-specific information.</p>
 * 
 * <p>Each ObjectWalk instance represents one level of traversal and contains:</p>
 * <ul>
 *   <li>An iterator for the objects at this level</li>
 *   <li>Event handlers for level transitions</li>
 *   <li>Data storage for level-specific information</li>
 *   <li>Reference to the parent item</li>
 * </ul>
 * 
 * @author Alex Misyuk
 */
public class ObjectWalk {
	
	/**
	 * Creates a new ObjectWalk instance for the specified iterator and context.
	 * 
	 * <p>This factory method creates an ObjectWalk that will iterate through the
	 * objects provided by the iterator within the given context.</p>
	 * 
	 * @param iterator Iterator for the objects at this level
	 * @param context The walking context
	 * @param indicatedItem The parent item that led to this walk level
	 * @return New ObjectWalk instance
	 */
	public static ObjectWalk walk(Iterator<IndicatedItem> iterator, ObjectWalkingContext context, IndicatedItem indicatedItem) {
		return new ObjectWalk(iterator, context, indicatedItem);
	}
	
	private static final Logger logger = LogManager.getLogger(ObjectWalk.class);
	
	private Iterator<IndicatedItem> iterator;
	private ObjectWalkingContext context;
	Consumer<Void> exitLevelHandler;
	Consumer<ObjectWalk> nextLevelHandler;
	Consumer<ObjectWalkingContext> endHandler;
	private IndicatedItem parent;
	
	/**
	 * Registers a handler to be called when exiting this level.
	 * 
	 * <p>The handler will be called when the walker finishes processing all objects
	 * at this level and moves back to the parent level.</p>
	 * 
	 * @param handler The handler to call when exiting this level
	 */
	public void onExitLevel(Consumer<Void> handler) {
		this.exitLevelHandler = handler;
	}
	
	/**
	 * Registers a handler to be called when entering the next level.
	 * 
	 * <p>The handler will be called when the walker moves to a deeper level
	 * in the object hierarchy.</p>
	 * 
	 * @param handler The handler to call when entering the next level
	 */
	public void onNextLevel(Consumer<ObjectWalk> handler) {
		this.nextLevelHandler = handler;
	}
	
	/**
	 * Registers a handler to be called when the walking process ends.
	 * 
	 * <p>The handler will be called when the entire walking process completes,
	 * either successfully or with an error.</p>
	 * 
	 * @param handler The handler to call when walking ends
	 */
	public void onEnd(Consumer<ObjectWalkingContext> handler) {
		this.endHandler = handler;
	}

	private HashMap<String,Object> data;

	/**
	 * Stores a key-value pair in this walk level's data.
	 * 
	 * <p>This method allows storing arbitrary data associated with this specific
	 * walk level. The data persists throughout the level's processing.</p>
	 * 
	 * @param key The key to store the value under
	 * @param obj The value to store
	 * @return This ObjectWalk instance for method chaining
	 */
	public ObjectWalk put(String key, Object obj) {
		getData().put(key, obj);
		return this;
	}

	/**
	 * Retrieves a value from this walk level's data.
	 * 
	 * @param <R> The expected return type
	 * @param key The key to retrieve
	 * @return The stored value, or null if not found
	 */
	@SuppressWarnings("unchecked")
	public <R> R get(String key) {
		Object obj = getData().get(key);
		return (R)obj;
	}

	/**
	 * Removes a key-value pair from this walk level's data.
	 * 
	 * @param <R> The expected return type of the removed value
	 * @param key The key to remove
	 * @return The removed value, or null if not found
	 */
	@SuppressWarnings("unchecked")
	public <R> R remove(String key) {
		Object obj = getData().remove(key);
		return (R)obj;
	}

	/**
	 * Returns all data stored at this walk level.
	 * 
	 * @return Map containing all stored key-value pairs for this level
	 */
	public Map<String, Object> data() {
		return getData();
	}
	
	/**
	 * Returns the internal data map, creating it if necessary.
	 * 
	 * @return The data map for this walk level
	 */
	private Map<String, Object> getData() {
		if (data == null) {
			data = new HashMap<>();
		}
		return data;
	}
	
	/**
	 * Private constructor for creating ObjectWalk instances.
	 * 
	 * @param iterator Iterator for objects at this level
	 * @param context The walking context
	 * @param parent The parent item that led to this level
	 */
	private ObjectWalk(Iterator<IndicatedItem> iterator, ObjectWalkingContext context, IndicatedItem parent) {
		this.context = context;
		this.iterator = iterator;
		this.parent = parent;
	}

	/**
	 * Returns a string representation of this ObjectWalk.
	 * 
	 * @return String representation
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.toString();
	}
	
	/**
	 * Returns the iterator for objects at this level.
	 * 
	 * @return Iterator for the objects to be processed at this level
	 */
	Iterator<IndicatedItem> iterator(){
		return iterator;
	}
	
	/**
	 * Returns the walking context associated with this walk level.
	 * 
	 * @return ObjectWalkingContext for this walking session
	 */
	public ObjectWalkingContext context() {
		return context;
	}

	/**
	 * Returns the parent item that led to this walk level.
	 * 
	 * <p>The parent item represents the object that contained the objects
	 * being processed at this level.</p>
	 * 
	 * @return The parent IndicatedItem
	 */
	public IndicatedItem getParent() {
		return parent;
	}
}
