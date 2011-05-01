import com.beatofthedrum.alacdecoder.AlacDecodeUtils;
import com.beatofthedrum.alacdecoder.AlacFile;


public class AudioSession {
	private AlacFile alac;
	private byte[] aesiv;
	private byte[] aeskey;
	private int controlPort;
	private int timingPort;
	private int frameSize;
	private int sampleSize;
	private int _7a;
	private int rice_historymult;
	private int rice_initialhistory;
	private int rice_kmodifier;
	private int _7f;
	private int _80;
	private int _82;
	private int _86;
	private int _8a_rate;
	private BiquadFilter bFilter;
	
	
	public AudioSession(byte[] aesiv, byte[] aeskey, int[] fmtp, int controlPort, int timingPort){	
		// KEYS
		this.aesiv = aesiv;
		this.aeskey = aeskey;
		
		// PORTS
		this.controlPort = controlPort;
		this.timingPort = timingPort;
		
		// FMTP
		frameSize = fmtp[1];
		_7a = fmtp[2];
		sampleSize = fmtp[3];
		rice_historymult = fmtp[4];
		rice_initialhistory = fmtp[5];
		rice_kmodifier = fmtp[6];
		_7f = fmtp[7];
		_80 = fmtp[8];
		_82 = fmtp[9];
		_86 = fmtp[10];
		_8a_rate = fmtp[11];
		
		initDecoder();
	}
	
	/**
	 * Initiate the decoder
	 */
	private void initDecoder(){
		if (this.getSampleSize() != 16){
			System.err.println("ERROR: 16 bits only!!!");
			return;
		}
		
		alac = AlacDecodeUtils.create_alac(this.getSampleSize(), 2);
		if (alac == null){
			System.err.println("ERROR: creating alac!!!");
			return;
		}
		alac.setinfo_max_samples_per_frame = this.getFrameSize();
		alac.setinfo_7a = this.get_7a();
		alac.setinfo_sample_size = this.getSampleSize();
		alac.setinfo_rice_historymult = this.getRiceHistoryMult();
	    alac.setinfo_rice_initialhistory = this.getRiceInitialhistory();
	    alac.setinfo_rice_kmodifier = this.getRiceKModifier();
	    alac.setinfo_7f = this.get_7f();
	    alac.setinfo_80 = this.get_80();
	    alac.setinfo_82 = this.get_82();
	    alac.setinfo_86 = this.get_86();
	    alac.setinfo_8a_rate = this.get_8a_rate();
	}
	
	public int OUTFRAME_BYTES(){
		return 4*(this.getFrameSize()+3);
	}
	
	public AlacFile getAlac(){
		return alac;
	}
	
	public void resetFilter(){
		bFilter = new BiquadFilter(this.getSampleSize(), this.getFrameSize());
	}
	
	public void updateFilter(int size){
		bFilter.update(size);
	}
	
	public BiquadFilter getFilter(){
		return bFilter;
	}
	
	public byte[] getAESIV(){
		return this.aesiv;
	}

	public byte[] getAESKEY(){
		return this.aeskey;
	}
	
	public int getControlPort(){
		return this.controlPort;
	}
	
	public int getTimingPort(){
		return this.timingPort;
	}
	
	public int getFrameSize(){
		return this.frameSize;
	}
	
	public int getSampleSize(){
		return this.sampleSize;
	}
	
	public int get_7a(){
		return this._7a;
	}
	
	public int getRiceHistoryMult(){
		return this.rice_historymult;
	}
	
	public int getRiceInitialhistory(){
		return this.rice_initialhistory;
	}

	public int get_8a_rate(){
		return this._8a_rate;
	}
	
	public int get_86(){
		return this._86;
	}
	
	public int get_82(){
		return this._82;
	}
	
	public int get_80(){
		return this._80;
	}
	
	public int get_7f(){
		return this._7f;
	}
	
	public int getRiceKModifier(){
		return this.rice_kmodifier;
	}
}
