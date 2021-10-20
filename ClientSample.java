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
    
    Client(int p, String h) {
        port = p;
        host = h;
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
            Handshake handshake = new Handshake(port);
   
            //Receive the upperCase sentence from the server
            MESSAGE = (Handshake)in.readObject();

            //show the message to the user
            System.out.println("Receive message: " + MESSAGE.getPeerId());
            
            //Send the sentence to the server
            sendMessage(handshake);

        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        } 
        catch ( ClassNotFoundException e ) {
            System.err.println("Class not found");
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
                in.close();
                out.close();
                requestSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
    //send a message to the output stream
    void sendMessage(Handshake msg){
        try{
            //stream write the message
            out.writeObject(msg);
            out.flush();
            System.out.println("Send message: " + msg.getPeerId() + " to Server");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    //main method
    public static void main(String args[]){
        Client client = new Client(Integer.parseInt(args[0]), args[1]);
        client.run();
    }
}