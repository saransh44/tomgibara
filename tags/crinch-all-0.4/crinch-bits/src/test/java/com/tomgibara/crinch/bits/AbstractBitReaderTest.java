/*
 * Copyright 2011 Tom Gibara
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.tomgibara.crinch.bits;

import java.util.Random;

import junit.framework.TestCase;

public abstract class AbstractBitReaderTest extends TestCase {

	abstract BitReader readerFor(BitVector vector);

	public void testReadBoolean() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			int size = r.nextInt(25) * 32;
			BitVector source = new BitVector(r, size);
			BitReader reader = readerFor(source);

			for (int j = 0; j < size; j++) {
				assertEquals("at bit " + j, source.getBit(j), reader.readBoolean());
			}
		}
	}
	
	public void testSkip() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			
			int size = r.nextInt(2500) * 32;
			BitVector source = new BitVector(r, size);
			BitReader reader = readerFor(source);
			
			long skipped = 0L;
			long read = 0L;
			while (true) {
				long oldpos = reader.getPosition();
				long toskip = r.nextInt(50);
				long actual = reader.skipBits(toskip);
				assertTrue(actual <= toskip);
				skipped += actual;
				long newpos = reader.getPosition();
				assertEquals(newpos, oldpos + actual);
				if (newpos == size) break;
				assertTrue(actual == toskip);
				int position = (int) reader.getPosition();
				assertEquals("at bit " + position,  source.getBit(position), reader.readBoolean());
				read++;
			}
			assertEquals(size, skipped + read);
		}
	}
	
	public void testRead() {
		Random r = new Random(0L);
		for (int i = 0; i < 1000; i++) {
			int size = r.nextInt(25) * 32;
			BitVector source = new BitVector(r, size);
			BitVector reverse = source.mutableCopy();
			reverse.reverse();
			BitReader reader = readerFor(source);

			while (true) {
				int oldpos = (int) reader.getPosition();
				int count = Math.min(size - oldpos, r.nextInt(33));
				int bits = reader.read(count);
				int newpos = (int) reader.getPosition();
				assertEquals(oldpos + count, newpos);
				int actual = (int) reverse.getBits(size - newpos, count);
				assertEquals(actual, bits);
				if (newpos == size) break;
			}
		}
	}
	
}