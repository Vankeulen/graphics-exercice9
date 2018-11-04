package ex9;
/**
 *
 * @author KoG
 */
public class IntFlags {
	
	/** Gets the value of the bit at bitIndex. */
	private static boolean get(int[] bits, int bitIndex) {
		int idx = bitIndex / 32;
		int pos = bitIndex % 32;
		return !((bits[idx] & (1 << pos)) == 0);
	}
	/** Sets the bit at bitIndex to enable, leaving other bits unnaffected. */
	private static void set(int[] bits, int bitIndex, boolean enable) {
		int idx = bitIndex / 32;
		int pos = bitIndex % 32;
		int val = bits[idx];
		
		if (enable) { val |= 1 << pos; } 
		else { val &= ~(1 << pos); }
		
		bits[idx] = val;
	}
	
	private int[] states;
	private int[] lastStates;
	private final int numSlots;
	
	public IntFlags(int numSlots) {
		this.numSlots = numSlots;
		states = new int[numSlots];
		lastStates = new int[numSlots];
	}
	
	public void next() {
		int[] s;
		s = lastStates;
		lastStates = states;
		states = s;
		System.arraycopy(lastStates, 0, states, 0, numSlots);
	}
	
	public boolean check(int bitPosition) {
		return get(states, bitPosition);
	}
	
	public boolean checkPressed(int bitPosition) {
		return get(states, bitPosition) && !get(lastStates, bitPosition);
	}
	
	public boolean checkReleased(int bitPosition) {
		return !get(states, bitPosition) && get(lastStates, bitPosition);
	}
	
	public void set(int bitPosition, boolean pressed) {
		set(states, bitPosition, pressed);
	}
	
	
}
