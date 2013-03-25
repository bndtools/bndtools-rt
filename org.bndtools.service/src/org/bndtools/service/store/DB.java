package org.bndtools.service.store;

/**
 * Provides access to the underlying document database. This is the entry point for the service, and acts as a {@link Store} factory.
 * 
 */
public interface DB {
	/**
	 * Creates a {@link Store} associated with a document type {@code <T>}. 
	 * The store contains documents from a named collection in the database. 
	 * If no collection exists for the provided name, one is created.
	 * 
	 * @param clazz The data model for the collection 
	 * @param collection Name of the collection in the database
	 * @return a Store linked to the collection name
	 * @throws Exception
	 */
	<T> Store<T> getStore(Class<T> clazz, String collection) throws Exception;

	/**
	 * Drops the current database. It is likely not allowed in most cases, 
	 * only for testing databases (e.g. database name following a naming convention)
	 *
	 */
	void drop() throws SecurityException;
}
