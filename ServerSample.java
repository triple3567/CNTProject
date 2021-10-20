import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Server{

    private static int sPort;  //The server will be listening on this port number


    public static void main(String[] args) throws Exception {

        sPort = Integer.parseInt(args[0]);        
        int peerId = Integer.parseInt(args[1]);

        System.out.println("The server is running.");
        ServerSocket listener = new ServerSocket(sPort);

        try {

            while(true) {

                new Handler(listener.accept(),peerId).start();
                System.out.println("Client "  + peerId + " is connected!");
                //peerId++;
            }
        } finally {

            listener.close();
        } 
    }
    
    /*** A handler thread class.  Handlers are spawned from the listening
    * loop and are responsible for dealing with a single client's requests.
    */
    private static class Handler extends Thread {
        private Handshake message;    //message received from the client
        private Socket connection;
        private ObjectInputStream in;   //stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private int no; //The index number of the client
        private int id; //ID of the client
        
        public Handler(Socket connection, int no) {
            this.connection = connection;
            this.no = no;
        }        
        
        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                Handshake handshake = new Handshake(sPort);
                try{
                    //send MESSAGE back to the client
                    sendMessage(handshake);
                    //receive the message sent from the client
                    message = (Handshake)in.readObject();
                    //show the message to the user
                    System.out.println("Receive message: " + message.getPeerId() + " from client " + no);
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }
        //send a message to the output stream
        public void sendMessage(Handshake handshake){
            try{
                out.writeObject(handshake);
                System.out.println("Send message: " + handshake.getPeerId() + " to Client " + no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
}