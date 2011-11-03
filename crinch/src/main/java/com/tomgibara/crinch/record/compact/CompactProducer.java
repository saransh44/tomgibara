package com.tomgibara.crinch.record.compact;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import com.tomgibara.crinch.bits.BitReader;
import com.tomgibara.crinch.bits.InputStreamBitReader;
import com.tomgibara.crinch.bits.ProfiledBitReader;
import com.tomgibara.crinch.coding.CodedReader;
import com.tomgibara.crinch.record.LinearRecord;
import com.tomgibara.crinch.record.ProcessContext;
import com.tomgibara.crinch.record.RecordProducer;
import com.tomgibara.crinch.record.RecordSequence;
import com.tomgibara.crinch.record.RecordStats;

public class CompactProducer implements RecordProducer<LinearRecord> {

	private final File file;
	
	public CompactProducer(File file) {
		if (file == null) throw new IllegalArgumentException("null file");
		this.file = file;
	}
	
	@Override
	public RecordSequence<LinearRecord> open(ProcessContext context) {
		Sequence seq = new Sequence(context);
		context.setRecordCount(seq.recordCount);
		return seq;
	}

	private class Sequence implements RecordSequence<LinearRecord> {
		
		final InputStream in;
		final BitReader reader;
		final CodedReader coded;
		final long recordCount;
		final RecordDecompactor decompactor;
		
		long recordsRead = 0;
		
		Sequence(ProcessContext context) {
			try {
				in = new BufferedInputStream(new FileInputStream(file), 1024);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			//reader = new InputStreamBitReader(in);
reader = new ProfiledBitReader(new InputStreamBitReader(in));
			coded = new CodedReader(reader, context.getCoding());
			RecordStats stats = RecordStats.read(coded);
			context.setRecordStats(stats);
			recordCount = stats.getRecordCount();
			context.setRecordCount(recordCount);
			decompactor = new RecordDecompactor(stats);
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean hasNext() {
			return recordsRead < recordCount;
		}
		
		@Override
		public LinearRecord next() {
			if (recordsRead == recordCount) throw new NoSuchElementException();
			recordsRead++;
			return decompactor.decompact(coded);
		}
		
		@Override
		public void close() {
((ProfiledBitReader) reader).dumpProfile(System.out);
			try {
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
	}
	
}