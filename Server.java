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
    Map<Integer, Peer.PeerInfo> peerInfo;

    Server(int sPort, int myPeerID, Map<Integer, Peer.PeerInfo> peerInfo){

        this.sPort = sPort;
        this.myPeerID = myPeerID;
        this.handlers = new ArrayList<>();
        logger = new Logger(myPeerID);
        this.peerInfo = peerInfo;

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
            this.buffer = new byte[100];
        }     

        public void run() {

            try{
                
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());

                doHandshaking();

                readLoop();
                
            }
            catch(IOException e){
                System.out.println("Disconnect with Client " + no);
            }
            finally{

            }

        }

        void readLoop(){

            try{

                while(true){

                    if (in.available() < 4){
                        
                        //TODO, Check for completion

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

                    //create message given type and payload

                    Message message = new Message(messageType, messagePayload);
                    message.readMessage();

                    logger.writeLog("[INFO] Peer [" + myPeerID + "] has read message of type" + message.msgType + " from Peer[" + id + "]");

                    //update Peer based on message
                    parseMessage(message);


                    //send back response
                        



                }
            }
            catch(Exception e){
                    e.printStackTrace();
                }
        }

        void parseMessage(Message message){

            switch (message.msgType){
            
                case choke:
                    break;
                case unchoke:
                    break;
                case interested:
                    break;
                case notInterested:
                    break;
                case have:
                    break;
                case bitfield:
                    
                    peerInfo.get(id).bitset.or(message.bitfieldPayload);

                    logger.writeLog("[INFO] Peer [" + myPeerID + "] recieved the bitfield message from Peer [" + id + "]");                            
                    break;
                case request:
                    break;
                case piece:
                    break;
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
                    in.readFully(buffer, 0, 32);
                    
                    System.out.println(Arrays.toString(buffer));

                    handshake = new Handshake();
                    handshake.readHandshake(buffer);
                    id = handshake.getPeerID();

                    //clear buffer
                    Arrays.fill(buffer, (byte)0);

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