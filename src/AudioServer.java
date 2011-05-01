/**
 * The class that process audio data
 * @author bencall
 *
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.beatofthedrum.alacdecoder.*;

public class AudioServer implements UDPDelegate{
	// Constantes
	public static final int BUFFER_FRAMES = 512;	// Total buffer size (number of frame)
	public static final int START_FILL = 282;		// Alac will wait till there are START_FILL frames in buffer
	public static final int MAX_PACKET = 2048;		// Also in UDPListener (possible to merge it in one place?)
	// Variables d'instances
	private int[]fmtp;	// Sound infos
	private int sampleSize; 
	private int frameSize;
	private AudioData[] audioBuffer;				// Buffer audio
	private boolean synced = false;
	private boolean decoder_isStopped = false;		//The decoder stops 'cause the isn't enough packet. Waits till buffer is full
	private int readIndex;							//	audioBuffer is a ring buffer. We write at writeindex(seqno) and we read at readindex(seqno).
	private int writeIndex;
	private int actualBufSize;
	private DatagramSocket sock, csock;
	// AES Keys
	private byte[] aesiv;
	private byte[] aeskey;
	private SecretKeySpec k;
	private Cipher c;    
	//Decoder
	private AlacFile alac;
	// Ports
	private InetAddress rtpClient;
	private int controlPort;
	// Mutex locks
    private final Lock lock = new ReentrantLock();    
    // Audio stuff
    biquadFilter bFilter;
    
    /**
     * Constructor. Initiate instances vars
     * @param aesiv
     * @param aeskey
     * @param fmtp
     * @param controlPort
     * @param timingPort
     */
	public AudioServer(byte[] aesiv, byte[] aeskey, int[] fmtp, int controlPort, int timingPort){		
		// Init instance var
		this.fmtp = fmtp;
		this.aesiv = aesiv;
		this.aeskey = aeskey;
		
		// Ports
		this.controlPort = controlPort;
		
		// Init functions
		this.initDecoder();
		this.initBuffer();
		this.initRTP();
		
		PCMPlayer player = new PCMPlayer(this);
		player.start();
	}
	
	public int getFrameSize(){
		return this.frameSize;
	}
	
	public int[] getNextFrame(){
	    synchronized (lock) {
			actualBufSize = writeIndex-readIndex;	// Packets in buffer
			System.err.println("FILL: "+actualBufSize);
			
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
					//TODO
					
					readIndex++;	// We read next packet
					
					// Underrun: stream reset
					bFilter = new biquadFilter(this.sampleSize, this.frameSize);	// New biquadFilter with default attribute (reset)
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				return null;
			}
			
			// Overrunning. Restart at a sane distance
		    if (actualBufSize >= BUFFER_FRAMES) {   // overrunning! uh-oh. restart at a sane distance
				System.err.println("Overrun!!! Too much frames in buffer!");
		        readIndex = writeIndex - START_FILL;
		    }
			// we get the value before the unlock ;-)
		    int read = readIndex;
		    readIndex++;
		     
		    actualBufSize = writeIndex-readIndex;
		    bFilter.update(actualBufSize); 
		    
		    AudioData buf = audioBuffer[read % BUFFER_FRAMES];
		    
		    if(!buf.ready){
		    	System.err.println("Missing Frame!");
		    	// Set to zero then
		    	for(int i=0; i<buf.data.length; i++){
		    		buf.data[i] = 0;
		    	}
		    }
		    buf.ready = false;
			return buf.data;

		}
	}
	
	
	public biquadFilter getFilter(){
		return bFilter;
	}
	
	/**
	 * Sets the packets as not ready. Audio thread will only listen to ready packets.
	 * No audio more.
	 */
	public void flush(){
		for (int i = 0; i< BUFFER_FRAMES; i++){
			audioBuffer[i].ready = false;
			synced = false;
		}
	}
	
	/**
	 * Return the server port for the bonjour service
	 * @return
	 */
	public int getServerPort() {
		return sock.getLocalPort();
	}
	
	/**
	 * Opens the sockets and begin listening
	 */
	private void initRTP(){
		int port = 6000;
		while(true){
			try {
				sock = new DatagramSocket(port);
				csock = new DatagramSocket(port+1);
			} catch (IOException e) {
				port = port + 2;
				continue;
			}
			break;
		}
		
		@SuppressWarnings("unused")
		UDPListener l1 = new UDPListener(sock, this);
		@SuppressWarnings("unused")
		UDPListener l2 = new UDPListener(csock, this);
	}
	
	/**
	 * Initiate the decoder
	 */
	private void initDecoder(){
		frameSize = fmtp[1];

		sampleSize = fmtp[3];
		if (sampleSize != 16){
			System.err.println("ERROR: 16 bits only!!!");
			return;
		}
		
		alac = AlacDecodeUtils.create_alac(sampleSize, 2);
		if (alac == null){
			System.err.println("ERROR: creating alac!!!");
			return;
		}
		alac.setinfo_max_samples_per_frame = frameSize;
		alac.setinfo_7a = fmtp[2];
		alac.setinfo_sample_size = sampleSize;
		alac.setinfo_rice_historymult = fmtp[4];
	    alac.setinfo_rice_initialhistory = fmtp[5];
	    alac.setinfo_rice_kmodifier = fmtp[6];
	    alac.setinfo_7f = fmtp[7];
	    alac.setinfo_80 = fmtp[8];
	    alac.setinfo_82 = fmtp[9];
	    alac.setinfo_86 = fmtp[10];
	    alac.setinfo_8a_rate = fmtp[11];
	}

	/**
	 * Initiate the ring buffer
	 */
	private void initBuffer(){
		audioBuffer = new AudioData[BUFFER_FRAMES];
		for (int i = 0; i< BUFFER_FRAMES; i++){
			audioBuffer[i] = new AudioData();
			audioBuffer[i].data = new int[4*(frameSize+3)];	// = OUTFRAME_BYTES = 4(frameSize+3)
		}
	}
	
	/**
	 * When udpListener gets a packet
	 */
	public void packetReceived(DatagramSocket socket, DatagramPacket packet) {
		this.rtpClient = packet.getAddress();		// The client address
		
		int type = packet.getData()[1] & ~0x80;
		if (type == 0x60 || type == 0x56) { 	// audio data / resend
			// Decale de 4 bytes supplementaires
			int off = 0;
			if(type==0x56){
				off = 4;
			}
			
			//seqno is on two byte
			int seqno = (int)((packet.getData()[2+off] & 0xff)*256 + (packet.getData()[3+off] & 0xff)); 
	
			// + les 12 (cfr. RFC RTP: champs a ignorer)
			byte[] pktp = new byte[packet.getLength() - off - 12];
			for(int i=0; i<pktp.length; i++){
				pktp[i] = packet.getData()[i+12+off];
			}
			
			this.putPacketInBuffer(seqno, pktp);
		}
	}
	
	/**
	 * Adds packet into the buffer
	 * @param seqno	seqno of the given packet. Used as index
	 * @param data
	 */
	private void putPacketInBuffer(int seqno, byte[] data){
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
				this.request_resend(writeIndex, seqno);
				outputSize = this.alac_decode(data, audioBuffer[(seqno % BUFFER_FRAMES)].data);
				audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
				writeIndex = seqno + 1;
			} else if(seqno > readIndex){												// readIndex < seqno < writeIndex not yet played but too late. Still ok
				outputSize = this.alac_decode(data, audioBuffer[(seqno % BUFFER_FRAMES)].data);
				audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
			} else {
				System.err.println("Late packet with seq. numb.: " + seqno);			// Really to late
			}
			
			// The number of packet in buffer
		    actualBufSize = writeIndex - readIndex;
		    
		    if(decoder_isStopped && actualBufSize > START_FILL){
			    System.err.println(seqno);
			    lock.notify();
			    //TODO
		    }
		}
	    
	}
	
	/**
	 * Ask iTunes to resend packet
	 * FUNCTIONAL??? NO PROOFS
	 * @param first
	 * @param last
	 */
	private void request_resend(int first, int last) {
		System.out.println("Resend Request: " + first + "::" + last);
		if(last<first){
			return;
		}
		
		byte request[] = new byte[8];
		request[0] = (byte)(0x80 & 0xFF);		// Header
		request[1] = (byte)((0x55|0x80) & 0xFF);
		request[2] = (byte)((1>>8) & 0xFF);			// Our seqnum
		request[3] = (byte)(1 & 0xFF);
		request[4] = (byte)((first>>8) & 0xFF);	// First
		request[5] = (byte)(first & 0xFF);
		request[6] = (byte)(((last-first+1)>>8) & 0xFF);	// Count
		request[7] = (byte)((last-first+1) & 0xFF);
		
		try {
			DatagramPacket temp = new DatagramPacket(request, request.length, rtpClient, controlPort);
			csock.send(temp);
			
		} catch (IOException e) {
			e.printStackTrace();
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
	    outputsize = AlacDecodeUtils.decode_frame(alac, packet, outbuffer, outputsize);

	    assert outputsize==frameSize*4;						// FRAME_BYTES length
	    
		return outputsize;
	}
	
	/**
	 * Initiate the cipher	
	 */
	private void initAES(){
		// Init AES encryption
		try {
			k = new SecretKeySpec(aeskey, "AES");
			c = Cipher.getInstance("AES/CBC/NoPadding");
			c.init(Cipher.DECRYPT_MODE, k, new IvParameterSpec(aesiv));
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
