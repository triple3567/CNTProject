import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ValueExp;
import javax.swing.text.DefaultStyledDocument.ElementSpec;

public class Server extends Thread{

    private int sPort;
    public int myPeerID;
    public int numConnections = 0;
    public List<Handler> handlers;
    public String host;
    public Logger logger;
    Map<Integer, Peer.PeerInfo> peerInfo;
    private final AtomicBoolean running = new AtomicBoolean(false);
    public int maxConnections;

    Server(int sPort, int myPeerID, Map<Integer, Peer.PeerInfo> peerInfo, int totalConnections){
        
        maxConnections = totalConnections;
        this.sPort = sPort;
        this.myPeerID = myPeerID;
        this.handlers = new ArrayList<>();
        logger = new Logger(myPeerID);
        this.peerInfo = peerInfo;
        running.set(true);

    }

    public void interrupt(){


        for(Handler h : handlers){

            h.interrupt();
        }
        running.set(false);
    }


    //Creates a new thread for Server. Will loop on listening for more connections. 
    //TODO create exit condition so server will close when all connections end
    public void run() {

        try{
        
            ServerSocket listener = new ServerSocket(sPort);
            System.out.println("Server started. Listening on port: " + sPort + " at host: " + java.net.InetAddress.getLocalHost().getHostName());

            try {

                //Add a new listener thread to handlers array when a connection is established. 
                while(numConnections < maxConnections) {

                    handlers.add(new Handler(listener.accept(), numConnections));
                    handlers.get(numConnections).start();
                    System.out.println("[SERVER] Connection " + numConnections + " established");
                    numConnections++;
                }

                while(running.get()){
                    
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
        private final AtomicBoolean running = new AtomicBoolean(false);

        public Handler(Socket connection, int no) {
            this.connection = connection;
            this.no = no;
            this.buffer = new byte[100];
            running.set(true);
        }     

        public void interrupt(){

            running.set(false);
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

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String sStackTrace = sw.toString();
                logger.writeLog("[ERROR] Peer [" + myPeerID + "] in Server read loop with connection to peer [" + peerID + "] " + sStackTrace);
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

            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
            catch(Exception e){
                e.printStackTrace();
            }
            }

        }

        void readLoop(){

            try{

                while(running.get()){

                    checkFileCompletion();

                    // if unchoked and buffer has no data
                    if (in.available() < 4 && !peerInfo.get(peerID).chokedby && peerInfo.get(myPeerID).hasCompleteFile == false){

                        sendRequest();
                        continue;
                    }

                    if (in.available() < 4 ){
                        
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

                    //update Peer based on message and send back response if needed
                    parseMessage(messageType, messagePayload);

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

        void parseMessage(int msgType, byte[] msgPayload){

            switch (msgType){
            
                case 0:
                    peerInfo.get(peerID).chokedby = true;
                    logger.writeLog("Peer [" + myPeerID + "] is choked by Peer [" + peerID + "]");
                    break;

                case 1:
                    
                    peerInfo.get(peerID).chokedby = true;
                    logger.writeLog("Peer [" + myPeerID + "] is unchoked by Peer [" + peerID + "]");
                    break;

                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    
                    int pieceNum = Message.readHavePayload(msgPayload);
                    logger.writeLog("Peer [" + myPeerID + "] recieved the 'have' message from Peer [" + peerID + "] for the piece [" + pieceNum + "]");
                    processHave(pieceNum);
                    break;
                case 5:
                    
                    BitSet b = Message.readBitfieldPayload(msgPayload);

                    if(b != null){
                        peerInfo.get(peerID).bitset.or(b);
                        logger.writeLog("Peer [" + myPeerID + "] recieved the 'bitfield' message from Peer [" + peerID + "]");
                    }
                    else{
                        
                        sendUninterested();
                        logger.writeLog("Peer [" + myPeerID + "] recieved the 'bitfield' message from Peer [" + peerID + "]");
                        break;
                    }
                    
                    //if the connected peer has more bits set than self send interested, else send not interested
                    if (peerInfo.get(myPeerID).bitset.cardinality() < peerInfo.get(peerID).bitset.cardinality()){

                        sendInterested();
                    }
                    else{

                        sendUninterested();
                    }
                    
                    break;
                case 6:
                    break;
                case 7:
                    break;
            }
            
        }

        void processHave(int piece){

            peerInfo.get(peerID).bitset.set(piece);

            if(peerInfo.get(peerID).bitset.cardinality() == peerInfo.get(peerID).bitset.size()){

                peerInfo.get(peerID).hasCompleteFile = true;
            }

            if(peerInfo.get(myPeerID).bitset.get(piece) == false && peerInfo.get(peerID).interested == false){

                sendInterested();
            }
            else if(peerInfo.get(myPeerID).bitset.get(piece) == false && peerInfo.get(peerID).interested == true){

                return;
            }
            else if(peerInfo.get(myPeerID).bitset.get(piece) == true){

                BitSet peerBitset = (BitSet)peerInfo.get(peerID).bitset.clone();
                BitSet myBitset = (BitSet)peerInfo.get(myPeerID).bitset.clone();

                ArrayList<Integer> bitIndex = new ArrayList<Integer>();

                for(int i = 0; i < myBitset.size(); i++){

                    if(myBitset.get(i) == false && peerBitset.get(i) == true){
    
                        bitIndex.add(i);
                    }
                }

                if(bitIndex.size() > 0 && peerInfo.get(peerID).interested == true){

                    return;
                }
                else if (bitIndex.size() > 0 && peerInfo.get(peerID).interested == false){
                    
                    sendInterested();
                }
                else if(bitIndex.size() == 0 && peerInfo.get(peerID).interested == true){

                    sendUninterested();
                }
                else if(bitIndex.size() == 0 && peerInfo.get(peerID).interested == false){
                    return;
                }

                return;
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
                    peerID = handshake.getPeerID();

                    //clear buffer
                    Arrays.fill(buffer, (byte)0);


                    logger.writeLog("Peer [" + myPeerID + "] is connected from Peer [" + peerID + "]");
                    
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
        public void sendMessage(byte[] message){
            try{
                out.write(message);
                out.flush();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

        void sendRequest(){

            BitSet peerBitset = (BitSet)peerInfo.get(peerID).bitset.clone();
            BitSet myBitset = (BitSet)peerInfo.get(myPeerID).bitset.clone();

            ArrayList<Integer> bitIndex = new ArrayList<Integer>();

            for(int i = 0; i < myBitset.size(); i++){

                if(myBitset.get(i) == false && peerBitset.get(i) == true){

                    bitIndex.add(i);
                }
            }

            if(bitIndex.size() == 0){
                return;
            }



            int requestIndex = (int)(Math.random() * bitIndex.size());

            Message m = new Message(6);
            m.setRequestPayload(bitIndex.get(requestIndex));
            byte[] mOut = m.writeMessage();

            sendMessage(mOut);

            logger.writeLog("[INFO] Peer [" + myPeerID + "] sent the request message to Peer [" + peerID + "] for peice " + bitIndex.get(requestIndex));

            processPiece(bitIndex.get(requestIndex));

            return;
            
        }

        void processPiece(int pieceIndex){

            try{
                while (in.available() < 4 ){
                            
                    //wait for piece

                }

                loop: while (true){

                    //read 4 bytes for message length
                    int messageLength = in.readInt();

                    //read 1 byte for type
                    int messageType = in.readUnsignedByte();

                    //process message payload
                    byte[] messagePayload = null;

                    if(messageLength > 0){

                        messagePayload = new byte[messageLength];
                        in.read(messagePayload, 0, messageLength);
                    }

                    switch (messageType){

                        case 0:

                            peerInfo.get(peerID).chokedby = true;
                            logger.writeLog("Peer [" + myPeerID + "] is choked by Peer [" + peerID + "]");
                            break;

                        case 1:

                            peerInfo.get(peerID).chokedby = false;
                            logger.writeLog("Peer [" + myPeerID + "] is unchoked by Peer [" + peerID + "]");
                            break;

                        case 4:

                            int pieceNum = Message.readHavePayload(messagePayload);
                            logger.writeLog("Peer [" + myPeerID + "] recieved the 'have' message from Peer [" + peerID + "] for the piece [" + pieceNum + "]");
                            processHave(pieceNum);
                            break;

                        case 7:

                            byte[] fullPiece = messagePayload;

                            int index = pieceIndex * peerInfo.get(myPeerID).pieceSize;

                            for(int i = 0; i < peerInfo.get(myPeerID).pieceSize; i++){

                                peerInfo.get(myPeerID).fileBytes[index] = fullPiece[i];
                                index++;
                            }

                            peerInfo.get(peerID).piecesReceived += 1;

                            for(Map.Entry<Integer, Peer.PeerInfo> entry : peerInfo.entrySet()){

                                int key = entry.getKey();
                                Peer.PeerInfo value = entry.getValue();
                    
                                //iterate over all peers except self
                                if(key != myPeerID) {
                    
                                    value.freshPieces.push(pieceIndex);
                                }
                            }

                            logger.writeLog("Peer [" + myPeerID + "] has download the piece [" + pieceIndex + "] from [" + peerID + "]. Now the number of pieces it has is " + peerInfo.get(myPeerID).bitset.cardinality());

                            if(peerInfo.get(myPeerID).bitset.cardinality() == peerInfo.get(myPeerID).bitset.size()){

                                peerInfo.get(myPeerID).hasCompleteFile = true;
                            }

                            break loop;
                    }
                }

            }
            catch(Exception e){

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String sStackTrace = sw.toString(); // stack trace as a string
                logger.writeLog("[ERROR] Peer [" + myPeerID + "]" + sStackTrace);
            }
        }
        
        void sendInterested(){

            Message m = new Message(2);
            byte[] outMessage = m.writeMessage();

            peerInfo.get(myPeerID).interested = true;

            sendMessage(outMessage);
        }

        void sendUninterested(){

            Message m = new Message(3);
            byte[] outMessage = m.writeMessage();

            peerInfo.get(myPeerID).interested = false;

            sendMessage(outMessage);
        }

        void checkFileCompletion(){

            if(peerInfo.get(myPeerID).bitset.cardinality() == peerInfo.get(myPeerID).bitset.size()){

                peerInfo.get(myPeerID).hasCompleteFile = true;

                for(Map.Entry<Integer, Peer.PeerInfo> entry : peerInfo.entrySet()){

                    int key = entry.getKey();
                    Peer.PeerInfo value = entry.getValue();
        
                    //iterate over all peers except self
                    if(key != myPeerID) {
        
                        if(value.interested == true){
                            value.interested = false;

                            sendUninterested();
                        }
                    }
                }
            }
        }

    }

    




}