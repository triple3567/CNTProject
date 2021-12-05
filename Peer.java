import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.*;
import java.lang.Math;
import java.lang.reflect.Array;
import java.nio.file.*;
import java.time.Instant;


public class Peer {
    
    public class PeerInfo{

        volatile String hostName;
        volatile int listeningPort;
        volatile boolean hasCompleteFile;
        volatile boolean[] bitArray;
        volatile boolean interested;
        volatile BitSet bitset;
        volatile boolean choked;
        volatile Stack<Integer> freshPieces;
        volatile int piecesReceived;
        volatile byte[] fileBytes;
        volatile boolean chokedby;
        volatile int pieceSize;
        volatile double downloadRate;
        volatile int numPieces;
    
        PeerInfo(String h, int l, boolean b, int numP, int pSize){
    
            numPieces = numP;
            pieceSize = pSize;
            hostName = h;
            listeningPort = l;
            hasCompleteFile = b;
            bitArray = new boolean[numP];
            bitset = new BitSet(numP);
            interested = false;
            choked = true;
            chokedby = true;
            freshPieces = new Stack<Integer>();
            piecesReceived = 0;
            fileBytes = new byte[pSize * numPieces];
            downloadRate = 0.0;
            
        }
    }

    //common file
    int myPeerID;
    int numOfPreferredNeighbors;
    int unchokingInterval;
    int optimisticUnchokingInterval;
    String fileName;
    int fileSize;
    int pieceSize;
    int numPieces;
    byte[] fileBytes;
    Logger logger;
    int startTime;
    boolean writeFileSwitch;

    //peer info file
    Map<Integer, PeerInfo> peerInfo = new HashMap<>(); 

    //Clients and Server
    List<Client> clients = new ArrayList<>();
    Server server;

    Peer(int peerID){

        myPeerID = peerID;
        startTime = (int)Instant.now().getEpochSecond();
        logger = new Logger(myPeerID);
        startup();
    }

    void startup(){

        readCommonFile();
        readPeerInfoFile();
        readFileIfComplete();
        startClients();
        startServer();


        neighborLoop();

        return;
    }

    void readFileIfComplete(){
        
        //if has file, load into bytearray, else initialize byte array as size
        try{
            if(peerInfo.get(myPeerID).hasCompleteFile){               

                peerInfo.get(myPeerID).fileBytes = new byte[numPieces * pieceSize];

                byte[] b = Files.readAllBytes(Paths.get("./peer_" + myPeerID + "/" + fileName));

                for(int count = 0; count < b.length; count++){

                    peerInfo.get(myPeerID).fileBytes[count] = b[count];
                }

                peerInfo.get(myPeerID).bitset = new BitSet(numPieces);
                peerInfo.get(myPeerID).hasCompleteFile = true;
                peerInfo.get(myPeerID).bitset.set(0, numPieces, true);
                writeFileSwitch = true;

            }
            else{
                
                peerInfo.get(myPeerID).fileBytes = new byte[numPieces * pieceSize];
                peerInfo.get(myPeerID).bitset = new BitSet(numPieces);
                peerInfo.get(myPeerID).hasCompleteFile = false;
                peerInfo.get(myPeerID).bitset.set(0, numPieces, false);
                writeFileSwitch = false;

            }
        }
        catch(IOException e){
            e.printStackTrace();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            logger.writeLog("[ERROR] Peer [" + myPeerID + "] in readFleIfComplete " + sStackTrace);
        }
    }

    void startClients(){

        //Create a client thread for each neighbor read from PeerInfo.cfg that is not itself.
        for(Map.Entry<Integer, PeerInfo> entry : peerInfo.entrySet()){

            int key = entry.getKey();
            PeerInfo value = entry.getValue();

            //iterate over all peers except self
            if(key != myPeerID) {

                System.out.println("Key: " + key + " myPeerID: " + myPeerID);
                
                clients.add(new Client(value.listeningPort, value.hostName, myPeerID, peerInfo, key));
                clients.get(clients.size() - 1).start();
            }
        }

        return;
    }

    void startServer(){

        //Start listening on the listening port
        server = new Server(peerInfo.get(myPeerID).listeningPort, myPeerID, peerInfo, peerInfo.size() - 1);
        server.start();

        return;
    }
    
    void readCommonFile(){

        File commonFile = new File("Common.cfg");

        try{
            Scanner reader = new Scanner(commonFile);

                
            reader.next();
            numOfPreferredNeighbors = reader.nextInt();

            reader.next();
            unchokingInterval = reader.nextInt();

            reader.next();
            optimisticUnchokingInterval = reader.nextInt();

            reader.next();
            fileName = reader.next();

            reader.next();
            fileSize = reader.nextInt();

            reader.next();
            pieceSize = reader.nextInt();
    
            reader.close();


            numPieces = (int)Math.ceil((double)fileSize/(double)pieceSize);


        }
        catch (FileNotFoundException e){
            e.printStackTrace();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            logger.writeLog("[ERROR] Peer [" + myPeerID + "] in readCommonFile " + sStackTrace);
        }

    }

    void readPeerInfoFile(){

        File peerInfoFile = new File("PeerInfo.cfg");

        try{

            Scanner reader = new Scanner(peerInfoFile);

            while(reader.hasNextLine()){

                int peerId = reader.nextInt();
                String hostname = reader.next();
                int listeningPort = reader.nextInt();
                boolean hasCompleteFile = reader.nextInt() == 1;
                reader.nextLine();

                PeerInfo p = new PeerInfo(hostname, listeningPort, hasCompleteFile, numPieces, pieceSize);

                peerInfo.put(peerId, p);
            }

            reader.close();
            
        }
        catch(FileNotFoundException e){
            e.printStackTrace();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            logger.writeLog("[ERROR] Peer [" + myPeerID + "] in readPeerInfoFile " + sStackTrace);
        }
    }

    void neighborLoop(){

        int preferredNeighborTimer = (int)Instant.now().getEpochSecond();
        int optimisticNeighborTimer = (int)Instant.now().getEpochSecond();

        loop: while(true){
            
            if(peerInfo.get(myPeerID).hasCompleteFile == false){

                if((int)Instant.now().getEpochSecond() - preferredNeighborTimer > unchokingInterval){

                    newPreferredNeighbors();
                    preferredNeighborTimer = (int)Instant.now().getEpochSecond();
                }
                
                if((int)Instant.now().getEpochSecond() - optimisticNeighborTimer > optimisticUnchokingInterval){

                    newOptimisticNeighbor();
                    optimisticNeighborTimer = (int)Instant.now().getEpochSecond();
                }
            }
            else{

                if(writeFileSwitch == false){

                    writeFile();
                }

                if((int)Instant.now().getEpochSecond() - preferredNeighborTimer > unchokingInterval){

                    newPreferredNeighborsRandom();
                    preferredNeighborTimer = (int)Instant.now().getEpochSecond();
                }
                
                if((int)Instant.now().getEpochSecond() - optimisticNeighborTimer > optimisticUnchokingInterval){

                    newOptimisticNeighbor();
                    optimisticNeighborTimer = (int)Instant.now().getEpochSecond();
                }

                for(Map.Entry<Integer, PeerInfo> entry : peerInfo.entrySet()){

                    int key = entry.getKey();
                    PeerInfo value = entry.getValue();
        
                    //iterate over all peers except self
                    if(key != myPeerID) {
        
                        if(value.hasCompleteFile == false){

                            continue loop;
                        }
                    }
                }

                //exit condition
                killThreads();
                logger.writeLog("ALL PEERS HAVE DOWNLOADED COMPlETE FILE");

                break loop;
                
            }

        }

        return;
    }

    void writeFile(){

        try{

            OutputStream out = new FileOutputStream("./peer_" + myPeerID + "/" + fileName);

            byte[] b = Arrays.copyOfRange(peerInfo.get(myPeerID).fileBytes, 0, fileSize);

            out.write(b);
            out.close();
        }
        catch(Exception e){

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString(); // stack trace as a string
            logger.writeLog("[ERROR] Peer [" + myPeerID + "]" + sStackTrace);
        }

        logger.writeLog("Peer [" + myPeerID + "] has downloaded the complete file");
        writeFileSwitch = true;

        return;
    }

    void killThreads(){

        for(Client c : clients){

            c.interrupt();
        }

        server.interrupt();

        return;
        
    }

    void newPreferredNeighbors(){

        int timeElapsed = startTime / (int)Instant.now().getEpochSecond();
        Map<Integer, Double> downloadRates = new HashMap<>();
        ArrayList<Integer> unchokeNeighbors = new ArrayList<Integer>();
        int numInterested = 0;

        for(Map.Entry<Integer, PeerInfo> entry : peerInfo.entrySet()){

            int key = entry.getKey();
            PeerInfo value = entry.getValue();

            //iterate over all peers except self
            if(key != myPeerID) {

                value.downloadRate = (double)value.piecesReceived / (double)timeElapsed;
                
                if(value.interested){

                    numInterested++;
                    downloadRates.put(key, value.downloadRate);
                }

            }
        }

        int neighborsToSet = Math.min(numInterested, numOfPreferredNeighbors);

        for(int i = 0; i < neighborsToSet; i++){

            int peerIdMax = -1;
            double max = -99999.99999;

            for(Map.Entry<Integer, Double> entry : downloadRates.entrySet()){

                int key = entry.getKey();
                double value = entry.getValue();

                if (value > max){

                    max = value;
                    peerIdMax = key;
                }
                
            }

            if(peerIdMax != -1){
                
                unchokeNeighbors.add(peerIdMax);
                downloadRates.remove(peerIdMax);
            }
                
        }

        String s = "";

        for(int i = 0; i < unchokeNeighbors.size(); i++){

            s += unchokeNeighbors.get(i);

            if(i < unchokeNeighbors.size() - 1){

                s += " ";
            }
        }

        logger.writeLog("Peer [" + myPeerID + "] has the preferred neighbors [" + s + "]");

        for(Map.Entry<Integer, PeerInfo> entry : peerInfo.entrySet()){

            int key = entry.getKey();
            PeerInfo value = entry.getValue();

            //iterate over all peers except self
            if(key != myPeerID) {

                if(unchokeNeighbors.contains(key)){

                    value.choked = false;
                }
                else{
                    
                    value.choked = true;
                }

            }
        }



        return;

    }

    void newPreferredNeighborsRandom(){

        ArrayList<Integer> neighborsInterested = new ArrayList<Integer>();
        ArrayList<Integer> unchokeNeighbors = new ArrayList<Integer>();
        int numInterested = 0;

        for(Map.Entry<Integer, PeerInfo> entry : peerInfo.entrySet()){

            int key = entry.getKey();
            PeerInfo value = entry.getValue();

            //iterate over all peers except self
            if(key != myPeerID) {

                if(value.interested == true){
                    
                    numInterested++;
                    neighborsInterested.add(key);
                }


            }
        }

        int neighborsToSet = Math.min(numInterested, numOfPreferredNeighbors);

        for(int i = 0; i < neighborsToSet; i++){
            int index = (int)(Math.random() * neighborsInterested.size());
            int unchokePeer = neighborsInterested.get(index);
            unchokeNeighbors.add(unchokePeer);
            neighborsInterested.remove(index);
        }

        String s = "";

        for(int i = 0; i < unchokeNeighbors.size(); i++){

            s += unchokeNeighbors.get(i);

            if(i < unchokeNeighbors.size() - 1){

                s += " ";
            }
        }

        logger.writeLog("Peer [" + myPeerID + "] has the preferred neighbors [" + s + "]");        

        for(Map.Entry<Integer, PeerInfo> entry : peerInfo.entrySet()){

            int key = entry.getKey();
            PeerInfo value = entry.getValue();

            //iterate over all peers except self
            if(key != myPeerID) {

                if(unchokeNeighbors.contains(key)){

                    value.choked = false;
                }
                else{
                    
                    value.choked = true;
                }

            }
        }




        return;
    }
    
    void newOptimisticNeighbor(){


        ArrayList<Integer> neighborsInterested = new ArrayList<Integer>();
        int unchokeNeighbor;

        for(Map.Entry<Integer, PeerInfo> entry : peerInfo.entrySet()){

            int key = entry.getKey();
            PeerInfo value = entry.getValue();

            //iterate over all peers except self
            if(key != myPeerID) {

                if(value.interested == true && value.choked == true){

                    neighborsInterested.add(key);
                }

            }
        }

        if(neighborsInterested.size() == 0){
            return;
        }

        int index = (int)(Math.random() * neighborsInterested.size());
        int unchokePeer = neighborsInterested.get(index);
        unchokeNeighbor = unchokePeer;

        logger.writeLog("Peer [" + myPeerID + "] has the optimistically unchoked neighbor [" + unchokeNeighbor + "]");

        peerInfo.get(unchokeNeighbor).choked = false;

        

        return;
    }
    
}
