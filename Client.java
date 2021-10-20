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
    
    Client(int p, String h, int myPeerID) {

        port = p;
        host = h;
        this.myPeerID = myPeerID;
        logger = new Logger(myPeerID);
    }
    
    public void run(){

        try{
            //create a socket to connect to the server
            requestSocket = new Socket(host, port);
            System.out.println("Connected to " + host + " in port " + port);

            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

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
        catch (ConnectException e) {
            System.err.println("Connection refused for host: " + host + ". You need to initiate a server first.");
        } 
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
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