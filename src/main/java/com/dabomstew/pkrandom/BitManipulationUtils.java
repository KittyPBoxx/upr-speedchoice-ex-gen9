package com.dabomstew.pkrandom;

import java.util.BitSet;
import java.util.stream.IntStream;

public class BitManipulationUtils {

    public static void writeValue(byte[] byteArray, int bitPosition, int bitLength, int value) {
        BitSet bitSet = byteArrayToBitSet(byteArray);

        for (int i = 0; i < bitLength; i++) {
            int bit = (value >> i) & 1;
            bitSet.set(bitPosition + i, bit == 1);
        }

        byte[] updatedByteArray = bitSetToByteArray(bitSet, byteArray.length);
        System.arraycopy(updatedByteArray, 0, byteArray, 0, byteArray.length);
    }

    public static int readValue(byte[] byteArray, int bitPosition, int bitLength) {
        BitSet bitSet = byteArrayToBitSet(byteArray);
        int value = 0;

        for (int i = 0; i < bitLength; i++) {
            if (bitSet.get(bitPosition + i)) {
                value |= (1 << i);
            }
        }

        return value;
    }

    private static BitSet byteArrayToBitSet(byte[] bytes) {
        BitSet bitSet = new FixedLengthBitSet(bytes.length * 8);
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[i / 8] & (1 << (i % 8))) != 0) {
                bitSet.set(i);
            }
        }
        return bitSet;
    }

    private static byte[] bitSetToByteArray(BitSet bitSet, int byteArrayLength) {
        byte[] bytes = new byte[byteArrayLength];
        for (int i = 0; i < byteArrayLength * 8; i++) {
            if (bitSet.get(i)) {
                bytes[i / 8] |= (byte) (1 << (i % 8));
            }
        }
        return bytes;
    }

    /*
    * Code Review:
    * The name 'FixedBitSetButTheToStringImplementationWasntMadeBySomeoneWhoShouldBeCommittedToArkham',
    * while accurate, violates our companies class name policy.
    * Please update this with something that won't trigger the linter.
    */
    private static class FixedLengthBitSet extends BitSet {
        private final int actualNumberOfBits;

        private FixedLengthBitSet() {
            throw new RuntimeException("Error creating a fixed length bitset with no size.");
        }

        public FixedLengthBitSet(int nbits) {
            super(nbits);
            actualNumberOfBits = nbits;
        }

        @Override
        public String toString() {
            final StringBuilder buffer = new StringBuilder(actualNumberOfBits);
            IntStream.range(0, actualNumberOfBits).mapToObj(i -> get(i) ? '1' : '0').forEach(buffer::append);
            return buffer.toString().replaceAll("(.{8})", "$1 ").trim();
        }
    }

}
