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
    
    Message(){

    }

    Message(int mType){

        messageTypeNumber = mType;
        calculateMessageType(mType);
        calculateMessagePayload();
    }

    Message(int mType, byte[] p){

        payloadBits = p;
        messageTypeNumber = mType;
        calculateMessageType(mType);
        calculateMessagePayload();
    }

    void calculateMessagePayload(){
        switch(msgType){
            case choke:
                bBufPayload = ByteBuffer.allocate(0);
                break;
            case unchoke:
                bBufPayload = ByteBuffer.allocate(0);
                break;
            case interested:
                bBufPayload = ByteBuffer.allocate(0);
                break;
            case notInterested:
                bBufPayload = ByteBuffer.allocate(0);
                break;
            case have:
                bBufPayload = ByteBuffer.allocate(4);
                break;
            case bitfield:
                break;
            case request:
                bBufPayload = ByteBuffer.allocate(4);
                break;
            case piece:
                bBufPayload = ByteBuffer.allocate(4);
                break;
        }
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
        switch (msgType) {
            case choke:
                return writeChoke();
            case unchoke:
                return writeUnchoke();
            case interested:
                return writeInterested();
            case notInterested:
                return writeNotInterested();
            case have:
                return writeHave();
            case bitfield:
                return writeBitfield();
            case request:
                return writeRequest();
            case piece:
                return writePiece();
            default:
                return null;
        }
    }

    byte[] writeChoke(){
        byte[] message = new byte[5];
        byte[] messageTypeBit =  ByteBuffer.allocate(1).putInt(messageTypeNumber).array();
        messageLengthBits = ByteBuffer.allocate(4).putInt(message.length).array();

        int count = 0;

        for(int i = 0; i < messageLengthBits.length; i++){
            message[count] = messageLengthBits[i];
            count++;
        }
        for(int i = 0; i < messageTypeBit.length; i++){
            message[count] = messageTypeBit[i];
            count++;
        }

        return message;
    }

    byte[] writeUnchoke(){
        byte[] message = new byte[5];
        byte[] messageTypeBit =  ByteBuffer.allocate(1).putInt(messageTypeNumber).array();
        messageLengthBits = ByteBuffer.allocate(4).putInt(message.length).array();

        int count = 0;

        for(int i = 0; i < messageLengthBits.length; i++){
            message[count] = messageLengthBits[i];
            count++;
        }
        for(int i = 0; i < messageTypeBit.length; i++){
            message[count] = messageTypeBit[i];
            count++;
        }
        return message;
    }

    //done
    byte[] writeInterested(){
        byte[] message = new byte[5];

        messageLengthBits = ByteBuffer.allocate(4).putInt(0).array();

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
        
        return message;
    }

    //done
    byte[] writeNotInterested(){
        byte[] message = new byte[5];

        messageLengthBits = ByteBuffer.allocate(4).putInt(0).array();

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

        return message;
    }

    byte[] writeHave(){
        byte[] message = new byte[9];
        byte[] messageTypeBit =  ByteBuffer.allocate(1).putInt(messageTypeNumber).array();
        messageLengthBits = ByteBuffer.allocate(4).putInt(message.length).array();
        byte[] payloadBytes = bBufPayload.wrap(payloadBits).array();

        int count = 0;

        for(int i = 0; i < messageLengthBits.length; i++){
            message[count] = messageLengthBits[i];
            count++;
        }
        for(int i = 0; i < messageTypeBit.length; i++){
            message[count] = messageTypeBit[i];
            count++;
        }
        for(int i = 0; i < payloadBytes.length; i++){
            message[count] = payloadBytes[i];
            count++;
        }
        return message;
    }
    
    //done
    void setBitfield(BitSet b){

        this.payloadBits = b.toByteArray();      
        this.bBufPayload = ByteBuffer.allocate(this.payloadBits.length);  
    }
    
    //done
    byte[] writeBitfield(){

        byte[] message = new byte[payloadBits.length+5];

        messageLengthBits = ByteBuffer.allocate(4).putInt(payloadBits.length).array();

        ByteBuffer bb = ByteBuffer.allocate(4); 
        bb.putInt(messageTypeNumber); 
        byte messageTypeBit = bb.array()[3];

        byte[] payloadBytes = ByteBuffer.wrap(payloadBits).array();
        int count = 0;

        for(int i = 0; i < messageLengthBits.length; i++){
            message[count] = messageLengthBits[i];
            count++;
        }
        
        message[count] = messageTypeBit;
        count++;
        
        for(int i = 0; i < payloadBytes.length; i++){
            message[count] = payloadBytes[i];
            count++;
        }
        return message;
    }

    //done
    void readBitfield(){

        if(payloadBits != null){
        
            bitfieldPayload = BitSet.valueOf(payloadBits);
        }
    }

    byte[] writeRequest(){
        byte[] message = new byte[9];
        byte[] messageTypeBit =  ByteBuffer.allocate(1).putInt(messageTypeNumber).array();
        messageLengthBits = ByteBuffer.allocate(4).putInt(message.length).array();
        byte[] payloadBytes = bBufPayload.wrap(payloadBits).array();

        int count = 0;

        for(int i = 0; i < messageLengthBits.length; i++){
            message[count] = messageLengthBits[i];
            count++;
        }
        for(int i = 0; i < messageTypeBit.length; i++){
            message[count] = messageTypeBit[i];
            count++;
        }
        for(int i = 0; i < payloadBytes.length; i++){
            message[count] = payloadBytes[i];
            count++;
        }
        return message;
    }

    byte[] writePiece(){
        byte[] message = new byte[9];
        byte[] messageTypeBit =  ByteBuffer.allocate(1).putInt(messageTypeNumber).array();
        messageLengthBits = ByteBuffer.allocate(4).putInt(message.length).array();
        byte[] payloadBytes = bBufPayload.wrap(payloadBits).array();

        int count = 0;

        for(int i = 0; i < messageLengthBits.length; i++){
            message[count] = messageLengthBits[i];
            count++;
        }
        for(int i = 0; i < messageTypeBit.length; i++){
            message[count] = messageTypeBit[i];
            count++;
        }
        for(int i = 0; i < payloadBytes.length; i++){
            message[count] = payloadBytes[i];
            count++;
        }
        return message;
    }

    void readMessage(){
        switch (msgType){
            
            case choke:
                break;
            case unchoke:
                break;
            case interested:
                break;
            case notInterested:
                break;
            case have:
                break;
            case bitfield:
                readBitfield();
            case request:
                break;
            case piece:
                break;
        }

        return;
    }

    void readMessage(byte[] b){
        
        messageLengthBits = Arrays.copyOfRange(b,0,4);
        int messageLength = ByteBuffer.wrap(Arrays.copyOfRange(b,0,4)).getInt();

        messageTypeNumber = ByteBuffer.wrap(Arrays.copyOfRange(b,5,6)).getInt();
        calculateMessageType(messageTypeNumber);
        if (messageLength > 1){
            payloadBits = Arrays.copyOfRange(b,6,b.length);
        }

        switch (msgType){
            
            case choke:
                break;
            case unchoke:
                break;
            case interested:
                break;
            case notInterested:
                break;
            case have:
                break;
            case bitfield:
                readBitfield();
            case request:
                break;
            case piece:
                break;
        }

        return;
    }



    

}
