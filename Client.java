import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
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
    
    Client(int p, String h, int myPeerID, Map<Integer, Peer.PeerInfo> peerInfo, int peerID) {

        port = p;
        host = h;
        this.myPeerID = myPeerID;
        this.peerID = peerID;
        this.peerInfo = peerInfo;
        logger = new Logger(myPeerID);
        running.set(true);
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

            
        }
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