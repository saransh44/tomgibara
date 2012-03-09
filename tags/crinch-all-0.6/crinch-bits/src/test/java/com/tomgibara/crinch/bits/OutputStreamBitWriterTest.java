package com.tomgibara.crinch.bits;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class OutputStreamBitWriterTest extends AbstractByteBasedBitWriterTest {

	@Override
	ByteBasedBitWriter newBitWriter(long size) {
		return new Writer(new ByteArrayOutputStream((int) size));
	}

	@Override
	BitReader bitReaderFor(BitWriter writer) {
		return new InputStreamBitReader(new ByteArrayInputStream(getWrittenBytes(writer)));
	}

	@Override
	byte[] getWrittenBytes(BitWriter writer) {
		Writer w = (Writer) writer;
		return w.out.toByteArray();
	}
	
	private static class Writer extends OutputStreamBitWriter {
		
		final ByteArrayOutputStream out;
		
		Writer(ByteArrayOutputStream out) {
			super(out);
			this.out = out;
		}
		
	}
	
}
