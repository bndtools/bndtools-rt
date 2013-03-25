/**
 * Abstraction service for a document database.
 *
 * <h3>Store type {@code <T>}:</h3>
 * <p>
 * {@code <T>} is the type of documents manipulated by a {@link org.bndtools.service.store.Store}
 *  and a {@link org.bndtools.service.store.Cursor} obtained from this store.
 * This type must obey some conditions:
 * <ul>
 * <li>{@code <T>} must have a public no-arg constructor
 * <li>Only public non-static fields of {@code <T>} can be stored in the database
 * <li>{@code <T>} must have a public non-static {@code _id} field
 * <li>If the {@code _id} field is left empty when inserting the object in the database, 
 * 			this field must be of type {@code String} or {@code byte[]} (in this case, the
 *      service assigns an ID to the document).
 * </ul>
 * 
 * <h3>Serialization process:</h3>
 * <p>
 * When inserting an object of type {@code <T>} in the database, all its public (non-static) fields 
 * are saved. If {@code <T>} -- or any type recursively contained in a public field of {@code <T>} -- 
 * does not have any public non-static field, the toString() representation of the object 
 * is stored instead. 
 * <p>
 * When a document is fetched from the database, every public field is set to the stored value. 
 * If an object is stored as its toString() representation, this object 
 * is re-constructed from the string representation, if possible.
 * <p>
 * If the saved object contains a reference to another object in a public non-static field, the 
 * referenced object is recursively treated as a data field. As a consequence, public non-static
 * fields of {@code <T>} must not contain a reference (direct or recursive) to "this", which
 * would prevent the insertion of the document in the database.
 */
package org.bndtools.service.store;
