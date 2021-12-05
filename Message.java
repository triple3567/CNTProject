import java.lang.Object;
import java.nio.ByteBuffer;
import java.lang.Math;
import java.util.*;
//import java.nio.charset.Charset;
public class Message {

    //private static Charset charset = Charset.forName("US-ASCII");
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

    byte[] messageLengthBits;
    ByteBuffer bBufPayload;
    int messageTypeNumber;
    byte[] payloadBits;
    MessageType msgType;
    BitSet bitfieldPayload = null;
    int messageLength;
    
    Message(){

    }

    Message(int mType){

        messageTypeNumber = mType;
        calculateMessageType(mType);
        payloadBits = new byte[0];
        messageLength = 0;
        bBufPayload = ByteBuffer.allocate(payloadBits.length);
    }

    Message(int mType, byte[] p){

        payloadBits = p;
        messageTypeNumber = mType;
        calculateMessageType(mType);
        messageLength = payloadBits.length;
        bBufPayload = ByteBuffer.allocate(payloadBits.length);
    }

    void calculateMessageType(int mType){
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
    }

    byte[] writeMessage() {
        
        byte[] message = new byte[messageLength + 5];
        messageLengthBits = ByteBuffer.allocate(4).putInt(messageLength).array();
        ByteBuffer bb = ByteBuffer.allocate(4); 
        bb.putInt(messageTypeNumber); 
        byte messageTypeBit = bb.array()[3];

        int count = 0;

        for(int i = 0; i < messageLengthBits.length; i++){
            message[count] = messageLengthBits[i];
            count++;
        }

        message[count] = messageTypeBit;
        count++;

        for(int i = 0; i < payloadBits.length; i++){
            message[count] = payloadBits[i];
            count++;
        }

        return message;
    }

    void setHavePayload(int piece){

        messageLength = 4;
        payloadBits = ByteBuffer.allocate(4).putInt(piece).array();
        bBufPayload = ByteBuffer.allocate(payloadBits.length);

    }
    
    void setBitfieldPayload(BitSet b){

        payloadBits = b.toByteArray();      
        bBufPayload = ByteBuffer.allocate(payloadBits.length);
        messageLength = payloadBits.length;  
    }

    void setRequestPayload(int piece){

        messageLength = 4;
        payloadBits = ByteBuffer.allocate(4).putInt(piece).array();
        bBufPayload = ByteBuffer.allocate(payloadBits.length);
    }

    void setPiecePayload(byte[] piece){

        messageLength = piece.length;
        payloadBits = piece;
        bBufPayload = ByteBuffer.allocate(payloadBits.length);

    }
    
    static BitSet readBitfieldPayload(byte[] p){

        if (p == null) return null;
        return BitSet.valueOf(p);

    }

    static int readHavePayload(byte[] p){

        ByteBuffer wrapped = ByteBuffer.wrap(p);
        return wrapped.getInt();
    }

    static int readRequestPayload(byte[] p){

        ByteBuffer wrapped = ByteBuffer.wrap(p);
        return wrapped.getInt();
    }

}
