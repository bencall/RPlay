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
			byte[] input = new byte[buf.length*2];
			
			for(int i=0; i<buf.length; i++){
				//(byte)(value >>> 24), (byte)(value >> 16 & 0xff), (byte)(value >> 16 & 0xff), (byte)(value & 0xff)
				input[i] = (byte)(buf[i] >> 16 & 0xff);
				input[i+1] = (byte)(buf[i] & 0xff);
			}
			
			System.err.println("Play!!!");
			dataLine.write(input, 0, server.getFrameSize()*2);
		}
	}
}
