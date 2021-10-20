import java.lang.Object;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
public class Message {
    String messageLenght;
    byte[] messageLenghtBits;
    
    ByteBuffer bBufMessage = ByteBuffer.allocate(4);
    ByteBuffer bBufPayload;
    int mPayload;
    private static Charset charset = Charset.forName("US-ASCII");
    public enum MessageType { 
		choke,
		unchoke,
		interested,
		notInterested,
		have,
		bitfield,
		request,
		piece
	}
    MessageType msgType;
    Message(int m,int mType){
        switch(mType){
            case 0:
                msgType = MessageType.choke;
                break;
            case 1:
                msgType=MessageType.unchoke;
                break;
            case 2:
                msgType=MessageType.interested;
                break;
            case 3:
                msgType=MessageType.notInterested;
                break;
            case 4:
                msgType=MessageType.have;
                break;
            case 5:
                msgType=MessageType.bitfield;
                break;
            case 6:
                msgType=MessageType.request;
                break;
            case 7:
                msgType=MessageType.piece;
                break;
        }
        
        messageLenght = String.valueOf(m);
        System.out.println(messageLenght);
        messageLenghtBits = messageLenght.getBytes();
        bBufMessage.put(messageLenghtBits);
    }
    void getMessagePayload(){
        switch(msgType){
            case choke:
                System.out.println("choke");
                bBufPayload = ByteBuffer.allocate(0);
                break;
            case unchoke:
                System.out.println("unchoke");
                bBufPayload = ByteBuffer.allocate(0);
                break;
            case interested:
                System.out.println("interested");
                bBufPayload = ByteBuffer.allocate(0);
                break;
            case notInterested:
                System.out.println("notInterested");
                bBufPayload = ByteBuffer.allocate(0);
                break;
            case have:
                System.out.println("have");
                bBufPayload = ByteBuffer.allocate(4);
                break;
            case bitfield:
                System.out.println("bitfield");
                //TODO depends on peer
                break;
            case request:
                System.out.println("request");
                bBufPayload = ByteBuffer.allocate(4);
                break;
            case piece:
                System.out.println("piece");
                bBufPayload = ByteBuffer.allocate(4);
                break;
        }
    }
    void getByteBuffer(){
        String s;
        if(bbuf.hasArray()){
            s = new String(bbuf.array(), charset);
            System.out.println(s);
        }
    }

    public static void main(String[] args){
        Message msg = new Message(3,7);
        msg.getMessagePayload();
        msg.getByteBuffer();
    }
    
}
