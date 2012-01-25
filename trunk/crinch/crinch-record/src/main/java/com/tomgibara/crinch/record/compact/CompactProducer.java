/*
 * Copyright 2011 Tom Gibara
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.tomgibara.crinch.record.compact;

import java.io.File;
import java.util.NoSuchElementException;

import com.tomgibara.crinch.bits.ByteBasedBitReader;
import com.tomgibara.crinch.bits.FileBitReaderFactory;
import com.tomgibara.crinch.bits.FileBitReaderFactory.Mode;
import com.tomgibara.crinch.coding.CodedReader;
import com.tomgibara.crinch.coding.ExtendedCoding;
import com.tomgibara.crinch.record.LinearRecord;
import com.tomgibara.crinch.record.RecordProducer;
import com.tomgibara.crinch.record.RecordSequence;
import com.tomgibara.crinch.record.RecordStats;
import com.tomgibara.crinch.record.def.RecordDef;
import com.tomgibara.crinch.record.def.SubRecordDef;
import com.tomgibara.crinch.record.process.ProcessContext;

public class CompactProducer implements RecordProducer<LinearRecord> {

	private final SubRecordDef subRecDef;
	
	private CompactStats compactStats;
	private ExtendedCoding coding;
	private RecordDecompactor decompactor;
	private FileBitReaderFactory fbrf;
	
	public CompactProducer() {
		this(null);
	}
	
	public CompactProducer(SubRecordDef subRecDef) {
		this.subRecDef = subRecDef;
	}
	
	@Override
	public void prepare(ProcessContext context) {
		RecordStats stats = context.getRecordStats();
		compactStats = new CompactStats("compact", context, subRecDef);
		compactStats.read();
		if (stats == null) throw new IllegalStateException("no statistics available");
		stats = stats.adaptFor(compactStats.definition);
		
		coding = context.getCoding();
		decompactor = new RecordDecompactor(stats, 0);
		File file = context.file(compactStats.type, false, compactStats.definition);
		fbrf = new FileBitReaderFactory(file, Mode.CHANNEL);
	}
	
	@Override
	public Accessor open() {
		return new Accessor();
	}

	public class Accessor implements RecordSequence<LinearRecord> {
		
		// local copy for possible performance gain
		final long bitsWritten = compactStats.bitsWritten;
		final RecordDecompactor decompactor = CompactProducer.this.decompactor.copy();
		
		final ByteBasedBitReader reader;
		final CodedReader coded;
		long ordinal = 0;
		
		Accessor() {
			reader = fbrf.openReader();
			coded = new CodedReader(reader, coding);
		}
		
		public Accessor setPosition(long position, long ordinal) {
			if (position < 0L) throw new IllegalArgumentException("negative position");
			if (position > bitsWritten) throw new IllegalArgumentException("position exceeds data length");
			if (ordinal < 0L) ordinal = -1L;
			reader.setPosition(position);
			this.ordinal = ordinal;
			return this;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean hasNext() {
			return reader.getPosition() < bitsWritten;
		}
		
		@Override
		public LinearRecord next() {
			if (reader.getPosition() == bitsWritten) throw new NoSuchElementException();
			CompactRecord record = decompactor.decompact(coded, ordinal, reader.getPosition());
			if (ordinal >= 0) ordinal++;
			return record;
			
		}
		
		@Override
		public void close() {
			fbrf.closeReader(reader);
		}
		
	}
	
	@Override
	public void complete() {
		compactStats = null;
		coding = null;
		decompactor = null;
		fbrf = null;
	}
	
}
