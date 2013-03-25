package org.bndtools.rt.store.mongo;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.bndtools.service.store.Cursor;

import aQute.lib.converter.*;

import com.mongodb.*;

public class MongoCursorImpl<T> implements Cursor<T> {
	static Pattern	QUERY	= Pattern.compile("(-)?(\\w+):([^()=><]+)");

	enum Ops {
		INC, SET, UNSET, ADD, REMOVE, APPEND;
	}

	static DBObject			EMPTY		= new BasicDBObject();
	static Converter		converter	= new Converter();
	final MongoStoreImpl<T>	store;
	DBObject				where;
	DBObject				select;
	DBObject				sort;
	DBObject				update;
	int						skip;
	int						limit;
	boolean					optimistic;
	T						optimisticDocument;
	
	public MongoCursorImpl(MongoStoreImpl<T> store) {
		this.store = store;
		optimistic = false;
	}

	public MongoCursorImpl(MongoStoreImpl<T> store, T target) throws Exception {
		this(store);
		where = store.filter(target);
	}

	public MongoCursorImpl(MongoStoreImpl<T> store, T target, boolean optimistic) throws Exception {
		this(store, target);
		this.optimistic = true;
		optimisticDocument = target;
	}

	public MongoCursorImpl<T> where(String ldap, Object... args) throws Exception {
		if (ldap == null)
			return this;

		combine("$and", store.filter(ldap, args));
		return this;
	}

	public MongoCursorImpl<T> or(T t) throws Exception {
		if(optimistic) {
			throw new IllegalStateException("Cannot be used while doing optimistc locking");
		}
		combine("$or", store.filter(t));
		return this;
	}

	public MongoCursorImpl<T> select(String... keys) {
		if (select == null)
			select = new BasicDBObject();
		for (String key : keys)
			select.put(key, 1);
		return this;
	}

	public MongoCursorImpl<T> slice(String key, int count) {
		if (select == null)
			select = new BasicDBObject();
		select.put(key, new BasicDBObject("$slice", count));
		return this;
	}

	public MongoCursorImpl<T> limit(int limit) {
		if(limit < 0) {
			throw new IllegalArgumentException("Limit should be a positive integer");
		}
		this.limit = limit;
		return this;
	}

	public MongoCursorImpl<T> skip(int skip) {
		this.skip = skip;
		return this;
	}

	public MongoCursorImpl<T> ascending(String field) {
		return sort(field, 1);
	}

	public MongoCursorImpl<T> descending(String field) {
		return sort(field, -1);
	}

	public T first() {
		limit = 1;
		Iterator<T> one = iterator();
		if (one.hasNext())
			return one.next();
		else
			return null;
	}

	public Iterator<T> iterator() {
		final DBCursor cursor = getDBCursor();

		return new Iterator<T>() {

			public boolean hasNext() {
				return cursor.hasNext();
			}

			@SuppressWarnings("unchecked")
			public T next() {
				DBObject object = cursor.next();
				// object contains the document + the version field
				int version = (Integer) object.get("__version");
				object.removeField("__version");
				object.removeField("__keywords");
				try {
					T document = (T) store.mcnv.fromMongo(store.type, object);
					store.versions.put(document, version);
					return document;
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}

			public void remove() {
				cursor.remove();
			}
		};
	}

	public List< ? > distinct(String field) throws Exception {
		assert skip == 0;
		assert limit == 0;
		assert select == null;

		Type toto = store.type.getField(field).getGenericType();
		Class< ? > to = store.type.getField(field).getType();
		if(to.isArray()) {
			toto = to.getComponentType();
		} else if( Collection.class.isAssignableFrom(to) && toto instanceof ParameterizedType) {
			toto = ((ParameterizedType)toto).getActualTypeArguments()[0];
		} else if(GenericArrayType.class.isAssignableFrom(to)) {
			toto = ((GenericArrayType)toto).getGenericComponentType();
		}
		
		List< ? > list;

		// Do we have a sub selection? Then use the filter
		// otherwise use the call without where clause
		if (where == null)
			list = store.collection.distinct(field);
		else
			list = store.collection.distinct(field, where);

		List<Object> result = new ArrayList<Object>(list.size());
		for (Object o : list) {
			result.add(converter.convert(toto, o));
		}
		return result;
	}

	private DBCursor getDBCursor() {
		if(select != null)
			select.put("__version", 1); // Forced retrieval of __version
		
		final DBCursor cursor = store.collection.find(where, select);
		if (limit != 0)
			cursor.limit(limit);
		else
			cursor.limit(100);

		if (skip != 0)
			cursor.skip(skip);
		if (sort != null) {
			cursor.sort(sort);
		}
		// System.out.println(where);
		return cursor;
	}

	public int remove() {
		WriteResult result = store.collection.remove(where);
		store.error(result);
		return result.getN();
	}

	private MongoCursorImpl<T> sort(String field, int i) {
		if (sort == null)
			sort = new BasicDBObject();
		sort.put(field, i);
		return this;
	}

	public int count() {
		DBCursor cursor = getDBCursor();
		return cursor.count();
	}

	public T one() throws Exception {
		limit = 2;
		T returnObject;
		Iterator<T> it = iterator();
		// At least one
		if (it.hasNext())
			returnObject = it.next();
		else
			throw new Exception("Empty cursor");
		
		// At most one
		if (it.hasNext()) {
			throw new Exception("More than one element in the cursor");
		} else {
			return returnObject;
		}
	}

	void combine(String type, DBObject filter) {
		if (where == null) {
			where = filter;
			return;
		}
		where = new BasicDBObject(type, Arrays.asList(where, filter));
	}

	public MongoCursorImpl<T> set(String field, Object value) throws Exception {
		combineUpdate(field, "$set", store.mcnv.toMongo(value));
		return this;
	}

	public MongoCursorImpl<T> unset(String field) throws Exception {
		combineUpdate(field, "$unset", null);
		return this;
	}

	MongoCursorImpl<T> appendAll(String field, Set<String> set) throws Exception {
		Object test = store.mcnv.toMongo(set);
		combineUpdate(field, "$pushAll", store.mcnv.toMongo(set));
		return this;
	}
	public MongoCursorImpl<T> append(String field, Object... value) throws Exception {
		if(update != null && update.get("$pushAll") != null && ((DBObject)update.get("$pushAll")).get(field) != null) {
			throw new UnsupportedOperationException("Cannot chain append() operations on the same field");
		}
		combineUpdate(field, "$pushAll", store.mcnv.toMongo(value));
		return this;
	}

	@Override
	public Cursor<T> pull(String field, Object... value) throws Exception {
		if(update != null && update.get("$pullAll") != null && ((DBObject)update.get("$pullAll")).get(field) != null) {
			throw new UnsupportedOperationException("Cannot chain pull() operations on the same field");
		}
		combineUpdate(field, "$pullAll", store.mcnv.toMongo(value));
		return this;
	}

	public MongoCursorImpl<T> inc(String field, Object value) throws Exception {
		if(update != null && update.get("$inc") != null && ((DBObject)update.get("$inc")).get(field ) != null) {
			throw new UnsupportedOperationException("Cannot chain inc() operations on the same field");
		}
		combineUpdate(field, "$inc", store.mcnv.toMongo(value));
		return this;
	}

	public boolean isEmpty() {
		return count() == 0;
	}

	private void combineUpdate(String field, String op, Object value) throws Exception {
		if (update == null)
			update = new BasicDBObject();

		DBObject o = (DBObject) update.get(op);
		if (o == null)
			update.put(op, o = new BasicDBObject());

		assert store.checkField(field, value);
		if (value instanceof Enum)
			value = value.toString();
		o.put(field, value);
	}

	public int update() throws Exception {
		if(optimistic) {
			where.put("__version", store.mcnv.toMongo(store.versions.get(optimisticDocument)));
		}
		inc("__version", 1);
		

		WriteResult result = store.collection.update(where, update, false, !optimistic);
		store.error(result);
		if(optimistic && result.getN() == 0) {
			throw new ConcurrentModificationException("Document was modified after obtention");
		}
		return result.getN();
	}

	public MongoCursorImpl<T> in(String field, Object... values) throws Exception {
		return in(field, Arrays.asList(values));
	}

	public MongoCursorImpl<T> in(String field, Collection< ? > values) throws Exception {
		if (where == null)
			where = new BasicDBObject();

		BasicDBList in = new BasicDBList();
		Field f = store.type.getField(field); // Verification that field name belongs to store type
		for (Object value : values) {

			// TODO need to consider collection fields ...

			in.add(store.mcnv.toMongo(value));
		}
		where.put(f.getName(), new BasicDBObject("$in", in));
		return this;
	}

	@Override
	public Cursor<T> eq(String key, Object value) {
		if (where == null)
			where = new BasicDBObject();
		where.put(key, value);
		return this;
	}

	@Override
	public Cursor<T> gt(String key, Object value) {
		if (where == null)
			where = new BasicDBObject();

		BasicDBObject vpart = (BasicDBObject) (where.get(key) == null ? new BasicDBObject() : where.get(key));
		vpart.put("$gt", value);
		where.put(key, vpart);
		return this;
	}

	@Override
	public MongoCursorImpl<T> lt(String key, Object value) {
		if (where == null)
			where = new BasicDBObject();

		BasicDBObject vpart = (BasicDBObject) (where.get(key) == null ? new BasicDBObject() : where.get(key));
		vpart.put("$lt", value);
		where.put(key, vpart);
		return this;
	}

	@Override
	public MongoCursorImpl<T> gte(String key, Object value) {
		if (where == null)
			where = new BasicDBObject();

		BasicDBObject vpart = (BasicDBObject) (where.get(key) == null ? new BasicDBObject() : where.get(key));
		vpart.put("$gte", value);
		where.put(key, vpart);
		return this;
	}

	@Override
	public MongoCursorImpl<T> lte(String key, Object value) {
		if (where == null)
			where = new BasicDBObject();

		BasicDBObject vpart = (BasicDBObject) (where.get(key) == null ? new BasicDBObject() : where.get(key));
		vpart.put("$lte", value);
		where.put(key, vpart);
		return this;
	}

	@Override
	public List<T> collect() {
		List<T> l = new ArrayList<T>();
		for (T t : this) {
			l.add(t);
		}
		return l;
	}

	static int	BATCH_SIZE	= 200;

	/**
	 * Visits the caller so that we can batch the selection.
	 */
	@Override
	public boolean visit(Visitor<T> visitor) throws Exception {
		int i = 0;
		while (true) {
			limit(BATCH_SIZE);
			int n = 0;
			for (T t : this) {
				if (!visitor.visit(t))
					return false;

				n++;
			}
			if (n != BATCH_SIZE)
				return true;

			skip(BATCH_SIZE * ++i);
		}
	}

	/**
	 * Index the text and set the keywords field with the tokenized texts.
	 */
	@Override
	public Cursor<T> text(String text) throws Exception {
		Search search = new Search();
		search.addAll(text);
		appendAll("__keywords", search.set());
		
		return this;
	}

	/**
	 * Index the text and set the keywords field with the tokenized texts.
	 */
	@Override
	public Cursor<T> word(String word) throws Exception {
		Search search = new Search();
		search.add(word);
		for (String s : search.set())
			append("__keywords", s);

		return this;
	}

	/**
	 * Create a query based on the query string and the templates.
	 * 
	 * @param query
	 *            A free text query string
	 * @param templates
	 *            The parsers picks out key:value strings and uses the templates
	 *            to replace them
	 */
	@Override
	public Cursor<T> query(String q, Map<String,String> templates) throws Exception {
		assert q != null;
		String parts[] = q.split("\\s+");
		Search positive = new Search();
		Search negative = new Search();

		for (String p : parts) {
			Matcher m = QUERY.matcher(p);
			if (templates != null && m.matches()) {
				boolean neg = m.group(1) != null;
				String key = m.group(2);
				String type = templates.get(key);

				if (type != null) {
					type += "=%s";
					String word = m.group(3);
					if (neg)
						where("!(" + type + ")", word);
					else
						where(type, word);
				}
			} else {
				if (p.startsWith("-"))
					negative.addAll(p);
				else
					positive.addAll(p);
			}
		}
		for (String key : positive.set()) {
			where("__keywords=%s", key);
		}
		for (String key : negative.set()) {
			where("(!(__keywords=%s)", key);
		}
		return this;
	}

}
