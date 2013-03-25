package org.bndtools.rt.store.test;

import java.util.*;

import junit.framework.TestCase;

import org.bndtools.service.store.DB;
import org.bndtools.service.store.Store;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class TestStore extends TestCase {

	static BundleContext context = FrameworkUtil.getBundle(TestStore.class).getBundleContext();
	
	static {
		try {
			ConfigurationAdmin configAdmin = (ConfigurationAdmin) context.getService(
				context.getServiceReference(ConfigurationAdmin.class));
		
			Configuration config = configAdmin.createFactoryConfiguration("org.bndtools.rt.store.mongo.MongoDBImpl", null);
			Dictionary<String, Object> properties = new Hashtable<String, Object>();
			properties.put("db", "test-store");
			config.update(properties);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	ServiceReference<DB> ref;
	DB db = null;
	Store<Pair> store;
	
	public void setUp() throws Exception {
		ref = context.getServiceReference(DB.class);
		Thread.sleep(100); // Give the framework a bit of time b/f requesting the service
		db = (DB) context.getService(ref);
		store = db.getStore(Pair.class, "collection");
	}
	
	public void tearDown() {
		context.ungetService(ref);
	}
	
	public static class Pair {
		public String	_id;
		public int		first;
		public int		second;
		
		public Pair()  {};
		
		@Override
		public boolean equals(Object arg0) {
			if(arg0 == null || !(arg0 instanceof Pair)) {
				return false;
			}
			Pair arg1 = (Pair) arg0;
			return (this.first == arg1.first && this.second == arg1.second);
		}

		public Pair(int first, int second) {
			this.first = first;
			this.second = second;
		}
		
		public Pair(String _id, int first, int second) {
			this(first, second);
			this._id = _id;
		}
		
		public String toString() {
			return first+":"+second+" ["+_id+"]";
		}
		
		
	}
	
	public void test_insert() throws Exception {
		Pair pair = new Pair(10, 15); 

		// pair is given an ID by the service
		assertNull(pair._id);
		store.insert(pair);
		assertNotNull(pair._id);
		
		assertEquals(1, store.count());
		Pair fromDB = store.all().one();
		assertEquals(pair.first, fromDB.first);
		assertEquals(pair.second, fromDB.second);
		
		Pair pairWithID = new Pair("UID", 1, 2);
		
		// pairWithID should keep the same _id when inserted
		store.insert(pairWithID);
		
		assertEquals(2, store.count());
		assertEquals(1, store.where("_id=%s", "UID").count());
		fromDB = store.where("_id=%s", "UID").one();
		assertEquals(pairWithID.first, fromDB.first);
		assertEquals(pairWithID.second, fromDB.second);
	}
	
	public void test_update() throws Exception {
		Pair pairWithoutID = new Pair(1,2);
		
		try {
			store.update(pairWithoutID);
			fail("It is not possible to update a document without _id");
		} catch(IllegalArgumentException e) {}
		try {
			store.update(pairWithoutID, "first");
			fail("It is not possible to update a document without _id");
		} catch(IllegalArgumentException e) {}
		
		Pair pairWithFakeID = new Pair("Dummy ID",1,2);
		try {
			store.update(pairWithFakeID);
			fail("It is not possible to update a document which was not inserted in the database");
		} catch(IllegalArgumentException e) {}
		try {
			store.update(pairWithFakeID, "first");
			fail("It is not possible to update a document which was not inserted in the database");
		} catch(IllegalArgumentException e) {}
		
		Pair realPair = new Pair(1,2);
		store.insert(realPair);
		
		String id = realPair._id;
		
		realPair.first = 3;
		realPair.second = 4;
		store.update(realPair, "first"); // Selective update
		
		Pair fetchedPair = store.all().one();
		
		assertEquals(id, fetchedPair._id);
		assertEquals(3,  fetchedPair.first);
		assertEquals(2, fetchedPair.second);
		
		store.update(realPair); // Complete update
		
		Pair fetchedPair2 = store.all().one();
		assertEquals(id, fetchedPair2._id);
		assertEquals(3, fetchedPair2.first);
		assertEquals(4, fetchedPair2.second);
	}
	

	public void test_upsert() throws Exception {
		Pair pair = new Pair(1,2);
		
		store.upsert(pair);
		assertNotNull(pair._id);
		
		assertEquals(1, store.count());
		Pair fetched = store.all().one();
		assertEquals(1, fetched.first);
		assertEquals(2, fetched.second);
		
		fetched.first = 3;
		fetched.second = 4;
		
		store.upsert(fetched);
		
		assertEquals(1, store.count());
		
		Pair fetched2 = store.all().one();
		assertEquals(3, fetched.first);
		assertEquals(3, fetched2.first);
		assertEquals(4, fetched.second);
		assertEquals(4, fetched2.second);
	}
	
	public void test_find() throws Exception {
		Pair pair = new Pair(1,2);
		
		Pair result = store.insert(pair);
		
		Pair fetched = store.find(pair).one();
		
		assertEquals(pair, result);
		assertEquals(fetched, result);
		
		pair._id = "Invalid";
		assertEquals(0, store.find(pair).count());
	}

	public void test_optimistic() throws Exception {
		Pair pair = new Pair(1,2);
		
		store.insert(pair);
		
		Pair fetched = store.all().one();
		store.all().inc("first",1).update();
		
		try {
			store.optimistic(fetched).inc("first", 1).update();
			fail();
		} catch (ConcurrentModificationException e) {}
		
		assertEquals(2, store.all().one().first);
	}
	
	public static class Cycle {
		public String _id;
		public Cycle ref;
		public List<Cycle> refs;
		public Cycle refArray[];
	}
	public void test_cycling_insert() throws Exception {
		Store<Cycle> store2 = db.getStore(Cycle.class, "cycle");
		
		Cycle c = new Cycle();
		c.ref = c;
		
		try {
			store2.insert(c);
			fail();
		} catch(IllegalArgumentException e) {}
		
		c.ref = null;
		c.refs = new ArrayList<Cycle>(Arrays.asList(new Cycle(), new Cycle()));
		store2.insert(c);
		
		c.refs.add(c);
		
		try {
			store2.update(c);
			fail();
		} catch(IllegalArgumentException e) {}
		
		c.refs = null;
		c.refArray = new Cycle[2];
		c.refArray[0] = null;
		Cycle indirect = new Cycle();
		indirect.ref = c;
		c.refArray[1] = indirect;
		
		try {
			store2.update(c);
			fail();
		} catch(IllegalArgumentException e) {}
	}
}