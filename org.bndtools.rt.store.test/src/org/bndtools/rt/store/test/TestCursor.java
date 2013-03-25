package org.bndtools.rt.store.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.bndtools.rt.store.test.TestStore.Pair;
import org.bndtools.service.store.Cursor;
import org.bndtools.service.store.Cursor.Visitor;
import org.bndtools.service.store.DB;
import org.bndtools.service.store.Store;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class TestCursor extends TestCase {

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
		populate();
	}
	
	public void tearDown() {
		context.ungetService(ref);
	}
	
	private void populate() throws Exception {
		for(int i=1; i<=10; i++) {
			for(int j=1; j<=10; j++) {
				Pair pair = new Pair(i,j);
				store.insert(pair);
			}
		}
	}
	
	public void test_where() throws Exception {
		
		assertEquals(10, store.where("first=%d", 5).count());
		assertEquals(2, store.where("&(first=%d)(second<%d)", 8, 3).count());
		assertEquals(20, store.where("|(second=%d)(second=%d)", 8, 4).count());
		assertEquals(90, store.where("!(second=%d)", 8).count());
		assertEquals(4, store.where("|(&(first=2)(second<=%d))(&(first=3)(second<=%d))",2,2).count());
	}
	
	public void test_or() throws Exception {
		
		Pair pair1 = store.where("first=1").first();
		Pair pair2 = store.where("first=2").first();
		
		Cursor<Pair> cursor = store.find(pair1).or(pair2);
		
		assertEquals(2, cursor.count());
		Iterator<Pair> it = cursor.iterator();
		Pair firstElt = it.next();
		Pair secondElt = it.next();
		
		assertTrue( (firstElt.equals(pair1) && secondElt.equals(pair2)) ||
					(firstElt.equals(pair2) && secondElt.equals(pair1)) );
		
		try {
			store.optimistic(pair1).or(pair2);
			fail();
		} catch(IllegalStateException e) {}
	}
	
	public void test_select() throws Exception {
		
		Pair pair = store.where("first=1").select("first").first();
		
		assertEquals(1, pair.first);
		assertEquals(0, pair.second);
	}
	
	public static class Slicee {
		public String _id;
		public int normalField;
		public int[] array;
		
		public boolean equals(Object o) {
			if(o == null || !(o instanceof Slicee)) {
				return false;
			}
			Slicee oo = (Slicee)o;
			return (_id == null ? oo._id == null : _id.equals(oo._id) && 
					normalField == oo.normalField && 
					array == null ? oo.array == null : array.equals(oo.array) );
		}
	}
	
	public void test_slice() throws Exception {
		Slicee slicee = new Slicee();
		slicee.array = new int[]{1,2,3,4,5,6,7,8,9,10};
		
		Store<Slicee> store2 = db.getStore(Slicee.class, "slicee");
		store2.insert(slicee);
		
		assertEquals(10, store2.all().one().array.length);
		assertEquals(4, store2.all().slice("array",4).one().array.length);
	}
	
	public void test_limit() throws Exception {
		Cursor<Pair> cursor = store.all().limit(10);
		
		int count = 0;
		for(Pair p : cursor) {
			count ++;
		}
		assertEquals(10, count);
		
		try {
			store.all().limit(-1);
			fail();
		} catch(IllegalArgumentException e) {}
	}
	
	public void test_skip() throws Exception {
		assertEquals(100, store.all().count());
		
		int count = 0;
		for(Pair p : store.all().skip(10)) {
			count ++;
		}
		assertEquals(90, count);
		
		assertEquals(2, store.all().ascending("first").skip(10).first().first);
	}
	
	public void test_ordering() throws Exception {
		Cursor<Pair> ascending = store.where("first=%d", 1).ascending("second");
		
		Iterator<Pair> it1 = ascending.iterator();
		int prev1 = Integer.MIN_VALUE;
		while(it1.hasNext()) {
			Pair pair = it1.next();
			assertTrue(pair.second >= prev1);
			prev1 = pair.second;
		}
		
		Cursor<Pair> descending = store.where("first=%d", 1).descending("second");
		
		Iterator<Pair> it2 = descending.iterator();
		int prev2 = Integer.MAX_VALUE;
		while(it2.hasNext()) {
			Pair pair = it2.next();
			assertTrue(pair.second <= prev2);
			prev2 = pair.second;
		}
	}
	
	public void test_set() throws Exception {
		int nbUpdate = store.where("first=%d", 5).set("second", -1).update();
		
		assertEquals(10, nbUpdate);
		
		Cursor<Pair> cursor = store.where("second=%d", -1);
		assertEquals(10, cursor.count());
		for(Pair p : cursor) {
			assertEquals(5, p.first);
		}
	}
	
	public void test_unset() throws Exception {
		int nbUpdate = store.where("first=%d", 7).unset("second").update();
		
		assertEquals(10, nbUpdate);
		
		Cursor<Pair> cursor = store.where("!(second=*)");
		assertEquals(10, cursor.count());
		for(Pair p : cursor) {
			assertEquals(7, p.first);
		}
	}
	
	public void test_append() throws Exception {
		Store<Slicee> store2 = db.getStore(Slicee.class, "slicee");
		
		Slicee slicee = new Slicee();
		slicee.array = new int[]{1,2,3};
		store2.insert(slicee);
		
		try {
			store2.all().append("normalField", 2).update();
			fail();
		} catch(Exception e) {} // Cannot append to non array field
		
		store2.all().append("array", 4).update();
		
		assertEquals(4, store2.all().one().array.length);
		assertEquals(4, store2.all().one().array[3]);
		
		store2.all().append("array", 5, 6).update();
		
		assertEquals(6, store2.all().one().array.length);
		assertEquals(6, store2.all().one().array[5]);
		
		try {
			Cursor<Slicee> cursor = store2.all();
			cursor.append("array", 7);
			cursor.append("array", 8);
			fail();
		} catch (UnsupportedOperationException e) {}
	}
	
	public void test_pull() throws Exception {
		Store<Slicee> store2 = db.getStore(Slicee.class, "slicee");
		
		Slicee slicee = new Slicee();
		slicee.array = new int[]{1,2,3,1};
		store2.insert(slicee);
		
		try {
			store2.all().pull("normalField", 1).update();
			fail();
		} catch(Exception e) {} // Cannot pull from non array field
		
		store2.all().pull("array", 1).update();
		assertEquals(2, store2.all().one().array.length);
		assertEquals(2, store2.all().one().array[0]);
		assertEquals(3, store2.all().one().array[1]);
		
		store2.all().pull("array", 2, 3).update();
		assertEquals(0, store2.all().one().array.length);
		
		try {
			store2.all().pull("array", 2).pull("array", 3).update();
			fail();
		} catch (UnsupportedOperationException e) {}
	}
	
	public void test_inc() throws Exception {
		store.where("first=10").inc("first", 5).update();
		
		assertEquals(10, store.where("first=15").count());
		ArrayList<Integer> list = new ArrayList<Integer>();
		for(int i=1; i<= 10; i ++) {
			list.add(i);
		}
		assertTrue(store.where("first=15").distinct("second").containsAll(list));
		
		store.where("second=1").inc("second", -5).update();
		
		list.remove(new Integer(10));
		list.add(15);
		assertEquals(10, store.where("second=-4").count());
		assertTrue(store.where("second=-4").distinct("first").containsAll(list));
		
		try {
			store.all().limit(1).inc("first", 1).inc("first", 2).update();
			fail();
		} catch(UnsupportedOperationException e) {}
		
		Pair pair = new Pair(0,0);
		store.insert(pair);
		store.find(pair).inc("first", 1).inc("second", 1).update();
		assertEquals(1, store.find(pair).one().first);
		assertEquals(1, store.find(pair).one().second);
	}
	
	public void test_in() throws Exception {
		assertEquals(30, store.all().in("first", 1,2,3).count());
		assertTrue(store.all().in("second", 0, -1).isEmpty());
	}
	
	public void test_eq() throws Exception {
		assertEquals(10, store.all().eq("first", 8).count());
		assertTrue(store.all().eq("second", 0).isEmpty());
	}
	
	public void test_gt_lt_gte_lte() throws Exception {
		assertEquals(40, store.all().gt("first", 4).lte("first", 8).count());
		assertEquals(40, store.all().lte("first", 8).gt("first", 4).count());
		assertEquals(16, store.all().lte("first", 8).gt("first", 4).gt("second", 4).lte("second", 8).count());
		assertEquals(0, store.all().gt("first", 5).lt("first", 5).count());
	}
	
	public void test_visit() throws Exception {
		boolean result = store.all().visit(new Visitor<TestStore.Pair>() {
			
			@Override
			public boolean visit(Pair t) throws Exception {
				if(t.first == 5) {
					t.second = -5;
					store.update(t);
				}
				return true;
			}
		});

		assertTrue(result);
		Cursor<Pair> cursor = store.all().eq("second", -5);
		assertEquals(10, cursor.count());
		for(Pair p : cursor) {
			assertEquals(5, p.first);
		}
	}
	
	public void test_first_one() throws Exception {
		Store<Slicee> store2 = db.getStore(Slicee.class, "slicee");
		
		Slicee slicee = new Slicee();
		store2.insert(slicee);
		
		assertEquals(slicee, store2.all().first());
		assertEquals(slicee, store2.all().one());
		
		Slicee slicee2 = new Slicee();
		store2.insert(slicee2);
		
		try {
			store2.all().one();
			fail();
		} catch (Exception e) {}
		assertTrue(slicee.equals(store2.all().first()) || slicee2.equals(store2.all().first()));
	}
	
	public void test_remove() throws Exception {
		assertEquals(10, store.where("first=5").remove());
		assertTrue(store.where("first=5").isEmpty());
		assertEquals(90, store.all().count());
	}
	
	public static class DistinctArrayList {
		public String _id;
		public ArrayList<Integer> field;
	}
	
	public static class DistinctGenericArray {
		public String _id;
		public List<Integer>[] field;
	}
	
	public void test_distinct() throws Exception {
		List<?> distinct = store.all().distinct("first");
		
		assertEquals(10, distinct.size());
		for(int i=1; i<=10; i++) {
			assertTrue(distinct.contains(i));
		}
		
		Store<Slicee> store2 = db.getStore(Slicee.class, "slicee");
		Slicee slicee1 = new Slicee();
		Slicee slicee2 = new Slicee();
		slicee1.array = new int[]{1,2,3};
		slicee2.array = new int[]{3,4,5};
		store2.insert(slicee1);
		store2.insert(slicee2);
		
		List<?> distinct2 = store2.all().distinct("array");
		assertEquals(5, distinct2.size());
		for(int i=1; i<=5; i++) {
			assertTrue(distinct2.contains(i));
		}
		
		Store<DistinctArrayList> store3 = db.getStore(DistinctArrayList.class, "distinctArrayList");
		DistinctArrayList d1 = new DistinctArrayList();
		DistinctArrayList d2 = new DistinctArrayList();
		d1.field = new ArrayList<Integer>();
		for(int i=1; i<=3; i++) {
			d1.field.add(i);
		}
		d2.field = new ArrayList<Integer>();
		for (int i = 3; i <= 5; i++) {
			d2.field.add(i);
		}
		store3.insert(d1);
		store3.insert(d2);
		List< ? > distinct3 = store3.all().distinct("field");
		assertEquals(5, distinct3.size());
		for (int i = 1; i <= 5; i++) {
			assertTrue(distinct3.contains(i));
		}
		
		Store<DistinctGenericArray> store4 = db.getStore(DistinctGenericArray.class, "distinctArrayListArray");
		DistinctGenericArray dl1 = new DistinctGenericArray();
		DistinctGenericArray dl2 = new DistinctGenericArray();
		dl1.field = new List[3];
		dl1.field[0] = Arrays.asList(1,2);
		dl1.field[1] = Arrays.asList(2,3);
		dl1.field[2] = Arrays.asList(3,4,5);
		dl2.field = new List[1];
		dl2.field[0] = Arrays.asList(3,4,5);
		store4.insert(dl1);
		store4.insert(dl2);
		List< ? > distinct4 = store4.all().distinct("field");
		assertEquals(3, distinct4.size());
		assertTrue(distinct4.contains(Arrays.asList(1,2)));
		assertTrue(distinct4.contains(Arrays.asList(2,3)));
		assertTrue(distinct4.contains(Arrays.asList(3,4,5)));
	}
	
	public void test_text_word() throws Exception {
		Store<Slicee> store2 = db.getStore(Slicee.class, "slicee");
		
		Slicee slicee1 = new Slicee();
		slicee1.normalField = 1;
		store2.insert(slicee1);
		store2.find(slicee1).word("hello").update();
		
		Slicee slicee2 = new Slicee();
		slicee2.normalField = 2;
		store2.insert(slicee2);
		store2.find(slicee2).text("hello world").update();
		
		assertEquals(2, store2.all().query("hello", null).count());
		assertEquals(1, store2.all().query("hello world", null).count());
		assertEquals(1, store2.all().query("	world  hello", null).count());
		assertEquals(1, store2.all().query("hello -world", null).count());
		assertEquals(1, store2.all().query("hello -world", null).one().normalField);
		
		Map<String, String>templates = new HashMap<String, String>();
		templates.put("n", "normalField");
		
		assertEquals(1, store2.all().query("n:1", templates).count());
		assertEquals(1, store2.all().query("n:1", templates).one().normalField);
		
		assertEquals(1, store2.all().query("-n:1", templates).count());
		assertEquals(2, store2.all().query("-n:1", templates).one().normalField);
	}
}
