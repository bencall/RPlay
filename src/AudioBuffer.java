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
    public int[] getNextFrame() {
        try {
            AudioData audioData = poll();
            if (audioData == null) {
                //missing in combat!
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

        int[] decoded = new int[session.OUTFRAME_BYTES()];
        int outputSize = this.alac_decode(data, decoded);
        AudioData audioData = new AudioData(decoded, seqno);

        try {
            put(seqno, audioData);
        } catch (IndexOutOfBoundsException e) {
            // Really to late
            System.err.println("Late packet with seq. numb.: " + seqno);
        }

        if(decoder_isStopped && calculateActualBufferSize() > START_FILL){
            nextFrameLock.lock();
            nextFrameIsWaiting.signal();
            nextFrameLock.unlock();
        }
    }

    /**
     * puts an Audio-Data into the array
     * @param seqno the Sequence-Number, used as instance
     * @param audioData the audio-Data to put
     * @throws java.lang.IndexOutOfBoundsException if the package is was already read
     */
    private void put(int seqno, AudioData audioData) {
        if (seqno < readSeqno) {
            throw new IndexOutOfBoundsException();
        }
        int index = seqno % BUFFER_FRAMES;
        audioBufferLock.lock();
        audioBuffer[index] = audioData;
        audioBufferLock.unlock();
        if (index > writeIndex)
            writeIndex = index;
    }

    /**
     * calculates how many packages got buffered
     * @return the number of packages buffered
     */
    private int calculateActualBufferSize() {
        int count = 0;
        audioBufferLock.lock();
        int limit = writeIndex;
        if (writeIndex < readIndex) {
            limit = BUFFER_FRAMES;
            for (int i = 0; i < writeIndex; i++) {
                AudioData audioData = audioBuffer[i];
                if (audioData.getSequenceNumber() > readSeqno)
                    count++;
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
