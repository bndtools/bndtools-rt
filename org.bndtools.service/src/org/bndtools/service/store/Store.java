package org.bndtools.service.store;


/**
 * Collection of documents stored in a database. Obtained from {@link DB}.
 *
 * @param <T> See {@link org.bndtools.service.store}
 */
public interface Store<T> {
	
	/**
	 * Inserts a document in the database. This document must have a unique _id field 
	 * set, or the _id field must be a byte array or a string, in order for the service 
	 * to provide one.
	 * 
	 * @param document Document to insert in the database
	 * @return The original document, or null if the insertion failed
	 * @throws Exception
	 * 
	 */
	 T insert(T document) throws Exception;

	/**
	 * Updates a document in the database. If field names are provided, only those fields are updated.
	 * If no field name is provided, the whole document is replaced. The _id of the document cannot be
	 * changed, and must correspond to an existing document in the store.
	 * 
	 * @param document Document containing information to update in the database
	 * @param fields Update exclusively those fields
	 * @throws Exception
	 */
	void update(T document, String... fields) throws Exception;

	/**
	 * Upserts a document in the database, i.e., updates it if it exists, or inserts otherwise.
	 * 
	 * @param document
	 * @throws Exception
	 */
	void upsert(T document) throws Exception;

	/**
	 * Returns a {@link Cursor} containing all the documents in the store.
	 * @return a {@link Cursor} containing all the documents of the store.
	 * @throws Exception
	 */
	Cursor<T> all() throws Exception;

	/**
	 * Returns a {@link Cursor} containing all the documents matching a LDAP request in the collection.
	 * 
	 * @param ldap Format string following the LDAP syntax
	 * @param args Argument list for the format string
	 * @return a {@link Cursor} containing all documents matching the LDAP request
	 * @throws Exception
	 */
	Cursor<T> where(String ldap, Object... args) throws Exception;

	/**
	 * Returns a {@link Cursor} containing the document whose {@code _id} field matches the _id
	 * of the document passed as parameter. This method is mostly useful to selectively fetch a
	 * document to update in the database.
	 * 
	 * @param target Document containing the _id to match in the database
	 * @return a {@link Cursor} containing the document
	 * @throws Exception
	 */
	Cursor<T> find(T target) throws Exception;


	/**
	 * Sets the fields to return in the documents
	 * 
	 * @param keys Fields to return
	 * @return a {@link Cursor} configured to return only the specified fields in the documents 
	 */
	Cursor<T> select(String... keys);

	/**
	 * Returns a globally unique identifier consisting of 12 bytes.
	 *
	 * @return a globally unique identifier consisting of 12 bytes.
	 */
	byte[] uniqueId();

	
	/**
	 * Performs optimistic locking for updating a document in the database. This method is similar to 
	 * {@link Store#find(Object)}, except that if the element was modified in the database
	 * between the document retrieval and the document update, 
	 * a {@code ConcurrentModificationException} is thrown
	 * when {@link Cursor#update()} is called for this document. {@code Cursor#or(Object)} cannot be used
	 * on the cursor obtained from this method.  
	 * @param p
	 * @return
	 * @throws Exception
	 */
	Cursor<T> optimistic(T p) throws Exception;

	/**
	 * Drop this collection, databases normally do not allow this though and
	 * throw a {@link SecurityException}
	 * 
	 * @throws SecurityException
	 *             when not allowed
	 */
	void drop() throws SecurityException;

	/**
	 * Returns the number of documents in the store.
	 * 
	 * @return The number of documents in the store
	 */
	long count();
}
