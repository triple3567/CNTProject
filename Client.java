import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    boolean sentChokeStatus;
    boolean clientStatus;

    Client(int p, String h, int myPeerID, Map<Integer, Peer.PeerInfo> peerInfo, int peerID) {

        port = p;
        host = h;
        this.myPeerID = myPeerID;
        this.peerID = peerID;
        this.peerInfo = peerInfo;
        logger = new Logger(myPeerID);
        clientStatus = true;
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
            mainLoop();
            
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
                logger.writeLog("Peer[" + myPeerID + "] closing Client with peer [" + peerID +"]");
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
            catch(Exception e){
                e.printStackTrace();
            }

        }
    }
    void mainLoop(){
    
        while(true){
            if (clientStatus == false){
                break;
            }
            if(peerInfo.get(peerID).choked == true){
                if(sentChokeStatus == false){
                    Message messageC = new Message(0);
                    byte[] messageOut = messageC.writeMessage();
                    sendMessage(messageOut);
                    sentChokeStatus = true;
                }
            }
            else{
                if(sentChokeStatus == true){
                    Message messageU = new Message(1);
                    byte[] messageOut = messageU.writeMessage();
                    sendMessage(messageOut);
                    sentChokeStatus = true;
                }
            }
            try{
                if (in.available() < 4){
                    continue;
                }
                //read 4 bytes for message length
                int messageLength = in.readInt();

                //read 1 byte for type
                int messageType = in.readUnsignedByte();

                //if message length > 0, read message payload
                byte[] messagePayload = null;

                if(messageLength > 0){

                    messagePayload = new byte[messageLength];
                    in.read(messagePayload, 0, messageLength);
                }
                switch(messageType){
                    case 0:
                        logger.writeLog("Choke Mssg Recieved by Client of" + peerID);
                        break;
                    case 1:
                        logger.writeLog("UnChoke Mssg Recieved by Client of" + peerID);
                        break;
                    case 2:
                        logger.writeLog("Peer[" + myPeerID + "] received 'Interested' message from [" + peerID +"]");
                        peerInfo.get(peerID).interested = true;
                        break;
                    case 3:
                        logger.writeLog("Peer[" + myPeerID + "] received 'Not Interested' message from [" + peerID +"]");
                        peerInfo.get(peerID).interested = false;
                        break;
                    case 4:
                        logger.writeLog("Have Mssg Recieved by Client of" + peerID);
                        break;
                    case 5:
                        logger.writeLog("Bitfield Mssg Recieved by Client of" + peerID);
                        break;
                    case 6:
                        logger.writeLog("Peer[" + myPeerID + "] received 'Request' message from [" + peerID +"]");
                        sendRequestedPiece(Message.readRequestPayload(messagePayload));
                        break;
                    case 7:
                        logger.writeLog("Piece Mssg Recieved by Client of" + peerID);
                        break;
                }
            }
            catch(Exception e){
                e.printStackTrace();
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String sStackTrace = sw.toString(); // stack trace as a string
                logger.writeLog("[ERROR] Peer [" + myPeerID + "]" + sStackTrace);
            }
            
            while(peerInfo.get(myPeerID).freshPieces.empty() != false){
                int p = (int)peerInfo.get(myPeerID).freshPieces.pop();
                Message messageH = new Message(4);
                messageH.setHavePayload(p);
                byte[] messageOut = messageH.writeMessage();
                sendMessage(messageOut);
                
            }
        }
        
    
    
    }
    void sendRequestedPiece(int p){
        //TODO
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
            while (in.available() < 4){

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