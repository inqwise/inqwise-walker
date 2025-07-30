package com.inqwise.walker;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Abstract base class for object traversal and processing.
 * 
 * <p>This class provides a framework for walking through object hierarchies with event-driven processing.
 * It maintains a registry of specialized walkers for different object types and manages the traversal context.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Event-driven processing with customizable handlers</li>
 *   <li>Support for multiple object types through walker registry</li>
 *   <li>Flow control (pause, resume, end)</li>
 *   <li>Error handling with success/failure callbacks</li>
 * </ul>
 * 
 * @author Alex Misyuk
 */
public abstract class ObjectWalker {
	
	/**
	 * Constants for metadata keys used during object traversal.
	 */
	public static final class Keys {
		private Keys() {}
		/** Path information for the current object in the hierarchy */
		public static final String PATH = "path";
	}
	
	private Set<Consumer<ObjectWalkingEvent>> contextEvents;
	private final BiMap<Class<?>, ObjectWalker> walkers;
	private Consumer<ObjectWalkingContext> endHandler;
	private Consumer<ObjectWalkingContext> errorHandler;
	
	/**
	 * Constructs a new ObjectWalker with optional child walkers.
	 * 
	 * <p>Child walkers are registered by their type and will be used when encountering
	 * objects of that specific type during traversal.</p>
	 * 
	 * @param walkers Set of child walkers to register, or null for no child walkers
	 */
	public ObjectWalker(Set<ObjectWalker> walkers) {
		this.contextEvents = Sets.newConcurrentHashSet();
		this.walkers = HashBiMap.create();
		if(null != walkers) {
			walkers.forEach(walker -> this.walkers.put(walker.type(), walker));
		}
	}
	
	/**
	 * Starts walking through the specified object.
	 * 
	 * <p>This method creates a new walking context and begins the traversal process.
	 * The returned context can be used to control the walking process and access results.</p>
	 * 
	 * @param object The object to walk through
	 * @return ObjectWalkingContext that manages the walking process
	 */
	public ObjectWalkingContext handle(Object object) {
		return handle(new IndicatedItem(object));
	}
	
	/**
	 * Internal method to handle walking through an IndicatedItem.
	 * 
	 * @param indicatedItem The item to walk through
	 * @return ObjectWalkingContext that manages the walking process
	 */
	ObjectWalkingContext handle(IndicatedItem indicatedItem) {
		var context = new ObjectWalkingContextImpl(indicatedItem.value(), walkers(), contextEvents);
		handle(indicatedItem, context);
		return context;
	}
	
	/**
	 * Registers an event handler that will be called for each object during traversal.
	 * 
	 * <p>The handler receives an ObjectWalkingEvent containing information about the current
	 * object being processed, including its value, metadata, and context.</p>
	 * 
	 * @param event The event handler to register
	 * @return This ObjectWalker instance for method chaining
	 */
	public ObjectWalker handler(Consumer<ObjectWalkingEvent> event) {
		contextEvents.add(event);
		return this;
	}
	
	/**
	 * Returns the type of objects this walker can handle.
	 * 
	 * <p>This method must be implemented by subclasses to specify which object types
	 * they can process.</p>
	 * 
	 * @return The Class object representing the type this walker handles
	 */
	protected abstract Class<?> type();

	/**
	 * Determines if an event should be fired for the entry object when it has a dedicated walker.
	 * Subclasses can override this to change the default behavior.
	 * 
	 * @return {@code false} by default, meaning events for entry objects with walkers are skipped.
	 */
	protected boolean fireEntryEvent() {
		return false;
	}
	
	/**
	 * Returns an unmodifiable view of the registered walkers.
	 * 
	 * @return BiMap containing the registered walkers mapped by their types
	 */
	BiMap<Class<?>, ObjectWalker> walkers(){
		return Maps.unmodifiableBiMap(walkers);
	}

	
	/**
	 * Internal method that handles the walking process for a specific item.
	 * 
	 * <p>This method creates an ObjectWalk instance, sets up event handlers, and
	 * initiates the traversal process.</p>
	 * 
	 * @param indicatedItem The item to walk through
	 * @param context The walking context
	 * @return This ObjectWalker instance
	 */
	protected final ObjectWalker handle(IndicatedItem indicatedItem, ObjectWalkingContext context) {
		Preconditions.checkArgument(!context.ended(), "context is ended");
		ObjectWalk walk = ObjectWalk.walk(createObjectIterator(indicatedItem, context), context, indicatedItem);
		enter(walk);
		
		context.endHandler(ctx -> { 
			if(ctx.success()){
				if(null != endHandler) {
					endHandler.accept(ctx);
				}
			} else {
				if(null != errorHandler) {
					errorHandler.accept(ctx);
				}
			}
		});
		
		context.handle(walk);
		
		return this;
	}

	/**
	 * Hook method called when entering a new walk level.
	 * 
	 * <p>Subclasses can override this method to perform custom logic when
	 * entering a new level of object traversal.</p>
	 * 
	 * @param walk The ObjectWalk instance for the current level
	 */
	protected void enter(ObjectWalk walk) {
		
	}
	
	/**
	 * Creates an iterator for the objects to be walked through.
	 * 
	 * <p>This method must be implemented by subclasses to define how to iterate
	 * through the specific object type they handle.</p>
	 * 
	 * @param indicatedItem The current item being processed
	 * @param context The walking context
	 * @return Iterator of IndicatedItem objects to walk through
	 */
	protected abstract Iterator<IndicatedItem> createObjectIterator(IndicatedItem indicatedItem, ObjectWalkingContext context);
	
	/**
	 * Returns a string representation of this ObjectWalker.
	 * 
	 * @return String representation
	 */
	@Override
	public String toString() {
		return toStringHelper().toString();
	}
	
	/**
	 * Creates a ToStringHelper for building string representations.
	 * 
	 * <p>Subclasses can override this method to customize their string representation.</p>
	 * 
	 * @return ToStringHelper instance
	 */
	protected MoreObjects.ToStringHelper toStringHelper(){
		return MoreObjects.toStringHelper(this);
	}
	
	/**
	 * Registers a handler to be called when walking completes successfully.
	 * 
	 * @param handler The handler to call on successful completion
	 * @return This ObjectWalker instance for method chaining
	 */
	public ObjectWalker endHandler(Consumer<ObjectWalkingContext> handler) {
		this.endHandler = handler;
		return this;
	}
	
	/**
	 * Registers a handler to be called when walking fails with an error.
	 * 
	 * @param handler The handler to call on error
	 * @return This ObjectWalker instance for method chaining
	 */
	public ObjectWalker errorHandler(Consumer<ObjectWalkingContext> handler) {
		this.errorHandler = handler;
		return this;
	}
}
