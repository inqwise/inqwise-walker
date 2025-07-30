package com.inqwise.walker;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;

/**
 * Walker implementation for Vert.x JsonArray instances.
 * 
 * <p>This walker traverses through all elements in a JsonArray, maintaining
 * indexed path information for each element. It can handle nested JsonObjects
 * and JsonArrays by delegating to appropriate walkers.</p>
 * 
 * <p>Path information is maintained in the format "array[0]", "array[1]" where
 * the index represents the position of the element in the array.</p>
 * 
 * @author Alex Misyuk
 */
public class JsonArrayWalker extends ObjectWalker {
	private static final Logger logger = LogManager.getLogger(JsonArrayWalker.class);

	/**
	 * Constructs a new JsonArrayWalker with no child walkers.
	 * 
	 * <p>This constructor creates a walker that will only process JsonArray instances
	 * without delegating to other walkers for nested objects.</p>
	 */
	public JsonArrayWalker() {
		super(null);
	}

	/**
	 * Constructs a new JsonArrayWalker with the specified child walkers.
	 * 
	 * <p>Child walkers are used to handle nested objects of specific types.
	 * For example, a JsonObjectWalker can be provided to handle nested objects.</p>
	 * 
	 * @param walkers Set of child walkers to use for nested objects
	 */
	public JsonArrayWalker(Set<ObjectWalker> walkers) {
		super(walkers);
	}
	
	/**
	 * Creates an iterator for all elements in the JsonArray.
	 * 
	 * <p>This method iterates through all elements in the JsonArray, creating
	 * IndicatedItem instances for each element with appropriate indexed path information.
	 * The path is constructed by appending the array index to the parent path.</p>
	 * 
	 * <p>If the array is empty, an empty iterator is returned.</p>
	 * 
	 * @param indicatedItem The current item containing the JsonArray
	 * @param context The walking context
	 * @return Iterator of IndicatedItem objects for each element in the JsonArray
	 */
	@Override
	protected Iterator<IndicatedItem> createObjectIterator(IndicatedItem indicatedItem, ObjectWalkingContext context){
		var json = ((JsonArray)indicatedItem.value());
		String path = Optional.ofNullable(indicatedItem.meta().get(Keys.PATH)).map(Object::toString).orElse(".");
		
		logger.debug("Creating iterator for JsonArray with {} elements", json.size());
		
		if(json.isEmpty()) {
			logger.debug("JsonArray is empty, returning empty iterator");
			return Collections.emptyIterator();
		}
		
		var iterator = IntStream.range(0, json.size())
		    .mapToObj(inx -> {
		    	var item = indicatedItem
			        .newSubItem(json.getValue(inx))
			        .put(Keys.PATH, path + "[" + inx + "]");
		    	logger.debug("Created IndicatedItem for index {}: {}", inx, item);
		    	return item;
		    })
		    .iterator();
		
		logger.debug("Returning iterator for JsonArray");
		return iterator;
	}
	
	/**
	 * Returns the JsonArray class that this walker handles.
	 * 
	 * @return JsonArray.class
	 */
	@Override
	protected Class<?> type() {
		return JsonArray.class;
	}
}