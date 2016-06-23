package edu.iris.dmc.seedcodec;

public class Sro {
	public static int[] decode(byte[] b, int numSamples, boolean swapBytes) throws CodecException {
		if (b.length < numSamples * 2) {
			throw new CodecException("Not enough bytes for " + numSamples
					+ " samples, need " + (2 * numSamples) + " but only have "
					+ b.length);
		}
		int[] out = new int[numSamples];
		int i=0;
		for (int lp = 0; lp < (numSamples * 2); lp += 2) {
			int j, gr;
			j = (b[lp] & 0x0F) << 8;
			j |= (b[lp + 1] & 0xFF);
			if (j >= 0x800)
				j -= 4096;
			gr = (b[lp] & 0xF0) >> 4;
			j <<= (10 - gr);
			out[i] = j;
			i++;
		}
		return out;
	}
}
