/*
 * Copyright 2012 Tom Gibara
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
package com.tomgibara.crinch.coding;

import java.math.BigInteger;

import com.tomgibara.crinch.bits.BitReader;
import com.tomgibara.crinch.bits.BitWriter;

public class RiceCoding extends UniversalCoding {

	private final int bits;
	
	// TODO need to investigate performance implications of allowing more than 64 bits
	public RiceCoding(int bits) {
		if (bits < 0) throw new IllegalArgumentException("negative bits");
		if (bits > 64) throw new IllegalArgumentException("bits greater than 64 not supported");
		this.bits = bits;
	}
	
	public int getBits() {
		return bits;
	}
	
	@Override
	int unsafeEncodePositiveInt(BitWriter writer, int value) {
		return bits >= 32 ? writer.write((long) value, bits) :
			UnaryCoding.zeroTerminated.unsafeEncodePositiveInt(writer, value >>> bits)
			+ writer.write(value, bits);
	}

	@Override
	int unsafeEncodePositiveLong(BitWriter writer, long value) {
		return
		UnaryCoding.zeroTerminated.unsafeEncodePositiveLong(writer, value >>> bits)
		+ writer.write(value, bits);
	}

	@Override
	int unsafeEncodePositiveBigInt(BitWriter writer, BigInteger value) {
		return
		UnaryCoding.zeroTerminated.unsafeEncodePositiveBigInt(writer, value.shiftRight(bits))
		+ writer.write(value.longValue(), bits);
	}

	@Override
	public int decodePositiveInt(BitReader reader) {
		return
		(UnaryCoding.zeroTerminated.decodePositiveInt(reader) << bits)
		| reader.read(bits);
	}

	@Override
	public long decodePositiveLong(BitReader reader) {
		return
		(UnaryCoding.zeroTerminated.decodePositiveLong(reader) << bits)
		| reader.readLong(bits);
	}

	@Override
	public BigInteger decodePositiveBigInt(BitReader reader) {
		return
		UnaryCoding.zeroTerminated.decodePositiveBigInt(reader)
		.shiftLeft(bits)
		.or(reader.readBigInt(bits));
	}

}