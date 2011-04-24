/**
 * The class that process audio data
 * @author bencall
 *
 */

import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.Security;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;

import com.beatofthedrum.alacdecoder.*;

public class AudioServer implements UDPDelegate{
	// Constantes
	public static final int BUFFER_FRAMES = 512;	// Total buffer size (number of frame)
	public static final int START_FILL = 282;		// Alac will wait till there are START_FILL frames in buffer
	public static final int MAX_PACKET = 2048;		// Also in UDPListener (possible to merge it in one place?)

	// Variables d'instances
	int[]fmtp;								// Sound infos
	AudioData[] audioBuffer;				// Buffer audio
	boolean synced = false;
	boolean decoder_isStopped = false;		//The decoder stops 'cause the isn't enough packet. Waits till buffer is full
	int readIndex;							//	audioBuffer is a ring buffer. We write at write index and we read at readindex.
	int writeIndex;
	int actualBufSize;
	DatagramSocket sock, csock;
	// AES Keys
	byte[] aesiv;
	byte[] aeskey;
    SecretKeySpec k;
    Cipher c;
    
	//Decoder
	AlacFile alac;
	int frameSize;
	// Mutex locks
    private final Lock lock = new ReentrantLock();
    private final Condition bufferOkCond = lock.newCondition();

	public AudioServer(byte[] aesiv, byte[] aeskey, int[] fmtp, int controlPort, int timingPort){
		// Init instance var
		this.fmtp = fmtp;
		this.aesiv = aesiv;
		this.aeskey = aeskey;
		
		// Init functions
		this.initDecoder();
		this.initBuffer();
		this.initRTP();		
	}
	
	public int getServerPort() {
		return sock.getLocalPort();
	}
	
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
	
	private void initDecoder(){
		frameSize = fmtp[1];
//		int samplingRate = fmtp[11];
		int sampleSize = fmtp[3];
		if (sampleSize != 16){
			return;
		}
		
		alac = AlacDecodeUtils.create_alac(sampleSize, 2);
		alac.setinfo_max_samples_per_frame = 3;
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

	private void initBuffer(){
		audioBuffer = new AudioData[BUFFER_FRAMES];
		for (int i = 0; i< BUFFER_FRAMES; i++){
			audioBuffer[i] = new AudioData();
		}
	}
	
	public void packetReceived(DatagramSocket socket, DatagramPacket packet) {
		int type = packet.getData()[1] & ~0x80;
		if (type == 0x60 || type == 0x56) { 	// audio data / resend
			
			// Decale de 4 bytes supplementaires
			int off = 0;
			if(type==0x56){
				off = 4;
			}
			
			//seqno is on two byte
			int seqno = (int)((packet.getData()[2+off] & 0xff)*256 + (packet.getData()[3+off] & 0xff)); 
			System.out.println("SEQNO: "+ seqno);
	
			// + les 12 (cfr. RFC RTP: champs a ignorer)
			byte[] pktp = new byte[packet.getLength() - off - 12];
			for(int i=0; i<pktp.length; i++){
				pktp[i] = packet.getData()[i+12+off];
			}
			
			this.putPacketInBuffer(seqno, pktp);
		}
	}
	
	private void putPacketInBuffer(int seqno, byte[] data){
	    // Ring buffer may be implemented in a Hashtable in java (simplier), but is it fast enough?

		// We lock the thread
		lock.lock();
		
		if(!synced){
			writeIndex = seqno;
			readIndex = seqno;
			synced = true;
		}

		if (seqno == writeIndex){													// Packet we expected
			audioBuffer[(seqno % BUFFER_FRAMES)].data = this.alac_decode(data);		// With (seqno % BUFFER_FRAMES) we loop from 0 to BUFFER_FRAMES
			audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
			writeIndex++;
		} else if(seqno > writeIndex){												// Too early, did we miss some packet between writeIndex and seqno?
			this.request_resend(writeIndex, seqno);
			audioBuffer[(seqno % BUFFER_FRAMES)].data = this.alac_decode(data);
			audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
			writeIndex = seqno + 1;
		} else if(seqno > readIndex){												// readIndex < seqno < writeIndex not yet played but too late. Still ok
			audioBuffer[(seqno % BUFFER_FRAMES)].data = this.alac_decode(data);
			audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
		} else {
			System.err.println("Late packet with seq. numb.: " + seqno);			// Really to late
		}
		
		// The number of packet in buffer
	    actualBufSize = writeIndex - readIndex;
	    if(actualBufSize<0){	// If write head (tete d'ecriture) is at index 1 and read head (tete de lecture) at 510 for example.
	    	actualBufSize = actualBufSize + BUFFER_FRAMES;
	    }
	    
	    // We unlock the tread
	    lock.unlock();
	    
	    if(!decoder_isStopped && actualBufSize > START_FILL){
// TODO	    	bufferOkCond.signal();
	    }
	    
	}
	
	private void request_resend(int writeIndex2, int seqno) {
		// TODO Auto-generated method stub
		
	}

	private int[] alac_decode(byte[] data){		
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
		
	    int[] outbuffer = new int[(frameSize+3) * 4];		// FrameSize * 4
	    
	    int outputsize = 0;
	    outputsize = AlacDecodeUtils.decode_frame(alac, packet, outbuffer, outputsize);
	    
	    System.out.println("SIZE: " + outputsize + " vs " + frameSize*4);
		return outbuffer;
	}
	
		
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
	
	private int decryptAES(byte[] array, int inputOffset, int inputLen, byte[] output, int outputOffset){
		try{
	        return c.update(array, inputOffset, inputLen, output, outputOffset);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return -1;
	}
}
