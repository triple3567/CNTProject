import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.*;
import java.lang.Math;
import java.nio.file.*;
import java.time.Instant;


public class Peer {
    
    public class PeerInfo{

        String hostName;
        int listeningPort;
        boolean hasCompleteFile;
        boolean[] bitArray;
        boolean interested;
        BitSet bitset;
        boolean choked;
    
        PeerInfo(String h, int l, boolean b, int numP){
    
            hostName = h;
            listeningPort = l;
            hasCompleteFile = b;
            bitArray = new boolean[numP];
            bitset = new BitSet(numP);
            interested = false;
            choked = true;
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

    //peer info file
    Map<Integer, PeerInfo> peerInfo = new HashMap<>(); 

    //Clients and Server
    List<Client> clients = new ArrayList<>();
    Server server;

    Peer(int peerID){

        myPeerID = peerID;
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
    }

    void readFileIfComplete(){
        
        //if has file, load into bytearray, else initialize byte array as size
        try{
            if(peerInfo.get(myPeerID).hasCompleteFile){
                
                fileBytes = Files.readAllBytes(Paths.get("./peer_" + myPeerID + "/" + fileName));
                peerInfo.get(myPeerID).bitset = new BitSet(numPieces);
                peerInfo.get(myPeerID).hasCompleteFile = true;
                peerInfo.get(myPeerID).bitset.set(0, numPieces, true);

            }
            else{
                fileBytes = new byte[fileSize];
                peerInfo.get(myPeerID).bitset = new BitSet(numPieces);
                peerInfo.get(myPeerID).hasCompleteFile = false;
                peerInfo.get(myPeerID).bitset.set(0, numPieces, false);

            }
        }
        catch(IOException e){
            e.printStackTrace();
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
    }

    void startServer(){

        //Start listening on the listening port
        server = new Server(peerInfo.get(myPeerID).listeningPort, myPeerID, peerInfo);
        server.start();
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

                PeerInfo p = new PeerInfo(hostname, listeningPort, hasCompleteFile, numPieces);

                peerInfo.put(peerId, p);
            }

            reader.close();
            
        }
        catch(FileNotFoundException e){
            e.printStackTrace();
        }
    }

    void neighborLoop(){

        int preferredNeighborTimer = (int)Instant.now().getEpochSecond();
        int optimisticNeighborTimer = (int)Instant.now().getEpochSecond();

        while(true){
            
            if(!peerInfo.get(myPeerID).hasCompleteFile){

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

                if((int)Instant.now().getEpochSecond() - preferredNeighborTimer > unchokingInterval){

                    newPreferredNeighborsRandom();
                    preferredNeighborTimer = (int)Instant.now().getEpochSecond();
                }
                
                if((int)Instant.now().getEpochSecond() - optimisticNeighborTimer > optimisticUnchokingInterval){

                    newOptimisticNeighbor();
                    optimisticNeighborTimer = (int)Instant.now().getEpochSecond();
                }

                //TODO: if all peers have complete file, kill all threads
            }

        }
    }

    void newPreferredNeighbors(){

    }
    void newPreferredNeighborsRandom(){

    }
    
    void newOptimisticNeighbor(){

    }
}
