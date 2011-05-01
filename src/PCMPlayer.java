import javax.sound.sampled.*;
import javax.sound.sampled.DataLine.Info;




/**
 * Will create a new thread and play packets added to the ring buffer and set as ready
 * @author bencall
 *
 */
public class PCMPlayer extends Thread{
	AudioFormat audioFormat;
	Info info;
	SourceDataLine dataLine;
	AudioServer server;
	long fix_volume = 0x10000;
	short rand_a, rand_b;
	
	public PCMPlayer(AudioServer server){
		super();
		this.server = server;
	
        try {
            audioFormat = new AudioFormat(44100, 16, 2, true, true);
            info = new DataLine.Info(SourceDataLine.class, audioFormat);
			dataLine = (SourceDataLine) AudioSystem.getLine(info);
	        dataLine.open(audioFormat);
	        dataLine.start();

        } catch (LineUnavailableException e) {
			e.printStackTrace();
		}

        
	}
	
	public void run(){
		while(true){
			int[] buf = server.getNextFrame();
			if(buf==null){
				continue;
			}
			
			int[] outbuf = new int[4*(server.getFrameSize()+3)];
			int k = stuff_buffer(server.getFilter().bf_playback_rate, buf, outbuf);

			byte[] input = new byte[outbuf.length*2];
			
			int j = 0;
			for(int i=0; i<outbuf.length; i++){
				input[j++] = (byte)(outbuf[i] >> 8);
				input[j++] = (byte)(outbuf[i]);
			}
			
			System.err.println("Play!!!");
			dataLine.write(input, 0, k*4);
		}
	}
	
	private int stuff_buffer(double playback_rate, int[] input, int[] output) {
		
	    int stuffsamp = server.getFrameSize();
	    int stuff = 0;
	    double p_stuff;
	    
	    p_stuff = 1.0 - Math.pow(1.0 - Math.abs(playback_rate-1.0), server.getFrameSize());
	    
	    if (Math.random() < p_stuff) {
	        stuff = playback_rate > 1.0 ? -1 : 1;
	        stuffsamp = (int) (Math.random() * (server.getFrameSize() - 2));
	    }

	    int j=0;
	    int l=0;
	    for (int i=0; i<stuffsamp; i++) {   // the whole frame, if no stuffing
	        output[j++] = dithered_vol(input[l++]);
	        output[j++] = dithered_vol(input[l++]);
	    }
	    
	    if (stuff!=0) {
	        if (stuff==1) {
	            // interpolate one sample
	            output[j++] = dithered_vol(((int)input[l-2] + (int)input[l]) >> 1);
	            output[j++] = dithered_vol(((int)input[l-1] + (int)input[l+1]) >> 1);
	        } else if (stuff==-1) {
	            l-=2;
	        }
	        for (int i=stuffsamp; i<server.getFrameSize() + stuff; i++) {
	        	output[j++] = dithered_vol(input[l++]);
	        	output[j++] = dithered_vol(input[l++]);
	        }
	    }
	    return server.getFrameSize() + stuff;
	}
	
	
	private short dithered_vol(int sample) {
	    long out;
	    rand_b = rand_a;
	    rand_a = (short) (Math.random() * 65535);
	    
	    out = (long)sample * fix_volume;
	    if (fix_volume < 0x10000) {
	        out += rand_a;
	        out -= rand_b;
	    }
	    return (short) (out>>16);
	}
}
