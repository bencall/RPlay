import com.beatofthedrum.alacdecoder.AlacDecodeUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A ring buffer where every frame is decrypted, decoded and stored
 * Basically, you can put and get packets
 * @author bencall
 *
 */
public class AudioBuffer {
	// Constants - Should be somewhere else
	public static final int BUFFER_FRAMES = 512;	// Total buffer size (number of frame)
	public static final int START_FILL = 282;		// Alac will wait till there are START_FILL frames in buffer
	public static final int MAX_PACKET = 2048;		// Also in UDPListener (possible to merge it in one place?)

	// The Lock for the next Frame Method
	final Lock nextFrameLock = new ReentrantLock();
	final Condition nextFrameIsWaiting = nextFrameLock.newCondition();

	//the lock for the audiobuffer
	final Lock audioBufferLock = new ReentrantLock();
    // The array that represents the buffer
	private AudioData[] audioBuffer;
	
	// Can we read in buffer?
	private boolean synced = false;
	
	//Audio infos (rate, etc...)
	AudioSession session;
	
	// The seqnos at which we read and write
	//used to track overrunning the buffer
	//index of the buffer
	private int readIndex = 0;
	//seqno
	private int readSeqno = -1;
	private int writeIndex = 0;
	//The decoder stops 'cause the isn't enough packet. Waits till buffer is ok
	private boolean decoder_isStopped = false;
	
	// RSA-AES decryption infos
	private SecretKeySpec k;
	private Cipher c; 
	
	// Needed for asking missing packets
	AudioServer server;
	
	
	/**
	 * Instantiate the buffer
	 * @param session	audio infos
	 * @param server	whre to ask for resending missing packets
	 */
	public AudioBuffer(AudioSession session, AudioServer server){
		this.session = session;
		this.server = server;
		
		audioBuffer = new AudioData[BUFFER_FRAMES];
	}
		
	/**
	 * Sets the read index to the write index. All the data will be ignored.
	 * No audio more.
	 */
	public void flush(){
		readIndex = writeIndex;
	}
	
	/**
	 * Returns the next ready frame. If none, waiting for one
	 * @return the next frame
	 */
	public int[] getNextFrame() throws InterruptedException {
	    synchronized (lock) {	    	
			actualBufSize = writeIndex-readIndex;	// Packets in buffer
		    if(actualBufSize<0){	// If loop
		    	actualBufSize = 65536-readIndex+ writeIndex;
		    }
		    
			if(actualBufSize<1 || !synced){			// If no packets more or Not synced (flush: pause)
				if(synced){							// If it' because there is not enough packets
					System.err.println("Underrun!!! Not enough frames in buffer!");
				}
				
				try {
					// We say the decoder is stopped and we wait for signal
					System.err.println("Waiting");
					decoder_isStopped = true;
				    	lock.wait();
				    decoder_isStopped = false;
					System.err.println("re-starting");					
					readIndex++;	// We read next packet
					
					// Underrun: stream reset
					session.resetFilter();
				} catch (InterruptedException e) {
					throw e;
				}
				
				return null;
			} else {
				readSeqno = audioData.getSequenceNumber();
				return audioData.getData();
			}
		} catch (IndexOutOfBoundsException e) {
			// If it' because there is not enough packets
			if(synced){
				System.err.println("Underrun!!! Not enough frames in buffer!");
			}
			// We say the decoder is stopped and we wait for signal
			System.err.println("Waiting");
			decoder_isStopped = true;
			nextFrameLock.lock();
			try {
				nextFrameIsWaiting.await();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} finally {
				nextFrameLock.unlock();
			}
			decoder_isStopped = false;
			System.err.println("re-starting");
			readIndex++;	// We read next packet

			// Underrun: stream reset
			session.resetFilter();
			return null;
		}
	}
	
	
	/**
	 * Adds packet into the buffer
	 * @param seqno	seqno of the given packet. Used as index
	 * @param data
	 */
	public void putPacketInBuffer(int seqno, byte[] data){
	    // Ring buffer may be implemented in a Hashtable in java (simplier), but is it fast enough?		
		// We lock the thread
		synchronized(lock){

			if(!synced){
				writeIndex = seqno;
				readIndex = seqno;
				synced = true;
			}

			@SuppressWarnings("unused")
			int outputSize = 0;
			if (seqno == writeIndex){													// Packet we expected
				outputSize = this.alac_decode(data, audioBuffer[(seqno % BUFFER_FRAMES)].data);		// With (seqno % BUFFER_FRAMES) we loop from 0 to BUFFER_FRAMES
				audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
				writeIndex++;
			} else if(seqno > writeIndex){												// Too early, did we miss some packet between writeIndex and seqno?
				server.request_resend(writeIndex, seqno);
				outputSize = this.alac_decode(data, audioBuffer[(seqno % BUFFER_FRAMES)].data);
				audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
				writeIndex = seqno + 1;
			} else if(seqno > readIndex){												// readIndex < seqno < writeIndex not yet played but too late. Still ok
				outputSize = this.alac_decode(data, audioBuffer[(seqno % BUFFER_FRAMES)].data);
				audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
			} else {
				System.err.println("Late packet with seq. numb.: " + seqno);			// Really to late
			}
		}
		for (int i = (readIndex + 1) ; i < limit; i++) {
			AudioData audioData = audioBuffer[i];
			if (audioData.getSequenceNumber() > readSeqno)
				count++;
		}
		audioBufferLock.unlock();
		return count;
	}

	/**
	 * returns the next AudioData
	 * @return null if empty or an instance of AudioData
	 */
	private synchronized AudioData poll() throws IndexOutOfBoundsException{
		if (readIndex >= writeIndex)
			throw new IndexOutOfBoundsException();
		audioBufferLock.lock();
		AudioData audioData = audioBuffer[readIndex];
		audioBuffer[readIndex] = null;
		audioBufferLock.unlock();
		readIndex = increment(readIndex);
		return audioData;
	}

	/**
	 * increments the index to comply with the ringbuffer (start at 0 if index == BUFFER_FRAMES)
	 * @return the incremented integer
	 */
	private int increment(int index) {
		if (index < BUFFER_FRAMES) {
			return index +1;
		} else {
			return 0;
		}
	}
	
	
	/**
	 * Decrypt and decode the packet.
	 * @param data
	 * @param outbuffer the result
	 * @return
	 */
	private int alac_decode(byte[] data, int[] outbuffer){		
		byte[] packet = new byte[MAX_PACKET];
		
		// Init AES
		initAES();
		
	    int i;
	    for (i=0; i+16<=data.length; i += 16){
	    	// Decrypt
	    	this.decryptAES(data, i, 16, packet, i);
	    }
	    
	    // The rest of the packet is unencrypted
	    for (int k = 0; k<(data.length % 16); k++){
	    	packet[i+k] = data[i+k];
	    }
	    	    
	    int outputsize = 0;
	    outputsize = AlacDecodeUtils.decode_frame(session.getAlac(), packet, outbuffer, outputsize);

	    assert outputsize==session.getFrameSize()*4;						// FRAME_BYTES length
	    
		return outputsize;
	}
	
	
	/**
	 * Initiate the cipher	
	 */
	private void initAES(){
		// Init AES encryption
		try {
			k = new SecretKeySpec(session.getAESKEY(), "AES");
			c = Cipher.getInstance("AES/CBC/NoPadding");
			c.init(Cipher.DECRYPT_MODE, k, new IvParameterSpec(session.getAESIV()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Decrypt array from input offset with a length of inputlen and puts it in output at outputoffsest
	 * @param array
	 * @param inputOffset
	 * @param inputLen
	 * @param output
	 * @param outputOffset
	 * @return
	 */
	private int decryptAES(byte[] array, int inputOffset, int inputLen, byte[] output, int outputOffset){
		try{
	        return c.update(array, inputOffset, inputLen, output, outputOffset);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return -1;
	}
	

}
