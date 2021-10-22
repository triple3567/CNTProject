import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Server extends Thread{

    private int sPort;
    public int myPeerID;
    public int numConnections = 0;
    public List<Handler> handlers;
    public String host;
    public Logger logger;

    Server(int sPort, int myPeerID){

        this.sPort = sPort;
        this.myPeerID = myPeerID;
        this.handlers = new ArrayList<>();
        logger = new Logger(myPeerID);
    }


    //Creates a new thread for Server. Will loop on listening for more connections. 
    //TODO create exit condition so server will close when all connections end
    public void run() {

        try{
        
            ServerSocket listener = new ServerSocket(sPort);
            System.out.println("Server started. Listening on port: " + sPort + " at host: " + java.net.InetAddress.getLocalHost().getHostName());

            try {

                //Add a new listener thread to handlers array when a connection is established. 
                while(true) {

                    handlers.add(new Handler(listener.accept(), numConnections));
                    handlers.get(numConnections).start();
                    System.out.println("[SERVER] Connection " + numConnections + " established");
                    numConnections++;
                }
            }
            catch(Exception e){

                e.printStackTrace();
            }
            finally {

                listener.close();
            }

        }
        catch(Exception e){

            e.printStackTrace();
        }
    }

    private class Handler extends Thread{

        private Handshake message;    //handshake message received from the client
        private Socket connection;
        private ObjectInputStream in;   //stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
        private byte[] buffer;
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

                doHandshaking();
                
            }
            catch(IOException e){
                System.out.println("Disconnect with Client " + no);
            }
            finally{

            }

        }

        void doHandshaking(){

            Handshake handshake = new Handshake();
            handshake.setPeerID(myPeerID);

            //try handshaking
                try{

                    //send MESSAGE back to the client
                    System.out.println(Arrays.toString(handshake.writeHandshake()));
                    sendMessage(handshake.writeHandshake());

                    //receive the message sent from the client
                    buffer = new byte[32];
                    in.readFully(buffer, 0, 32);
                    
                    System.out.println(Arrays.toString(buffer));

                    handshake = new Handshake();
                    handshake.readHandshake(buffer);
                    id = handshake.getPeerID();


                    //show the message to the user
                    System.out.println("Receive handshake: " + handshake.getPeerID() + " from client " + no);

                    logger.writeLog("Peer [" + myPeerID + "] is connected from Peer [" + id + "]");
                }
                catch(Exception e){
                    e.printStackTrace();
                }
        }

        //send a message to the output stream
        public void sendMessage(byte[] message){
            try{
                out.write(message);
                out.flush();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
        
    }

}