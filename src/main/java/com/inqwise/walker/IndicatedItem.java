package com.inqwise.walker;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.MoreObjects;

/**
 * Wrapper class for objects being processed during walking.
 * 
 * <p>This class encapsulates an object value along with its metadata and parent relationship.
 * It provides a way to track the object's position in the hierarchy and store additional
 * information about the object during processing.</p>
 * 
 * <p>Each IndicatedItem contains:</p>
 * <ul>
 *   <li>The actual object value being processed</li>
 *   <li>Metadata associated with the object (e.g., path information)</li>
 *   <li>Reference to the parent item in the hierarchy</li>
 * </ul>
 * 
 * @author Alex Misyuk
 */
public class IndicatedItem {
	private Object value;
	private Map<String,Object> meta;
	private IndicatedItem parent;

	/**
	 * Constructs a new IndicatedItem for the specified value.
	 * 
	 * <p>This constructor creates a root-level item with no parent.</p>
	 * 
	 * @param value The object value to wrap
	 */
	public IndicatedItem(Object value) {
		this.value = value;
		this.parent = null;
	}
	
	/**
	 * Private constructor for creating child items.
	 * 
	 * <p>This constructor is used internally to create child items that have
	 * a parent relationship in the object hierarchy.</p>
	 * 
	 * @param value The object value to wrap
	 * @param parent The parent item in the hierarchy
	 */
	private IndicatedItem(Object value, IndicatedItem parent) {
		this.value = value;
		this.parent = parent;
	}
	
	/**
	 * Returns the wrapped object value.
	 * 
	 * @return The object value being processed
	 */
	public Object value() {
		return value;
	}
	
	/**
	 * Returns the metadata associated with this item.
	 * 
	 * <p>Metadata includes information such as the object's path in the hierarchy,
	 * parent references, and any custom data added during processing.</p>
	 * 
	 * @return Map containing metadata key-value pairs
	 */
	public Map<String, Object> meta() {
		return getMeta();
	}
	
	/**
	 * Returns the internal metadata map, creating it if necessary.
	 * 
	 * @return The metadata map for this item
	 */
	private Map<String, Object> getMeta() {
		if (meta == null) {
			meta = new HashMap<>();
		}
		return meta;
	}
	
	/**
	 * Creates a new child item for the specified value.
	 * 
	 * <p>This method creates a new IndicatedItem that represents a child of this item
	 * in the object hierarchy. The new item will inherit metadata from this parent
	 * and can have additional metadata added to it.</p>
	 * 
	 * @param value The object value for the child item
	 * @return New IndicatedItem representing the child
	 */
	public IndicatedItem newSubItem(Object value) {
		return new IndicatedItem(value, this);
	}

	/**
	 * Stores a key-value pair in this item's metadata.
	 * 
	 * <p>This method allows storing arbitrary data associated with this specific item.
	 * The metadata persists throughout the item's processing and can be accessed by handlers.</p>
	 * 
	 * @param key The key to store the value under
	 * @param obj The value to store
	 * @return This IndicatedItem instance for method chaining
	 */
	public IndicatedItem put(String key, Object obj) {
		getMeta().put(key, obj);
		return this;
	}
	
	/**
	 * Returns a string representation of this IndicatedItem.
	 * 
	 * @return String representation including value and metadata
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("value", value).add("meta", meta).toString();
	}
}
