import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.time.Period;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.plaf.basic.BasicLookAndFeel;

public class Client extends Thread{
    Socket requestSocket;           //socket connect to the server
    ObjectOutputStream out;         //stream write to the socket
    ObjectInputStream in;          //stream read from the socket
    String message;                //message send to the server
    Handshake MESSAGE;                //capitalized message read from the server
    int port;
    String host;
    int myPeerID;
    byte[] buffer;
    int peerID;     //id of server
    Logger logger;
    Map<Integer, Peer.PeerInfo> peerInfo;
    private final AtomicBoolean running = new AtomicBoolean(false);
    boolean currChoked;
    
    Client(int p, String h, int myPeerID, Map<Integer, Peer.PeerInfo> peerInfo, int peerID) {

        port = p;
        host = h;
        this.myPeerID = myPeerID;
        this.peerID = peerID;
        this.peerInfo = peerInfo;
        logger = new Logger(myPeerID);
        running.set(true);
        currChoked = true;
    }
    
    public void interrupt(){
        running.set(false);
    }

    public void run(){

        connectToServer();

        try{

            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            doHandshaking();

            //send bitfield message
            sendBitfieldMessage();

            //process interested or not interested
            processInterestedOrNotInterested();

            //main loop
            clientLoop();
            

        }
        catch (Exception e){

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString(); // stack trace as a string
            logger.writeLog("[ERROR] Peer [" + myPeerID + "]" + sStackTrace);
        }
        finally{
            //Close connections
            try{

                if(in != null){
                    
                    in.close();
                }
                
                if(out != null){
                    
                    out.close();
                }

                if(requestSocket != null){
                    
                    requestSocket.close();
                }
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
            catch(Exception e){
                e.printStackTrace();
            }

        }
    }

    void clientLoop(){

        while (running.get()){

            if(peerInfo.get(peerID).choked){

                if(currChoked == false){
                    sendChoke();

                }

                readBuffer();
                sendNewHave();

            }
            else{

                if(currChoked == true){
                    sendUnchoke();
                }

                readBuffer();
                sendNewHave();
                
            }
        }
    }

    void sendNewHave(){

        while(!peerInfo.get(peerID).freshPieces.empty()){

            int piece = peerInfo.get(peerID).freshPieces.pop();

            Message m = new Message(4);
            m.setHavePayload(piece);
            byte[] mOut = m.writeMessage();

            sendMessage(mOut);

        }

        return;
    }

    void readBuffer(){

        try{
            if (in.available() >= 5){

                //read 4 bytes for message length
                int messageLength = in.readInt();

                //read 1 byte for type
                int messageType = in.readUnsignedByte();

                //if message length > 0, read message payload
                byte[] messagePayload = null;

                if(messageLength > 0){

                    messagePayload = new byte[messageLength];
                    in.readFully(messagePayload, 0, messageLength);
                }

                switch(messageType){

                    case 2:
                        processInterested();
                        logger.writeLog("Peer [" + myPeerID + "] received the 'interested' message from [" + peerID + "]");
                        break;
                    case 3:
                        processUninterested();
                        logger.writeLog("Peer [" + myPeerID + "] received the 'not interested' message from [" + peerID + "]");
                        break;
                    case 6:
                        processRequest(messagePayload);
                        break;
                }
                
            }
        }
        catch(Exception e){

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            logger.writeLog("[ERROR] Peer [" + myPeerID + "] in Client read loop with connection to peer [" + peerID + "] " + sStackTrace);
            e.printStackTrace();
        }

        return;
    }

    void processInterested(){

        peerInfo.get(peerID).interested = true;
        return;
    }

    void processUninterested(){

        peerInfo.get(peerID).interested = false;
        return;
    }

    void processRequest(byte[] b){

        int pieceRequested = Message.readRequestPayload(b);

        logger.writeLog("Peer [" + myPeerID + "] received the 'request' message from [" + peerID + "] for peice [" + pieceRequested + "]");

        sendPiece(pieceRequested);
        return;
    }

    void sendPiece(int pieceRequested){

        int size = peerInfo.get(myPeerID).pieceSize;

        byte[] fileCopy = Arrays.copyOf(peerInfo.get(myPeerID).fileBytes, peerInfo.get(myPeerID).fileBytes.length);

        int pieceStartIndex = pieceRequested * size;
        int pieceEndIndex = pieceStartIndex + size;

        byte[] b = new byte[size];
        int count = 0;

        for (int i = pieceStartIndex; i < pieceEndIndex; i++){
            b[count] = fileCopy[i];
            count++;
        }

        Message m = new Message(7);
        m.setPiecePayload(b);
        byte[] mOut = m.writeMessage();

        sendMessage(mOut);

        return;
    }

    void sendChoke(){

        Message m = new Message(0);
        byte[] mOut = m.writeMessage();
        sendMessage(mOut);
        currChoked = true;

        return;
    }

    void sendUnchoke(){

        Message m = new Message(1);
        byte[] mOut = m.writeMessage();
        sendMessage(mOut);
        currChoked = false;

        return;
    }

    void connectToServer(){

        //create a socket to connects to the server, restarts if fails
        while (true){
            try{
                TimeUnit.SECONDS.sleep(5);
                
                requestSocket = new Socket(host, port);
                break;
            }
            catch(Exception e){
                logger.writeLog("Peer [" + myPeerID +"] has failed to connect to Peer [" + peerID + "]. retrying in 5 seconds...");
            }
        }
    }

    void doHandshaking(){
        try{

            //Do Handshaking
            //Receive the upperCase sentence from the server
            Handshake handshake = new Handshake();
            buffer = new byte[32];
            in.readFully(buffer, 0, 32);

            System.out.println(Arrays.toString(buffer));

            handshake.readHandshake(buffer);
            
            peerID = handshake.getPeerID();

            //show the message to the user
            System.out.println("Receive handshake: " + peerID);
            
            //Send the sentence to the server
            handshake = new Handshake();
            handshake.setPeerID(myPeerID);
            sendMessage(handshake.writeHandshake());

            logger.writeLog("Peer [" + myPeerID + "] makes a connection to Peer [" + peerID + "]");

        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }

    }

    void sendBitfieldMessage(){

        try{


            Message message = new Message(5);
            message.setBitfieldPayload(peerInfo.get(myPeerID).bitset);
            byte[] messageOut = message.writeMessage();
            sendMessage(messageOut);


        }
        catch(Exception e){
            
            
            e.printStackTrace();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString(); // stack trace as a string
            logger.writeLog("[ERROR] Peer [" + myPeerID + "]" + sStackTrace);
        }
    }

    void processInterestedOrNotInterested(){

        try{

            //wait for 4 bytes of data to be in input stream
            while (in.available() < 5){

            }

            int messageLength = in.readInt();
            int messageType = in.readUnsignedByte();

            Message m = new Message(messageType);

            if(m.msgType == Message.MessageType.interested){

                peerInfo.get(peerID).interested = true;
                logger.writeLog("Peer [" + myPeerID + "] received the 'interested' message from [" + peerID + "]");
            }
            else if (m.msgType == Message.MessageType.notInterested){

                peerInfo.get(peerID).interested = false;
                logger.writeLog("Peer [" + myPeerID + "] received the 'not interested' message from [" + peerID + "]");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        
    }

    //send a message to the output stream
    void sendMessage(byte[] message){
        try{
            //stream write the message
            out.write(message);
            out.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    
}