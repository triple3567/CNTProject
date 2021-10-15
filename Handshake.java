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

    //TODO
    byte[] writeHandshake(){

        byte[] handshakeMessage = new byte[32];

        return void;
    }

    //TODO
    void readHandshake(byte[] b){

        return void;
    }
}