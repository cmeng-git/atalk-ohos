/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.impl.neomedia.conference;

import java.lang.ref.SoftReference;

import javax.media.Buffer;

/**
 * Caches <code>short</code> arrays for the purposes of reducing garbage collection.
 *
 * @author Lyubomir Marinov
 */
class ShortArrayCache
{
	/**
	 * The cache of <code>short</code> arrays managed by this instance for the purposes of reducing
	 * garbage collection.
	 */
	private SoftReference<short[][]> elements;

	/**
	 * The number of elements at the head of {@link #elements} which are currently utilized.
	 * Introduced to limit the scope of iteration.
	 */
	private int length;

	/**
	 * Allocates a <code>short</code> array with length/size greater than or equal to a specific
	 * number. The returned array may be a newly-initialized instance or one of the elements
	 * cached/pooled by this instance.
	 *
	 * @param minSize
	 * 		the minimum length/size of the array to be returned
	 * @return a <code>short</code> array with length/size greater than or equal to <code>minSize</code>
	 */
	public synchronized short[] allocateShortArray(int minSize)
	{
		short[][] elements = (this.elements == null) ? null : this.elements.get();

		if (elements != null) {
			for (int i = 0; i < length; i++) {
				short[] element = elements[i];

				if ((element != null) && element.length >= minSize) {
					elements[i] = null;
					return element;
				}
			}
		}
		return new short[minSize];
	}

	/**
	 * Returns a specific non-<code>null</code> <code>short</code> array into the cache/pool implemented by
	 * this instance.
	 *
	 * @param shortArray
	 * 		the <code>short</code> array to be returned into the cache/pool implemented by this
	 * 		instance. If <code>null</code> , the method does nothing.
	 */
	public synchronized void deallocateShortArray(short[] shortArray)
	{
		if (shortArray == null)
			return;

		short[][] elements;

		if ((this.elements == null) || ((elements = this.elements.get()) == null)) {
			elements = new short[8][];
			this.elements = new SoftReference<short[][]>(elements);
			length = 0;
		}

		if (length != 0)
			for (int i = 0; i < length; i++)
				if (elements[i] == shortArray)
					return;

		if (length == elements.length) {
			/*
			 * Compact the non-null elements at the head of the storage in order to possibly
			 * prevent reallocation.
			 */
			int newLength = 0;

			for (int i = 0; i < length; i++) {
				short[] element = elements[i];

				if (element != null) {
					if (i != newLength) {
						elements[newLength] = element;
						elements[i] = null;
					}
					newLength++;
				}
			}

			if (newLength == length) {
				// Expand the storage.
				short[][] newElements = new short[elements.length + 4][];

				System.arraycopy(elements, 0, newElements, 0, elements.length);
				elements = newElements;
				this.elements = new SoftReference<short[][]>(elements);
			}
			else {
				length = newLength;
			}
		}
		elements[length++] = shortArray;
	}

	/**
	 * Ensures that the <code>data</code> property of a specific <code>Buffer</code> is set to an
	 * <code>short</code> array with length/size greater than or equal to a specific number.
	 *
	 * @param buffer
	 * 		the <code>Buffer</code> the <code>data</code> property of which is to be validated
	 * @param newSize
	 * 		the minimum length/size of the <code>short</code> array to be set as the value of the
	 * 		<code>data</code> property of the specified <code>buffer</code> and to be returned
	 * @return the value of the <code>data</code> property of the specified <code>buffer</code> which is
	 * guaranteed to have a length/size of at least <code>newSize</code> elements
	 */
	public short[] validateShortArraySize(Buffer buffer, int newSize)
	{
		Object data = buffer.getData();
		short[] shortArray;

		if (data instanceof short[]) {
			shortArray = (short[]) data;
			if (shortArray.length < newSize) {
				deallocateShortArray(shortArray);
				shortArray = null;
			}
		}
		else
			shortArray = null;
		if (shortArray == null) {
			shortArray = allocateShortArray(newSize);
			buffer.setData(shortArray);
		}
		return shortArray;
	}
}
