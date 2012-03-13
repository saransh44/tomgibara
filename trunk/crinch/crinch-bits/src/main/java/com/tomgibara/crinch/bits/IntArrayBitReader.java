/*
 * Copyright 2007 Tom Gibara
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

/**
 * A {@link BitReader} that sources its bits from an InputStream. Bits are read
 * from the int array starting at index zero. Within each int, the most
 * signficant bits are read first.
 * 
 * 
 * @author Tom Gibara
 */

//TODO optimize readUntil
public class IntArrayBitReader extends AbstractBitReader {

    // statics
    
    //hope that this will be inlined...
    //i is number of 1's in lsbs
    private static int mask(int i) {
        return i == 0 ? 0 : -1 >>> (32 - i);
    }
    
    // fields
    
    private final int[] ints;
    private final long size;
    private long position = 0L;
    
    // constructors

    /**
	 * Creates a new {@link BitReader} which is backed by an int array with
	 * least capacity required to store the specified number of bits.
	 * 
	 * @param size
	 *            the number of bits that can be read, not negative, not greater
	 *            than greatest possible number of bits in an int array
	 * @see #getInts()
	 */
    
    public IntArrayBitReader(long size) {
        if (size < 0) throw new IllegalArgumentException("negative size");
        long length = size >> 5;
        if (length > Integer.MAX_VALUE) throw new IllegalArgumentException("size exceeds maximum possible array bits");
        ints = new int[(int) length];
        this.size = size;
    }
    
    /**
	 * Creates a new {@link BitReader} which is backed by the specified int array.
	 * The size of the reader will equal the total number of bits in the array.
	 * 
	 * @param ints
	 *            the ints from which bits will be read, not null
	 * @see #getSize()
	 */
    
    public IntArrayBitReader(int[] ints) {
    	if (ints == null) throw new IllegalArgumentException("null ints");
    	this.ints = ints;
    	size = ((long) ints.length) << 5;
    }

	/**
	 * Creates a new {@link BitReader} which is backed by the specified int
	 * array. Bits will be read from the int array up to the specified size.
	 * 
	 * @param ints
	 *            the ints from which bits will be read, not null
	 * @param size
	 *            the number of bits that may be read, not negative and no
	 *            greater than the number of bits supplied by the array
	 */
    
    public IntArrayBitReader(int[] ints, long size) {
    	if (ints == null) throw new IllegalArgumentException("null ints");
        if (size < 0) throw new IllegalArgumentException("negative size");
        long maxSize = ((long) ints.length) << 5;
        if (size > maxSize) throw new IllegalArgumentException("size exceeds maximum permitted by array length");
        this.ints = ints;
        this.size = size;
    }

    // bit reader methods
    
    @Override
    public int readBit() {
        if (position >= size) throw new EndOfBitStreamException();
        int k = (ints[(int)(position >> 5)] >> (31 - (((int)position) & 31))) & 1;
        position++;
        return k;
    }

    @Override
    public int read(int count) {
        if (count == 0) return 0;
        if (position + count > size) throw new IllegalStateException(String.format("position: %d, size: %d, count %d", position, size, count));
        int frontBits = ((int)position) & 31;
        int firstInt = (int)(position >> 5);
        int value;

        int sumBits = count + frontBits;
        if (sumBits <= 32) {
            value = (ints[firstInt] >> (32 - sumBits)) & mask(count);
        } else {
            value = ((ints[firstInt] << (sumBits - 32)) | (ints[firstInt + 1] >>> (64 - sumBits))) & mask(count);
        }

        position += count;
        return value;
    }

    @Override
    public long skipBits(long count) {
    	if (count < 0) throw new IllegalArgumentException("negative count");
    	count = Math.min(size - position, count);
    	position += count;
    	return count;
    }
    
    @Override
    public long getPosition() {
        return position;
    }
    
    public long setPosition(long position) {
        if (position < 0) throw new IllegalArgumentException();
        return this.position = Math.min(position, size);
    }
    

    // accessors

    /**
     * The int array the backs this {@link BitReader}.
     * 
     * @return the ints read by this {@link BitReader}, never null
     */
    
    public int[] getInts() {
        return ints;
    }

	/**
	 * The maximum number of bits that may be read by this {@link BitReader}.
	 * 
	 * @return the least position at which there is no bit to read, never
	 *         negative
	 */
    
    public long getSize() {
        return size;
    }
    
}
