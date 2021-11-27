import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

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
    
    Client(int p, String h, int myPeerID, Map<Integer, Peer.PeerInfo> peerInfo) {

        port = p;
        host = h;
        this.myPeerID = myPeerID;
        this.peerInfo = peerInfo;
        logger = new Logger(myPeerID);
    }
    
    public void run(){

        while (true){

            try{
                //create a socket to connect to the server
                requestSocket = new Socket(host, port);
                System.out.println("Connected to " + host + " in port " + port);

                //initialize inputStream and outputStream
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(requestSocket.getInputStream());

                doHandshaking();

                //send bitfield message
                sendBitfieldMessage();


            }
            catch (Exception e){
                logger.writeLog("Peer [" + myPeerID +"] has failed to connect to Peer [" + peerID + "]. retrying...");
                continue;
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

                break;
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
            message.setBitfield(peerInfo.get(myPeerID).bitset);
            byte[] messageOut = message.writeBitfield();
            sendMessage(messageOut);

            logger.writeLog("[INFO] Peer [" + myPeerID + "] sent a bitfield message to Peer [" + peerID + "]");
           

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