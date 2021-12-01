import java.lang.Object;
import java.nio.ByteBuffer;
import java.lang.Math;
import java.util.*;
//import java.nio.charset.Charset;
public class Message {

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
    int messageLength;
    
    Message(){}

    Message(int mType){    
        messageTypeNumber = mType;
        calculateMessageType(mType);
        payloadBits = new byte[0];
        messageLength = 1;
        bBufPayload = ByteBuffer.allocate(payloadBits.length);
    }
    Message(int mType, byte[] p){
        payloadBits = p;
        messageTypeNumber = mType;
        calculateMessageType(mType);
        messageLength = payloadBits.length + 1;
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

    byte[] writeMessage(){
        byte[] message = new byte[messageLength + 4];
        messageLengthBits = ByteBuffer.allocate(4).putInt(messageLength).array();
        byte[] payloadBytes = bBufPayload.wrap(payloadBits).array();
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
        for(int i = 0; i < payloadBytes.length; i++){
            message[count] = payloadBytes[i];
            count++;
        }

        return message;
    }

    void writeChoke(){
        Message message = new Message(0);
        message.writeMessage();
    }

    void writeUnchoke(){
        Message message = new Message(1);
        message.writeMessage();
    }

    void writeInterested(){
        Message message = new Message(2);
        message.writeMessage();
    }

    void writeNotInterested(){
        Message message = new Message(3);
        message.writeMessage();
    }

    void writeHave(int piece){
        byte[] bytearray = ByteBuffer.allocate(4).putInt(piece).array();
        Message message = new Message(4, bytearray);
        message.writeMessage();
    }

    byte[] writeBitfield(){

        byte[] message = new byte[payloadBits.length+5];

        ByteBuffer bb = ByteBuffer.allocate(4); 
        bb.putInt(messageTypeNumber); 

        byte messageTypeBit = bb.array()[3];
        messageLengthBits = ByteBuffer.allocate(4).putInt(message.length).array();

        byte[] payloadBytes = bBufPayload.wrap(payloadBits).array();
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

    void setBitfield(BitSet b){

        this.payloadBits = b.toByteArray();      
        this.bBufPayload = ByteBuffer.allocate(this.payloadBits.length);  
    }

    void writeRequest(int piece){
        byte[] bytearray = ByteBuffer.allocate(4).putInt(piece).array();
        Message message = new Message(6, bytearray);
        message.writeMessage();
    }

    void writePiece(int piece, byte[] payload){
        byte[] bytearray = new byte[payload.length + 4];
        byte[] pieceBytes = ByteBuffer.allocate(4).putInt(piece).array();

        int count = 0;

        for(int i = 0; i < pieceBytes.length; i++){
            bytearray[count] = pieceBytes[i];
            count++;
        }
        for(int i = 0; i < payload.length; i++){
            bytearray[count] = payload[i];
            count++;
        }

        Message message = new Message(7, bytearray);
        message.writeMessage();
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
        messageLength = ByteBuffer.wrap(Arrays.copyOfRange(b,0,4)).getInt();

        messageTypeNumber = ByteBuffer.wrap(Arrays.copyOfRange(b,5,6)).getInt();
        calculateMessageType(messageTypeNumber);
        if (messageLength > 1){
            payloadBits = Arrays.copyOfRange(b,6,b.length);
        }

        readMessage();

        return;
    }

    BitSet readBitfield(){
        return BitSet.valueOf(payloadBits);
    }

    int getPieceIndex() {
        byte[] pieceIndex = new byte[4];

        for (int i = 0; i < 4; i++) {
            pieceIndex[i] = payloadBits[i];
        }
        ByteBuffer temp = ByteBuffer.wrap(pieceIndex);

        return temp.getInt();
    }

    byte[] getPiece() {
        byte[] piece = new byte[messageLength - 5];

        for (int i = 0; i < messageLength - 5; i++) {
            piece[i] = payloadBits[i + 4];
        }

        return piece;
    }

    // int getMessageType() {
    //     return messageTypeNumber;
    // }
    // int getMessageType(byte[] message) {
    //     return message[4];
    // }
    // byte[] getPayload(byte[] message){
    //     byte[] payload = new byte[this.messageLength - 1];

    //     return payload;
    // }
}