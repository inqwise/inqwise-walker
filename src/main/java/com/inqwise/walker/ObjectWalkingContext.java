package com.inqwise.walker;

import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.BiMap;

/**
 * Context interface for managing object walking state and control flow.
 * 
 * <p>This interface provides methods to control the walking process, access walking state,
 * and manage data associated with the walking context.</p>
 * 
 * <p>The context maintains:</p>
 * <ul>
 *   <li>Current walking level and position</li>
 *   <li>Registered walkers for different object types</li>
 *   <li>Contextual data and metadata</li>
 *   <li>Flow control state (paused, ended, etc.)</li>
 * </ul>
 * 
 * @author Alex Misyuk
 */
public interface ObjectWalkingContext {

	/**
	 * Handles the specified ObjectWalk and begins processing.
	 * 
	 * <p>This method initiates the walking process for the given walk object.
	 * It manages the walking stack and coordinates the traversal.</p>
	 * 
	 * @param objectWalk The walk object to process
	 */
	void handle(ObjectWalk objectWalk);

	/**
	 * Checks if the walking process has ended.
	 * 
	 * @return true if walking has ended, false otherwise
	 */
	boolean ended();

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
	 * Skips the current level and moves to the next item at the same level.
	 * 
	 * <p>This method allows for selective processing by skipping certain
	 * branches of the object hierarchy.</p>
	 */
	void skipLevel();

	/**
	 * Ends the walking process immediately.
	 * 
	 * <p>This method terminates the walking process and triggers any registered
	 * end handlers. The context will be marked as ended and no further processing
	 * will occur.</p>
	 */
	void end();

	/**
	 * Returns the registered walkers mapped by their supported types.
	 * 
	 * @return BiMap containing walkers mapped by their Class types
	 */
	BiMap<Class<?>, ObjectWalker> walkers();

	/**
	 * Stores a key-value pair in the context data.
	 * 
	 * <p>This method allows storing arbitrary data associated with the walking context.
	 * The data persists throughout the walking process and can be accessed by handlers.</p>
	 * 
	 * @param key The key to store the value under
	 * @param obj The value to store
	 * @return This context instance for method chaining
	 */
	ObjectWalkingContext put(String key, Object obj);

	/**
	 * Retrieves a value from the context data.
	 * 
	 * @param <R> The expected return type
	 * @param key The key to retrieve
	 * @return The stored value, or null if not found
	 */
	<R> R get(String key);

	/**
	 * Returns all context data as a map.
	 * 
	 * @return Map containing all stored key-value pairs
	 */
	Map<String, Object> data();

	/**
	 * Removes a key-value pair from the context data.
	 * 
	 * @param <R> The expected return type of the removed value
	 * @param key The key to remove
	 * @return The removed value, or null if not found
	 */
	<R> R remove(String key);

	/**
	 * Returns the current walk level being processed.
	 * 
	 * @return ObjectWalk representing the current level
	 */
	ObjectWalk currentLevel();

	/**
	 * Registers a handler to be called when walking ends.
	 * 
	 * <p>The handler will be called regardless of whether the walking completed
	 * successfully or failed with an error.</p>
	 * 
	 * @param onEnd The handler to call when walking ends
	 * @return This context instance for method chaining
	 */
	ObjectWalkingContext endHandler(Consumer<ObjectWalkingContext> onEnd);

	/**
	 * Checks if the walking process completed successfully.
	 * 
	 * @return true if walking completed without errors, false otherwise
	 */
	boolean success();
	
	/**
	 * Checks if the walking process failed with an error.
	 * 
	 * @return true if walking failed with an error, false otherwise
	 */
	boolean failed();
	
	/**
	 * Returns the cause of failure if the walking process failed.
	 * 
	 * @return Throwable representing the error cause, or null if no error occurred
	 */
	Throwable cause();

	/**
	 * Checks if the walking process is currently paused.
	 * 
	 * @return true if walking is paused, false otherwise
	 */
	boolean paused();

	/**
	 * Pauses the walking process.
	 * 
	 * <p>When paused, the walking process will stop at the current position
	 * and wait for a resume() call before continuing.</p>
	 * 
	 * @return This context instance for method chaining
	 */
	ObjectWalkingContext pause();

	/**
	 * Resumes the walking process after being paused.
	 * 
	 * <p>This method continues the walking process from where it was paused.
	 * If the context is not paused, this method has no effect.</p>
	 */
	void resume();
}
