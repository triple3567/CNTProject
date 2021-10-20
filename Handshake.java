import java.nio.ByteBuffer;
import java.util.*;

public class Handshake{

    String handshakeHeaderString = "P2PFILESHARINGPROJ";
    byte[] handshakeHeader = handshakeHeaderString.getBytes();
    int peerID = -1;

    int getPeerID(){
        return peerID;
    }

    void setPeerID(int p){
        peerID = p;
    }

    //Writes a handshake with the current peerID, Returns the byte array representation of the handshake
    byte[] writeHandshake(){

        byte[] handshakeMessage = new byte[32];
        byte[] peerIDBytes = ByteBuffer.allocate(4).putInt(peerID).array();;

        int count = 0;

        for(int i = 0; i < handshakeHeader.length; i++){

            handshakeMessage[count] = handshakeHeader[i];
            count++;
        }

        for(int i = 0; i < 10; i++){

            handshakeMessage[count] = 0;
            count++;
        }

        for(int i = 0; i < peerIDBytes.length; i++){

            handshakeMessage[count] = peerIDBytes[i];
            count++;
        }

        return handshakeMessage;
    }

    //reads a handshake and stores the peer ID in the local variable peerID. 
    void readHandshake(byte[] b){

        String s = new String(Arrays.copyOfRange(b, 0, 18));

        if(!s.equals(handshakeHeaderString)){

            System.out.println("Invalid Handshake Read");

        }
        peerID = ByteBuffer.wrap(Arrays.copyOfRange(b, 28, 32)).getInt();
        

    }

}