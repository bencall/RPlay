/**
 * The class that process audio data
 * @author bencall
 *
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import com.beatofthedrum.alacdecoder.*;

public class AudioServer implements UDPDelegate{
	// Constantes
	public static final int BUFFER_FRAMES = 512;

	// Variables d'instances
	int[]fmtp;					// Sound infos
	AudioData[] audioBuffer;	// Buffer audio
	DatagramSocket sock, csock;
	
	boolean ab_synced = false;	// Is audio synced?
	int ab_read, ab_write;
	
	public AudioServer(byte[] aesiv, byte[] aeskey, int[] fmtp, int controlPort, int timingPort){
		this.fmtp = fmtp;
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
		
		UDPListener l1 = new UDPListener(sock, this);
		UDPListener l2 = new UDPListener(csock, this);
	}
	
	private void initDecoder(){
//		int frameSize = fmtp[1];
//		int samplingRate = fmtp[11];
		int sampleSize = fmtp[3];
		if (sampleSize != 16){
			return;
		}
		
		AlacFile alac = AlacDecodeUtils.create_alac(sampleSize, 2);
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
	}
	
	public void packetReceived(DatagramSocket socket, DatagramPacket packet) {
		int type = packet.getData()[1] & ~0x80;
		if (type == 0x60 || type == 0x56) { 	// audio data / resend
			byte[] pktp = packet.getData();
			if(type==0x56){
				for(int i=0; i<pktp.length-4; i++){
					pktp[i] = packet.getData()[i+4];
				}
			}
			
			//seqno is on two byte
			int seqno = ((int)pktp[2] << 8) + (pktp[3] & 0xff); 
			this.putPacketInBuffer(seqno, pktp);
			
		}
	}
	
	private void putPacketInBuffer(int seqno, byte[] data){
	    if (!ab_synced) {
	        ab_write = seqno;
	        ab_read = seqno - 1;
	        ab_synced = true;				
	    }
	    
	    
	}
	
	private void alac_decrypt(){
//	    unsigned char packet[MAX_PACKET];
//	    assert(len<=MAX_PACKET);
//
//	    unsigned char iv[16];
//	    int i;
//	    memcpy(iv, aesiv, sizeof(iv));
//	    for (i=0; i+16<=len; i += 16)
//	        AES_cbc_encrypt((unsigned char*)buf+i, packet+i, 0x10, &aes, iv, AES_DECRYPT);
//	    if (len & 0xf)
//	        memcpy(packet+i, buf+i, len & 0xf);
//
//	    int outsize;
//
//	    decode_frame(decoder_info, packet, dest, &outsize);
//
//	    assert(outsize == FRAME_BYTES);
	}
}
