import java.lang.Object;
import java.nio.ByteBuffer;
public class Message {
    int messageLenght;
    byte[] messageLenghtBits;
    
    ByteBuffer bbuf = ByteBuffer.allocate(4);
    int mPayload;
    enum messageType { 
		choke(0),
		unchoke(1),
		interested(2),
		notInterested(3),
		have(4),
		bitfield(5),
		request(6),
		piece(7)
        
        int value;
        messageType(int v){
            value = v;
        }
	}
    Message(int m){
        messageLenght = m;
        messageLenghtBits = messageLenght.getBytes();
        bbuf.wrap(messageLenghtBits);
    }
    void getMessagePayload(){
        if(messageType)
    }
}
