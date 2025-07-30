package com.inqwise.walker;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Iterators;
import com.inqwise.difference.Differences;

/**
 * Walker implementation for Differences objects from the inqwise-difference library.
 * 
 * <p>This walker traverses through difference structures, which represent changes
 * between two objects. It delegates the iteration to the underlying Differences
 * object's iterator and wraps each difference item in an IndicatedItem.</p>
 * 
 * <p>This walker is designed to work with the inqwise-difference library for
 * object comparison and diffing operations.</p>
 * 
 * @author Alex Misyuk
 */
public class DifferencesWalker extends ObjectWalker {
	
	/**
	 * Constructs a new DifferencesWalker with no child walkers.
	 * 
	 * <p>This constructor creates a walker that will only process Differences instances
	 * without delegating to other walkers for nested objects.</p>
	 */
	public DifferencesWalker() {
		super(null);
	}
	
	/**
	 * Constructs a new DifferencesWalker with the specified child walkers.
	 * 
	 * <p>Child walkers are used to handle nested objects within the difference
	 * structure that may be of specific types.</p>
	 * 
	 * @param walkers Set of child walkers to use for nested objects
	 */
	public DifferencesWalker(Set<ObjectWalker> walkers) {
		super(walkers);
	}
	
	/**
	 * Returns the Differences class that this walker handles.
	 * 
	 * @return Differences.class
	 */
	@Override
	public final Class<?> type() {
		return Differences.class;
	}

	/**
	 * Creates an iterator for all differences in the Differences object.
	 * 
	 * <p>This method uses the underlying Differences object's iterator and transforms
	 * each difference item into an IndicatedItem using the newSubItem method.
	 * This allows the walking framework to process each difference with proper
	 * metadata and context.</p>
	 * 
	 * @param indicatedItem The current item containing the Differences object
	 * @param context The walking context
	 * @return Iterator of IndicatedItem objects for each difference in the Differences object
	 */
	@Override
	protected Iterator<IndicatedItem> createObjectIterator(IndicatedItem indicatedItem, ObjectWalkingContext context) {
		return Iterators.transform(((Differences)indicatedItem.value()).iterator(),indicatedItem::newSubItem );
	}	
}