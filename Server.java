import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import javax.swing.text.DefaultStyledDocument.ElementSpec;

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
        private int peerID; //ID of the client

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
                    logger.writeLog("Peer [" + myPeerID + "] read a message with message type int " + messageType);

                    //if message length > 0, read message payload

                    byte[] messagePayload = null;

                    if(messageLength > 0){

                        messagePayload = new byte[messageLength];
                        in.read(messagePayload, 0, messageLength);
                    }

                    //create message given type and payload

                    Message message;

                    if (messageLength > 0){
                        message = new Message(messageType, messagePayload);
                    }
                    else{
                        message = new Message(messageType);
                    }
                    message.readMessage();

                    //update Peer based on message and send back response if needed
                    parseMessage(message);

                }
            }
            catch(Exception e){
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    String sStackTrace = sw.toString();
                    logger.writeLog("[ERROR] Peer [" + myPeerID + "] in Server read loop with connection to peer [" + peerID + "] " + sStackTrace);
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
                    
                    processHave(message);
                    break;
                case bitfield:

                    
                    
                    if(message.bitfieldPayload != null){
                        peerInfo.get(peerID).bitset.or(message.bitfieldPayload);
                        logger.writeLog("Peer [" + myPeerID + "] recieved the bitfield message from Peer [" + peerID + "]");
                    }
                    else{
                        Message m = new Message(3);
                        byte[] outMessage = m.writeNotInterested();
                        sendMessage(outMessage);
                        logger.writeLog("Peer [" + myPeerID + "] recieved the bitfield message from Peer [" + peerID + "]");
                        break;
                    }
                    
                    //if the connected peer has more bits set than self send interested, else send not interested
                    if (peerInfo.get(myPeerID).bitset.cardinality() < peerInfo.get(peerID).bitset.cardinality()){

                        Message m = new Message(2);
                        byte[] outMessage = m.writeInterested();
                        sendMessage(outMessage);
                    }
                    else{

                        Message m = new Message(3);
                        byte[] outMessage = m.writeNotInterested();
                        sendMessage(outMessage);
                    }
                    
                    break;
                case request:
                    break;
                case piece:
                    break;
            }
            
        }

        void processHave(Message m){


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
                    peerID = handshake.getPeerID();

                    //clear buffer
                    Arrays.fill(buffer, (byte)0);

                    //show the message to the user
                    System.out.println("Receive handshake: " + handshake.getPeerID() + " from client " + no);

                    logger.writeLog("Peer [" + myPeerID + "] is connected from Peer [" + peerID + "]");
                    
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