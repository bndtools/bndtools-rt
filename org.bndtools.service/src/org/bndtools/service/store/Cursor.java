package org.bndtools.service.store;

import java.util.*;

/**
 * Used to build queries, iterate over the results, and modify documents from a {@link Store}.
 *
  *<p>
 * Four categories of methods are accessible for a Cursor: Update, Selection, Projection, and Endpoint
 * <ul>
 * <li>An update action modifies documents already present in the store.
 * <li>A selection action allows to fetch a subset of documents from the store, depending on some criteria
 * <li>A projection action controls the amount of information returned for each document
 * <li>An endpoint method acts on the cursor, and actually performs the work related to the database
 * </ul>
 * 
 * <p>
 * The first three categories follow the builder pattern, each method adds an additional information
 * to the cursor and return the cursor itself. As a result, it is possible to chain building instruction.
 * Once the cursor is ready, an endpoint method may be used on it to actually perform work on documents
 * from the store.
 * 
 * <p>
 * {@link Cursor#limit(int)}, {@link Cursor#skip(int)}, {@link Cursor#select(String...)}, {@link Cursor#slice(String, int)},
 * and sorting operations have no effect on update actions.
 * 
 * @param <T> See {@link org.bndtools.service.store}
 */
public interface Cursor<T> extends Iterable<T> {
	
	/**
	 * Describe an operation to perform on each document in a {@link Cursor} (see {@link Cursor#visit(Visitor)}).
	 *
	 * @param <T> See {@link org.bndtools.service.store}
	 */
	public interface Visitor<T> {
		/** 
		 * Operation to perform on an element.
		 * 
		 * @param t Document on which to perform the operation
		 * @return false to stop the batch processing
		 * @throws Exception
		 */
		boolean visit(T t) throws Exception;
	}

	/**
	 * Limits the documents contained in the cursor to documents matching a LDAP request (Selection action).
	 * 
	 * @category Selection
	 * @param ldap Format string following the LDAP syntax
	 * @param args Argument list for the format string
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> where(String ldap, Object... args) throws Exception;

	/**
	 * Adds the specified document to the cursor (Selection action). This method is mostly useful to selectively update 
	 * documents previously fetched from the database (see {@link Store#find(Object)}).
	 * 
	 * @category Selection
	 * @param t Document to add to the cursor
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> or(T t) throws Exception;

	/**
	 * Sets the fields to return (Projection action).
	 * 
	 * @category Projection
	 * @param keys Fields to return
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> select(String... keys) throws Exception;

	/**
	 * Limits the number of element from an array field to return (Projection action). A positive count number means the first
	 * count elements, whereas a negative count means the last count elements from the array.
	 * 
	 * @category Projection
	 * @param arrayField Array field in the document
	 * @param count Number of elements to return from this array
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> slice(String arrayField, int count) throws Exception;

	/**
	 * Limits the number of documents contained in the cursor to the first {@code limit} elements (Selection action).
	 * 
	 * @category Selection
	 * @param limit Number of documents to keep in the cursor
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> limit(int limit) throws Exception;

	/**
	 * Skips the first {@code skip} elements from the cursor (Selection action).
	 * 
	 * @category Selection 
	 * @param skip Number of documents from the cursor to skip
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> skip(int skip) throws Exception;

	/**
	 * Orders the documents in the cursor in increasing value for the specified field (Selection action).
	 * 
	 * @category Selection
	 * @param field Field on which to order the documents
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> ascending(String field) throws Exception;

	/**
	 * Orders the documents in the cursor in decreasing value for the specified field (Selection action).
	 * 
	 * @category Selection
	 * @param field Field on which to order the documents
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> descending(String field) throws Exception;

	/**
	 * Sets the field to the specified value for each document in the cursor (Update action).
	 * 
	 * @category Update
	 * @param field
	 * @param value
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> set(String field, Object value) throws Exception;

	/**
	 * Removes the specified field from the documents in the cursor (Update action).
	 * 
	 * @category Update
	 * @param field Field to remove from the documents
	 * @return this (Builder Pattern)
	 */
	Cursor<T> unset(String field) throws Exception;

	/**
	 * Adds values to an array field in the documents in the cursor. OBS: only one {@code Cursor#append(String, Object...)} call per field 
	 * can be used per request (otherwise an {@link UnsupportedOperationException} is thrown). (Update action).
	 * 
	 * @category Update
	 * @param arrayField Array field in the document
	 * @param value Value(s) to add to the array
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> append(String arrayField, Object... value) throws Exception;

	/**
	 * Removes all the occurrences of the given value(s) from the specified array field. OBS: only one {@code Cursor#pull(String, Object...)} call per field 
	 * can be used per request (otherwise an {@link UnsupportedOperationException} is thrown). (Update action).
	 * 
	 * @category Update
	 * @param arrayField Array field in the document
	 * @param value Value to remove from the array
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> pull(String arrayField, Object... value) throws Exception;

	/**
	 * Increase the value contained in a filed by a given number. OBS: only one {@code Cursor#inc(String, Object)} call per field 
	 * can be used per request (otherwise an {@link UnsupportedOperationException} is thrown) (Update action)
	 * 
	 * @category Update
	 * @param field Field to modify
	 * @param value Increase step
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> inc(String field, Object value) throws Exception;

	/**
	 * Selects only documents for which the specified field is one of the specified values (Select action)
	 * 
	 * @category Selection
	 * @param field Field in the document
	 * @param values 
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> in(String field, Object... values) throws Exception;

	/**
	 * Selects only documents for which the specified field is one of the specified values (Select action)
	 * 
	 * @category Selection
	 * @param field Field in the document
	 * @param values 
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> in(String field, Collection< ? > values) throws Exception;

	/**
	 * Selects only documents for which the specified field is equal to the specified value (Select action)
	 * 
	 * @category Selection
	 * @param field Field in the document
	 * @param value Value to which the field must be equal to
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> eq(String field, Object value);

	/**
	 * Selects only documents for which the specified field is strictly greater than the specified value (Select action)
	 * 
	 * @category Selection
	 * @param field Field in the document
	 * @param value Strict lower bound on the field value
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> gt(String field, Object value);

	/**
	 * Selects only documents for which the specified field is strictly less than the specified value (Select action)
	 * 
	 * @category Selection
	 * @param field Field in the document
	 * @param value Strict upper bound on the field value
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> lt(String field, Object value);

	/**
	 * Selects only documents for which the specified field is greater than or equal to the specified value (Select action)
	 * 
	 * @category Selection
	 * @param field Field in the document
	 * @param value lower bound on the field value
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> gte(String field, Object value);

	/**
	 * Selects only documents for which the specified field is less than or equal to the specified value (Select action)
	 * 
	 * @category Selection
	 * @param field Field in the document
	 * @param value upper bound on the field value
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> lte(String field, Object value);

	/**
	 * Adds the word to the {@code keywords} array field of the documents in the cursor (Update action). 
	 * If the documents do not have such a field, it is automatically created. This method is best used 
	 * with cursors containing only one document (e.g. created with {@link Store#find(Object)}). Common
	 * English words in can be automatically skipped (e.g. is, am, and, etc.), depending on the implementation.
	 * 
	 * @param word Keyword to add to the keywords array in the document
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> word(String word) throws Exception;
	
	/**
	 * Adds words contained in the text to the keywords field of the documents in the cursor (Update action).
	 * See {@link Cursor#word(String)}.
	 * 
	 * @category Update
	 * @param text Text containing keywords to add to the document
	 * @return this (Builder Pattern)
	 * @throws Exception
	 */
	Cursor<T> text(String text) throws Exception;

	/**
	 * Selects documents for which the {@code keywords} field contains all the words in the query string.
	 * <p>
	 * It is possible to query directly on another field of the documents by adding template instruction(s),
	 * of the form {@code foo:bar} in the query string. In that case, the corresponding template 
	 * (i.e. key is {@code foo} in the templates) is used as a format string (following the LDAP syntax), 
	 * for which {@code bar} is passed as a format string argument. If no template is found with key 
	 * {@code foo}, the template instruction is ignored by the query.
	 * <p>
	 * If several words (including template instructions) are present in the query string, only documents 
	 * that match <em>all</em> of them are selected by the query. 
	 * <p>
	 * A word can begin with a dash (e.g. {@code -foo}, in which case only documents that do not contain
	 * this word in their {@code keywords} field are selected.
	 * @category Selection
	 * @param query Query string, which may have several words and/or template instructions
	 * @param templates Map in which the key is used to identify the template in the query (left part
	 * @return
	 * @throws Exception
	 */
	Cursor<T> query(String query, Map<String,String> templates) throws Exception;

	/**
	 * Returns the first element of the cursor.
	 * 
	 * @category Endpoint
	 * @return The first element of the cursor, or null if there is none.
	 * @throws Exception
	 */
	T first() throws Exception;

	/**
	 * Returns the first element of the cursor if and only if there is only one 
	 * element in the cursor. Otherwise, throws an exception.
	 * 
	 * @category Endpoint
	 * @return First element of the cursor
	 * @throws Exception if there is not exactly one document in the cursor
	 */
	T one() throws Exception;

	/**
	 * Returns an {@link Iterator} over all the documents in the cursor.
	 * 
	 * @category Endpoint
	 * @return Iterator over the documents in the cursor
	 */
	@Override
	Iterator<T> iterator();

	/**
	 * Perform the operation described in {@link Visitor#visit(Object)} on each element of the cursor.
	 * 
	 * @category Endpoint
	 * @param visitor
	 * @return False if and only if the batch processing was interrupted by one of the individual operations.
	 * @throws Exception
	 */
	boolean visit(Visitor<T> visitor) throws Exception;

	/**
	 * Removes the documents in the cursor from the database. OBS: {@link Cursor#skip(int)} and {@link Cursor#limit(int)} 
	 * have no influence on the number of documents being removed.
	 * 
	 * @category Endpoint
	 * @return The number of documents removed from the database
	 * @throws Exception
	 */
	int remove() throws Exception;

	/**
	 * Returns the number of documents in the cursor. OBS: {@link Cursor#limit(int)} and {@link Cursor#skip(int)}
	 * are not taken into account in the result.
	 * 
	 * @category Endpoint
	 * @return The number of documents in the cursor
	 * @throws Exception
	 */
	int count() throws Exception;

	/**
	 * Lists every distinct value from a given field for documents in the cursor.  
	 * OBS: {@link Cursor#skip(int)} and {@link Cursor#limit(int)} have no influence on this request. 
	 * 
	 * @category Endpoint
	 * @param field 
	 * @return Set of distinct values for a field across document in the cursor
	 * @throws Exception
	 */
	List< ? > distinct(String field) throws Exception;

	/**
	 * Lists every document in the cursor.
	 * 
	 * @category Endpoint
	 * @return List of documents in the cursor
	 */
	List<T> collect();

	/**
	 * Tests if the cursor does not hold any document.
	 * 
	 * @category Endpoint
	 * @return true if the cursor is empty, false otherwise.
	 * @throws Exception
	 */
	boolean isEmpty() throws Exception;

	/**
	 * Commits the update actions in the database.
	 * 
	 * @category Endpoint
	 * @return The number of documents updated by this call
	 * @throws Exception
	 */
	int update() throws Exception;
}
