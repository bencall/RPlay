/**
 * Immutable Audio-Data class.
 * @author bencall
 *
 */
public class AudioData {
	private final int[] data;
	private final int sequenceNumber;

	public AudioData(int[] data, int sequenceNumber) {
		this.data = data;
		this.sequenceNumber = sequenceNumber;
	}

	public int[] getData() {
		return data;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AudioData)) return false;

		AudioData audioData = (AudioData) o;

		if (sequenceNumber != audioData.sequenceNumber) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return sequenceNumber;
	}
}
