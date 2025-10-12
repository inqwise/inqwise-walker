package com.inqwise.walker;

import java.util.Map;

/**
 * Event interface representing an object walking event.
 * 
 * <p>This interface provides access to information about the current object being processed
 * during the walking process. It includes the object value, metadata, context, and walking state.</p>
 * 
 * <p>Events are created for each object encountered during traversal and passed to registered
 * event handlers for processing.</p>
 * 
 * @author Alex Misyuk
 */
public interface ObjectWalkingEvent {
	
	/**
	 * Returns the current depth level in the object hierarchy.
	 * 
	 * <p>The level index represents how deep the walker is in the object structure.
	 * Root level is 0, each nested level increments this value.</p>
	 * 
	 * @return The current level index (0-based)
	 */
	int levelIndex();
	
	/**
	 * Returns the current walk level being processed.
	 * 
	 * @return ObjectWalk representing the current level
	 */
	ObjectWalk level();
	
	/**
	 * Ends the walking process immediately.
	 * 
	 * <p>This method terminates the walking process and triggers any registered
	 * end handlers. No further objects will be processed.</p>
	 */
	void end();

	/**
	 * Returns the object currently being processed.
	 * 
	 * <p>This is the actual object value that triggered this event during traversal.</p>
	 * 
	 * @return The object being processed, or null if no object is available
	 */
	Object indicatedObject();

	/**
	 * Returns metadata associated with the current object.
	 * 
	 * <p>Metadata includes information such as the object's path in the hierarchy,
	 * parent references, and any custom data added during processing.</p>
	 * 
	 * @return Map containing metadata key-value pairs
	 */
	Map<String, Object> meta();

	/**
	 * Returns the walking context associated with this event.
	 * 
	 * <p>The context provides access to the overall walking state, registered walkers,
	 * and control methods for the walking process.</p>
	 * 
	 * @return ObjectWalkingContext for this walking session
	 */
	ObjectWalkingContext context();

	/**
	 * Returns the walker that will be used to process this object.
	 * 
	 * <p>This method returns the walker that was selected based on the object's type.
	 * If no walker is available for this object type, null is returned.</p>
	 * 
	 * @return ObjectWalker for this object type, or null if no walker is available
	 */
	ObjectWalker getWalker();

	/**
	 * Checks if a walker is available for the current object.
	 * 
	 * @return true if a walker is available, false otherwise
	 */
	boolean hasWalker();

	/**
	 * Sets the walker to be used for processing this object.
	 * 
	 * <p>This method allows customizing which walker should be used for the current object.
	 * It can be used to override the default walker selection logic.</p>
	 * 
	 * @param walker The walker to use for this object
	 */
	void setWalker(ObjectWalker walker);
}
