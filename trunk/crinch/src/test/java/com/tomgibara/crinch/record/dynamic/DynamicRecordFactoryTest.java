package com.tomgibara.crinch.record.dynamic;

import static com.tomgibara.crinch.record.ColumnType.BOOLEAN_PRIMITIVE;
import static com.tomgibara.crinch.record.ColumnType.BOOLEAN_WRAPPER;
import static com.tomgibara.crinch.record.ColumnType.CHAR_WRAPPER;
import static com.tomgibara.crinch.record.ColumnType.INT_PRIMITIVE;
import static com.tomgibara.crinch.record.ColumnType.LONG_PRIMITIVE;
import static com.tomgibara.crinch.record.ColumnType.STRING_OBJECT;

import java.util.ArrayList;
import java.util.Arrays;

import com.tomgibara.crinch.record.ColumnOrder;
import com.tomgibara.crinch.record.ColumnType;
import com.tomgibara.crinch.record.LinearRecord;
import com.tomgibara.crinch.record.ParsedRecord;
import com.tomgibara.crinch.record.RecordDefinition;
import com.tomgibara.crinch.record.StdColumnParser;
import com.tomgibara.crinch.record.StringRecord;
import com.tomgibara.crinch.record.ColumnParser;

import junit.framework.TestCase;

public class DynamicRecordFactoryTest extends TestCase {

	private static final ColumnParser parser = new StdColumnParser();
	
	public void testDefinitionCons() {
		try {
			new RecordDefinition(true, true, null);
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
		
		try {
			new RecordDefinition(true, true, new ArrayList<ColumnType>(), (ColumnOrder[]) null);
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}

		try {
			new RecordDefinition(true, true, Arrays.asList((ColumnType) null));
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
		
		try {
			new RecordDefinition(true, true, Arrays.asList(INT_PRIMITIVE), new ColumnOrder(-1, true, true));
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
		
		try {
			new RecordDefinition(true, true, Arrays.asList(INT_PRIMITIVE), new ColumnOrder(1, true, true));
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
		
		try {
			new RecordDefinition(true, true, Arrays.asList(INT_PRIMITIVE), new ColumnOrder(0, true, true), new ColumnOrder(0, true, true));
			fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
	}
	
	public void testGetName() {
		
		RecordDefinition def1 = new RecordDefinition(true, true, Arrays.asList(INT_PRIMITIVE, INT_PRIMITIVE, STRING_OBJECT), new ColumnOrder(2, false, false));
		RecordDefinition def2 = new RecordDefinition(true, true, Arrays.asList(INT_PRIMITIVE, INT_PRIMITIVE, INT_PRIMITIVE), new ColumnOrder(2, false, false));
		RecordDefinition def3 = new RecordDefinition(true, true, Arrays.asList(INT_PRIMITIVE, INT_PRIMITIVE, INT_PRIMITIVE));
		
		DynamicRecordFactory fac1 = DynamicRecordFactory.getInstance(def1);
		DynamicRecordFactory fac2 = DynamicRecordFactory.getInstance(def2);
		DynamicRecordFactory fac3 = DynamicRecordFactory.getInstance(def3);
		
		assertFalse(fac1.getName().equals(fac2.getName()));
		assertFalse(fac2.getName().equals(fac3.getName()));
		assertFalse(fac3.getName().equals(fac1.getName()));
	}
	
	public void testNewRecord() {
		RecordDefinition def = new RecordDefinition(true, false, Arrays.asList(INT_PRIMITIVE, BOOLEAN_PRIMITIVE, BOOLEAN_WRAPPER, STRING_OBJECT, LONG_PRIMITIVE, CHAR_WRAPPER), new ColumnOrder(0, true, false), new ColumnOrder(1, true, false), new ColumnOrder(2, false, true), new ColumnOrder(3, true, false), new ColumnOrder(4, true, false));
		DynamicRecordFactory fac = DynamicRecordFactory.getInstance(def);
		LinearRecord rec = fac.newRecord(new ParsedRecord(parser, new StringRecord(0L, -1L, "1", "true", "", "Tom", "3847239847239843", "")));
		assertEquals("[1,true,null,Tom,3847239847239843,null]", rec.toString());
	}
	
}