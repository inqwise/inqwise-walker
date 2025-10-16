package com.inqwise.walker;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.collect.BiMap;
import com.google.common.collect.Sets;

/**
 * Implementation of ObjectWalkingContext that manages the walking state and control flow.
 * 
 * <p>This class provides the core implementation for object walking, including:</p>
 * <ul>
 *   <li>Walking stack management for handling nested object traversal</li>
 *   <li>Event processing and handler coordination</li>
 *   <li>Flow control (pause, resume, end, skip)</li>
 *   <li>Error handling and state management</li>
 *   <li>Context data storage and retrieval</li>
 * </ul>
 * 
 * <p>The implementation uses a stack-based approach to manage multiple levels
 * of object traversal and provides thread-safe operations for concurrent access.</p>
 * 
 * @author Alex Misyuk
 */
public class ObjectWalkingContextImpl implements ObjectWalkingContext {
	private static final Logger logger = LogManager.getLogger(ObjectWalkingContextImpl.class);
	
	private ArrayDeque<ObjectWalk> objectWalkStack;
	private Object object;
	private boolean ended;
	private boolean paused;
	private Throwable cause;
	private BiMap<Class<?>, ObjectWalker> walkers;
	private HashMap<String,Object> data;
	private Set<Runnable> endHandlers;
	private Set<Consumer<ObjectWalkingEvent>> contextEvents;
	private Stack<Consumer<ObjectWalk>> enterLevelHandlers;
	
	/**
	 * Constructs a new ObjectWalkingContextImpl with the specified parameters.
	 * 
	 * <p>This constructor initializes the walking context with the root object,
	 * registered walkers, and event handlers.</p>
	 * 
	 * @param object The root object to walk through
	 * @param walkers BiMap of registered walkers by their supported types
	 * @param contextEvents Set of event handlers to call during walking
	 */
	public ObjectWalkingContextImpl(Object object, BiMap<Class<?>, ObjectWalker> walkers, Set<Consumer<ObjectWalkingEvent>> contextEvents) {
		this.walkers = walkers;
		this.contextEvents = contextEvents;
		this.objectWalkStack = new ArrayDeque<>();
		this.object = object;
		this.endHandlers = Sets.newHashSet();
	}
	
	/**
	 * Stores a key-value pair in the context data.
	 * 
	 * @param key The key to store the value under
	 * @param obj The value to store
	 * @return This context instance for method chaining
	 */
	@Override
	public ObjectWalkingContextImpl put(String key, Object obj) {
		getData().put(key, obj);
		return this;
	}

	/**
	 * Retrieves a value from the context data.
	 * 
	 * @param <R> The expected return type
	 * @param key The key to retrieve
	 * @return The stored value, or null if not found
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <R> R get(String key) {
		Object obj = getData().get(key);
		return (R)obj;
	}

	/**
	 * Removes a key-value pair from the context data.
	 * 
	 * @param <R> The expected return type of the removed value
	 * @param key The key to remove
	 * @return The removed value, or null if not found
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <R> R remove(String key) {
		Object obj = getData().remove(key);
		return (R)obj;
	}

	/**
	 * Returns all context data as a map.
	 * 
	 * @return Map containing all stored key-value pairs
	 */
	@Override
	public Map<String, Object> data() {
		return getData();
	}
	
	/**
	 * Returns the internal data map, creating it if necessary.
	 * 
	 * @return The data map for this context
	 */
	private Map<String, Object> getData() {
		if (data == null) {
			data = new HashMap<>();
		}
		return data;
	}
	
	/**
	 * Ends the walking process immediately.
	 * 
	 * <p>This method terminates the walking process, processes any exit level handlers,
	 * and triggers all registered end handlers. The context is marked as ended and
	 * no further processing will occur.</p>
	 */
	@Override
	public void end() {
		logger.trace("end of walking");
		this.ended = true;
		
		while(!objectWalkStack.isEmpty()) {
			var walk = objectWalkStack.pop();
			if(null != walk.exitLevelHandler) {
				walk.exitLevelHandler.accept(null);
			}
			
			if(null != walk.endHandler) {
				walk.endHandler.accept(this);
			}
		}
		
		endHandlers.forEach(Runnable::run);
	}
	
	/**
	 * Skips the current level and moves to the next item at the same level.
	 * 
	 * <p>This method removes the current level from the stack and processes
	 * any exit level handlers before moving to the next item.</p>
	 */
	@Override
	public synchronized void skipLevel() {
		logger.trace("skipLevel");
		var walk = objectWalkStack.pop();
		if(null != walk.exitLevelHandler) {
			walk.exitLevelHandler.accept(null);
		}
	}
	
	/**
	 * Processes the next item at the current level.
	 * 
	 * <p>This method retrieves the next item from the current level's iterator,
	 * determines the appropriate walker for the item's type, and processes all
	 * registered event handlers.</p>
	 * 
	 * @return true if there are more items to process at the current level, false otherwise
	 */
	synchronized boolean stepNext() {
		var currentLevel = currentLevel();
		var currentLevelIterator = currentLevel().iterator();
		logger.trace("stepNext ended:{}, paused:{}, hasNext:{}", ended(), paused(), currentLevelIterator.hasNext());
		
		if(!ended() && !paused() && currentLevelIterator.hasNext()) {
			var indicatedItem = currentLevelIterator.next();
			logger.trace("indicatedItem:{}", indicatedItem);
			var value = indicatedItem.value();
			
			AtomicReference<ObjectWalker> walkerReference = new AtomicReference<>();
			
			Class<?> type = null;
			if(null != value) {
				type = value.getClass();
				Optional.ofNullable(walkers().get(type)).ifPresent(walkerReference::set);
			}
			
			if(null != value && walkerReference.get() == null) {
				logger.debug("Not found walker for type '{}', value:{}", type, value);
			}
			
			// Fire events only if there is no walker for this object, or if the walker allows entry events
			var walker = walkerReference.get();
			boolean shouldFireEvent = (walker == null) || walker.fireEntryEvent();
			
			if (shouldFireEvent) {
				var eventsIterator = contextEvents().iterator();
				while(!ended() && !paused() && eventsIterator.hasNext()) {
					try {
						eventsIterator.next().accept(new ObjectWalkingEventImpl(indicatedItem, currentLevel, walkerReference));
					} catch (Exception e) {
						logger.error("Exception in event handler", e);
						// Re-throw the exception so it can be caught by the outer try-catch in next()
						throw e;
					}
				}
			}
			
			// Only delegate to walker if one exists and events haven't ended the process
			if(!ended() && !paused()) {
				if(null != walker) {
					logger.trace("relay walker: {}", walker);
					walker.handle(indicatedItem, this);
				}
			}
		}
		
		return !ended() && currentLevelIterator.hasNext();
	}
	
	/**
	 * Processes all items at the current level and manages level transitions.
	 * 
	 * <p>This method continuously processes items at the current level until either:
	 * - All items are processed (moves to parent level)
	 * - Walking is ended or paused
	 * - An error occurs</p>
	 */
	synchronized void next() {
		logger.trace("level {}", levelIndex());
		try {
			while(!ended() && !paused()) {
			logger.trace("Checking if stack is empty before continuing");
			checkEmpty();
				// Check state and manipulate stack atomically
				boolean shouldEndLevel = !ended() && !stepNext();
				if(shouldEndLevel) {
					logger.trace("end level {}", levelIndex());
					if(!objectWalkStack.isEmpty()) {
						ObjectWalk walk = null;
						synchronized (objectWalkStack) {
							if(!objectWalkStack.isEmpty()) {
								walk = objectWalkStack.pop();
							}
						}
						
						if(null != walk && null != walk.exitLevelHandler) {
							walk.exitLevelHandler.accept(null);
						}
					}
				}
			}
		} catch(Throwable ex) {
			cause = ex;
			end();
		}
	}
	
	/**
	 * Checks if the walking stack is empty and ends walking if so.
	 * 
	 * <p>This method ensures that walking ends when there are no more levels
	 * to process in the stack.</p>
	 */
	private void checkEmpty() {
		if(objectWalkStack.isEmpty()) {
			logger.trace("Ending walking due to empty stack");
			end();
		}
	}

	/**
	 * Returns the current depth level in the object hierarchy.
	 * 
	 * @return The current level index (0-based)
	 */
	@Override
	public int levelIndex() {
		return objectWalkStack.size();
	}
	
	/**
	 * Checks if the walking process has ended.
	 * 
	 * @return true if walking has ended, false otherwise
	 */
	@Override
	public boolean ended() {
		return ended;
	}
	
	/**
	 * Checks if the walking process is currently paused.
	 * 
	 * @return true if walking is paused, false otherwise
	 */
	@Override
	public boolean paused() {
		return paused;
	}
	
	/**
	 * Handles the specified ObjectWalk and begins processing.
	 * 
	 * <p>This method adds the walk to the stack, processes any next level handlers,
	 * and begins processing items at this level.</p>
	 * 
	 * @param objectWalk The walk object to process
	 */
	@Override
	public synchronized void handle(ObjectWalk objectWalk) {
		var walk = objectWalkStack.isEmpty() ? null : currentLevel();
		if(null != walk && null != walk.nextLevelHandler) {
			objectWalk.nextLevelHandler.accept(objectWalk);
		}
		
		if(null != enterLevelHandlers) {
			enterLevelHandlers.forEach(h -> h.accept(objectWalk));
		}
		
		objectWalkStack.add(objectWalk);
		next();
	}

	/**
	 * Returns the registered walkers mapped by their supported types.
	 * 
	 * @return BiMap containing walkers mapped by their Class types
	 */
	@Override
	public BiMap<Class<?>, ObjectWalker> walkers() {
		return walkers;
	}
	
	/**
	 * Returns the current walk level being processed.
	 * 
	 * @return ObjectWalk representing the current level
	 */
	@Override
	public ObjectWalk currentLevel() {
		return objectWalkStack.peek();
	}
	
	/**
	 * Returns the root object being walked.
	 * 
	 * @return The root object
	 */
	public Object object() {
		return object;
	}

	/**
	 * Registers a handler to be called when walking ends.
	 * 
	 * @param handler The handler to call when walking ends
	 * @return This context instance for method chaining
	 */
	@Override
	public ObjectWalkingContext endHandler(Consumer<ObjectWalkingContext> handler) {
		endHandlers.add(() -> handler.accept(this));
		return this;
	}

	/**
	 * Returns the context events for iteration.
	 * 
	 * @return Iterable of context event handlers
	 */
	Iterable<Consumer<ObjectWalkingEvent>> contextEvents() {
		return contextEvents;
	}
	
	/**
	 * Checks if the walking process failed with an error.
	 * 
	 * @return true if walking failed with an error, false otherwise
	 */
	@Override
	public boolean failed() {
		return null != cause;
	}
	
	/**
	 * Checks if the walking process completed successfully.
	 * 
	 * @return true if walking completed without errors, false otherwise
	 */
	@Override
	public boolean success() {
		return null == cause;
	}
	
	/**
	 * Returns the cause of failure if the walking process failed.
	 * 
	 * @return Throwable representing the error cause, or null if no error occurred
	 */
	@Override
	public Throwable cause() {
		return cause;
	}
	
	/**
	 * Pauses the walking process.
	 * 
	 * <p>When paused, the walking process will stop at the current position
	 * and wait for a resume() call before continuing.</p>
	 * 
	 * @return This context instance for method chaining
	 */
	@Override
	public synchronized ObjectWalkingContext pause() {
		logger.trace("pause");
		this.paused = true;
		return this;
	}
	
	/**
	 * Resumes the walking process after being paused.
	 * 
	 * <p>This method continues the walking process from where it was paused.
	 * If the context is not paused, this method has no effect.</p>
	 */
	@Override
	public synchronized void resume() {
		logger.trace("resume");
		this.paused = false;
		next();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).omitNullValues().add("ended", ended)
				.add("paused", paused).toString();
	}
}
