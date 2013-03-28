package org.bndtools.rt.store.mongo;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.bndtools.service.store.Cursor;
import org.bndtools.service.store.Store;
import org.bson.types.*;

import aQute.lib.base64.*;
import aQute.lib.hex.*;

import com.mongodb.*;
import com.mongodb.gridfs.*;

public class MongoStoreImpl<T> implements Store<T> {
	final static Pattern	BINARY_PATTERN	= Pattern
													.compile("\\[h((?:[a-fA-f0-9][a-fA-f0-9])+)]|\\[b([a-zA-Z0-9+/]+={0,2})]");
	final static Pattern	SIMPLE_EXPR		= Pattern.compile("([^=><~*]+)\\s*(=|<=|>=|>|<|~=)\\s*([^\\s]+)");
	final MongoDBImpl		handler;
	final Class<T>			type;
	final DBCollection		collection;
	GridFS					gridfs;
	final Field				_id;
	final ArrayList<Field>	fields 			= new ArrayList<Field>();
	final MongoCodec		mcnv			= new MongoCodec(this);
	
	final WeakHashMap<T, Integer> versions = new WeakHashMap<T, Integer>();
	/**
	 *  
	 * @param handler: A database connection
	 * @param type: The data model for the collection, must contain an _id field
	 * @param collection
	 * @throws Exception
	 */
	public MongoStoreImpl(MongoDBImpl handler, Class<T> type, DBCollection collection) throws Exception {
		this.handler = handler;
		this.collection = collection;
		//this.type = type;
		this.type = type;
		Field tmp = null;
		for (Field f : type.getFields()) {
			if(!Modifier.isStatic(f.getModifiers())) {
				if (f.getName().equals("_id"))
					tmp = f;
				fields.add(f);
			}
		}
		if (tmp == null)
			throw new IllegalArgumentException("No _id field, required");

		_id = tmp;
	}

	/**
	 * Insert a document in the database.
	 * This document must have an _id field set, or this field must be a byte 
	 * array or a string.
	 * 
	 * @param document
	 * @return T
	 * @throws Exception
	 * 
	 */
	public T insert(T document) throws Exception {
		Object key = _id.get(document);
		if (key == null) {
			attributeID(document);
		}
		DBObject o = (DBObject) mcnv.toMongo(document);
		o.put("__version", 0);
		try {
			WriteResult result = collection.insert(o);
			CommandResult lastError = result.getLastError();

			if (lastError != null) {
				Integer code = (Integer) lastError.get("code");
				if (code != null && code == 11000)
					return null; // insert failed!

				error(result);
			}
			versions.put(document, 0);
			return document;
		}
		catch (MongoException.DuplicateKey e) {
			return null;
		}
	}

	/**
	 * Manually attribute an ID to a document, to ensure key consistency across documents
	 * @param document Document for which an ID is needed
	 * @throws Exception 
	 */
	private void attributeID(T document) throws Exception {
		if (_id.getType() == byte[].class)
			_id.set(document, ObjectId.get().toByteArray());
		else if (_id.getType() == String.class)
			_id.set(document, ObjectId.get().toString());
		else
			throw new IllegalArgumentException(
					"Has no _id set and id cannot be created because it is not a byte[] or a String");
	}


	public void update(T document, String... fieldNames) throws Exception {
		MongoCursorImpl<T> cursor = find(document);
		if(cursor.count() == 0) {
			throw new IllegalArgumentException("No corresponding document in the database");
		}
		
		if (fieldNames == null || fieldNames.length == 0) {
			for (Field field : fields) {
				if(field.getName() != "_id")
					cursor.set(field.getName(), field.get(document));
			}
		} else {
			Class< ? > c = document.getClass();
			for (String fieldName : fieldNames) {
				Field field = c.getField(fieldName);
				cursor.set(fieldName, field.get(document));
			}
		}
		cursor.update();
	}

	public void upsert(T document) throws Exception {
		if(_id.get(document) == null || find(document).count() == 0) {
			insert(document);
		} else {
			update(document);
		}
		if(find(document).count() == 0) {
			
		}
	}

	public MongoCursorImpl<T> all() throws Exception {
		return new MongoCursorImpl<T>(this).where("_id=*");
	}

	public MongoCursorImpl<T> where(String ldap, Object... args) throws Exception {
		return new MongoCursorImpl<T>(this).where(ldap, args);
	}

	public MongoCursorImpl<T> find(T select) throws Exception {
		return new MongoCursorImpl<T>(this, select);
	}

	
	void error(WriteResult result) {
		if (result.getLastError() != null && result.getError() != null)
			throw new RuntimeException(result.getError());

	}

	/**
	 * Create a filter out of an LDAP expression.
	 * 
	 * @param where
	 * @param ldap
	 * @return
	 * @throws Exception
	 */
	DBObject filter(String ldap, Object... args) throws Exception {

		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof byte[])
				args[i] = "[h" + Hex.toHexString((byte[]) args[i]) + "]";

			// TODO more conversions?
		}
		String formatted = String.format(ldap, args);
		if (!formatted.startsWith("("))
			formatted = "(" + formatted + ")";

		Reader r = new StringReader(formatted);
		return expr(r, r.read());
	}

	private DBObject expr(Reader ldap, int c) throws Exception {
		while (Character.isWhitespace(c))
			c = ldap.read();

		assert c == '(';
		DBObject query = new BasicDBObject();

		do {
			c = ldap.read();
		} while (Character.isWhitespace(c));

		switch (c) {
			case '&' : {
				List<DBObject> exprs = exprs(ldap);
				query.put("$and", exprs);
				break;
			}

			case '|' : {
				List<DBObject> exprs = exprs(ldap);
				query.put("$or", exprs);
				break;
			}

			case '!' : {
				List<DBObject> exprs = exprs(ldap);
				query.put("$nor", exprs);
				break;
			}

			case -1 :
				throw new EOFException();

			default :
				while (Character.isWhitespace(c))
					c = ldap.read();

				StringBuilder sb = new StringBuilder();
				boolean regex = false;

				while (true) {
					if (c < 0)
						throw new EOFException();

					if (c == '\\') {
						c = ldap.read();
						if (c < 0)
							throw new EOFException();
					} else if (c == '*') {
						regex = true;
						sb.append(".");
					} else if (c == ')')
						break;

					sb.append((char) c);
					c = ldap.read();
				}
				Matcher m = SIMPLE_EXPR.matcher(sb);
				if (!m.matches())
					throw new IllegalArgumentException("Not a valid LDAP expression " + sb);

				String key = m.group(1);
				String op = m.group(2);
				String value = m.group(3);

				if (op.equals("=")) {
					if (".*".equals(value))
						query.put(key, new BasicDBObject("$exists", true));
					else if ("[]".equals(value)) {
						query.put(key, Collections.EMPTY_LIST);
					} else {
						Matcher matcher = BINARY_PATTERN.matcher(value);
						if (matcher.matches()) {
							if (matcher.group(2) != null) // [b matched
								query.put(key, Base64.decodeBase64(matcher.group(2)));
							else
								// [h matched
								query.put(key, Hex.toByteArray(matcher.group(1)));
						} else if (regex) {
							query.put(key, new BasicDBObject("$regex", "^" + value));
							// TODO ensure valid regex for value
						} else
							query.put(key, fromBson(key, value));
					}
				} else if (op.equals(">"))
					query.put(key, new BasicDBObject("$gt", fromBson(key, value)));
				else if (op.equals(">="))
					query.put(key, new BasicDBObject("$gte", fromBson(key, value)));
				else if (op.equals("<"))
					query.put(key, new BasicDBObject("$lt", fromBson(key, value)));
				else if (op.equals("<="))
					query.put(key, new BasicDBObject("$lte", fromBson(key, value)));
				else if (op.equals("~="))
					query.put(key, new BasicDBObject("$regex", fromBson(key, value)).append("$options", "i"));
				// TODO ensure valid regex for value
				else
					throw new IllegalArgumentException("Unknown operator " + op);

				// TODO optimize by recognizing patterns that map to better
				// operators
		}
		return query;
	}

	private Object fromBson(String key, String value) throws Exception {
		Object result = value;
		if ("null".equals(result)) {
			result = null;
		} else if ("true".equals(result)) {
			result = true;
		} else if ("false".equals(result)) {
			result = false;
		}

		try {
			Field field = type.getField(key);
			if (field.getType() == byte[].class) {
				if (value.matches("([0-9a-fA-F][0-9a-fA-F])+"))
					result = Hex.toByteArray(value);
				else if (value.matches("(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)"))
					result = Base64.decodeBase64(value);
			}
			// if (result == null)
			else
				result = mcnv.converter.convert(field.getGenericType(), result);
		}
		catch (Exception e) {
			// ignore
		}

		result = mcnv.toMongo(result);
		if (result == null)
			return result;

		// In a query, we do not specify the
		// collection/array levels
		if (result instanceof Iterable) {
			return ((Iterable< ? >) result).iterator().next();
		} else if (result.getClass() != byte[].class && result.getClass().isArray())
			return Array.getLength(result) > 0 ? Array.get(result, 0) : result;
		else
			return result;
	}

	private List<DBObject> exprs(Reader ldap) throws Exception {
		int c;
		do {
			c = ldap.read();
		} while (Character.isWhitespace(c));

		List<DBObject> list = new ArrayList<DBObject>();
		while (c == '(') {
			list.add(expr(ldap, c));

			// read ( for another or ) for close
			c = ldap.read();
		}
		return list;
	}

	/**
	 * Create a minimal {@link BasicDBObject} sufficient to uniquely
	 * identify a document in the database.
	 * @param t
	 * @return
	 * @throws Exception
	 */
	BasicDBObject filter(T t) throws Exception {
		BasicDBObject or = new BasicDBObject();
		Object id = _id.get(t);
		if (id != null)
			or.append("_id", id);
		else {
			throw new IllegalArgumentException("Document has no _id set");
		}
		return or;
	}

	// pl: only used in cursorImpl
	boolean checkField(String field, Object value) throws Exception {
		Field f = type.getField(field);
		return f != null;
	}

	public MongoCursorImpl<T> select(String... keys) {
		return new MongoCursorImpl<T>(this).select(keys);
	}

	/**
	 * Returns a globally unique identifier consisting of 12 bytes.
	 */
	public byte[] uniqueId() {
		return new ObjectId().toByteArray();
	}

	GridFS getGridFs() {
		if (gridfs == null) {
			this.gridfs = new GridFS(collection.getDB(), collection.getName());

		}
		return gridfs;
	}

	public void drop() {
		handler.checkTest();
		collection.drop();
	}

	/**
	 * TODO implement optimistic locking
	 */
	public Cursor<T> optimistic(T target) throws Exception {
		return new MongoCursorImpl<T>(this, target, true);
	}

	@Override
	public long count() {
		return collection.count();
	}

}
