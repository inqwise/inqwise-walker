package com.inqwise.walker;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.MoreObjects;

/**
 * Implementation of ObjectWalkingEvent that provides access to walking event information.
 * 
 * <p>This class encapsulates information about a specific object being processed during
 * the walking process. It provides access to the object value, metadata, context,
 * and the walker that will be used to process the object.</p>
 * 
 * <p>The implementation uses an AtomicReference to store the walker, allowing for
 * dynamic walker assignment during event processing.</p>
 * 
 * @author Alex Misyuk
 */
public class ObjectWalkingEventImpl implements ObjectWalkingEvent {

	private IndicatedItem indicatedItem;
	private ObjectWalk walk;
	private AtomicReference<ObjectWalker> walkerReference;

	/**
	 * Constructs a new ObjectWalkingEventImpl with the specified parameters.
	 * 
	 * <p>This constructor creates an event for the specified item at the given walk level,
	 * with a reference to the walker that will be used to process the item.</p>
	 * 
	 * @param indicatedItem The item being processed
	 * @param walk The current walk level
	 * @param walkerReference Atomic reference to the walker for this item
	 */
	public ObjectWalkingEventImpl(IndicatedItem indicatedItem, ObjectWalk walk, AtomicReference<ObjectWalker> walkerReference) {
		this.indicatedItem = indicatedItem;
		this.walk = walk;
		this.walkerReference = walkerReference;
	}

	/**
	 * Returns the object currently being processed.
	 * 
	 * <p>This is the actual object value that triggered this event during traversal.</p>
	 * 
	 * @return The object being processed
	 */
	@Override
	public Object indicatedObject() {
		return indicatedItem.value();
	}
	
	/**
	 * Ends the walking process immediately.
	 * 
	 * <p>This method terminates the walking process and triggers any registered
	 * end handlers. No further objects will be processed.</p>
	 */
	@Override
	public void end() {
		walk.context().end();
	}

	/**
	 * Returns the current depth level in the object hierarchy.
	 * 
	 * <p>The level index represents how deep the walker is in the object structure.
	 * Root level is 0, each nested level increments this value.</p>
	 * 
	 * @return The current level index (0-based)
	 */
	@Override
	public int levelIndex() {
		return walk.context().levelIndex();
	}

	/**
	 * Returns a string representation of this ObjectWalkingEventImpl.
	 * 
	 * <p>The string representation includes the indicated object, metadata,
	 * and level index for debugging purposes.</p>
	 * 
	 * @return String representation of this event
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("indicatedObject", indicatedObject())
				.add("meta", meta())
				.add("levelIndex", levelIndex())
				.toString();
	}
	
	/**
	 * Returns metadata associated with the current object.
	 * 
	 * <p>Metadata includes information such as the object's path in the hierarchy,
	 * parent references, and any custom data added during processing.</p>
	 * 
	 * @return Map containing metadata key-value pairs
	 */
	@Override
	public Map<String,Object> meta(){
		return indicatedItem.meta();
	}

	/**
	 * Returns the current walk level being processed.
	 * 
	 * @return ObjectWalk representing the current level
	 */
	@Override
	public ObjectWalk level() {
		return walk;
	}
	
	/**
	 * Returns the walking context associated with this event.
	 * 
	 * <p>The context provides access to the overall walking state, registered walkers,
	 * and control methods for the walking process.</p>
	 * 
	 * @return ObjectWalkingContext for this walking session
	 */
	@Override
	public ObjectWalkingContext context() {
		return walk.context();
	}
	
	/**
	 * Returns the walker that will be used to process this object.
	 * 
	 * <p>This method returns the walker that was selected based on the object's type.
	 * If no walker is available for this object type, null is returned.</p>
	 * 
	 * @return ObjectWalker for this object type, or null if no walker is available
	 */
	@Override
	public ObjectWalker getWalker() {
		return walkerReference.get();
	}
	
	/**
	 * Checks if a walker is available for the current object.
	 * 
	 * @return true if a walker is available, false otherwise
	 */
	@Override
	public boolean hasWalker() {
		return null != walkerReference.get();
	}
	
	/**
	 * Sets the walker to be used for processing this object.
	 * 
	 * <p>This method allows customizing which walker should be used for the current object.
	 * It can be used to override the default walker selection logic.</p>
	 * 
	 * @param walker The walker to use for this object
	 */
	@Override
	public void setWalker(ObjectWalker walker) {
		walkerReference.set(walker);
	}
}
