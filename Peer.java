import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.*;
import java.lang.Math;
import java.nio.file.*;


public class Peer {
    
    public class PeerInfo{

        String hostName;
        int listeningPort;
        boolean hasCompleteFile;
        boolean[] bitArray;
    
        PeerInfo(String h, int l, boolean b){
    
            hostName = h;
            listeningPort = l;
            hasCompleteFile = b;
            bitArray = new boolean[numPieces];
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

    //peer info file
    Map<Integer, PeerInfo> peerInfo = new HashMap<>(); 

    //Clients and Server
    List<Client> clients = new ArrayList<>();
    Server server;

    Peer(int peerID){

        myPeerID = peerID;
        startup();
    }

    void startup(){

        readCommonFile();
        readPeerInfoFile();
        readFileIfComplete();
        startClients();
        startServer();
    }

    void readFileIfComplete(){
        
        //if has file, load into bytearray, else initialize byte array as size
        try{
            if(peerInfo.get(myPeerID).hasCompleteFile){
                
                fileBytes = Files.readAllBytes(Paths.get("./peer_" + myPeerID + "/" + fileName));
                peerInfo.get(myPeerID).hasCompleteFile = true;
                Arrays.fill(peerInfo.get(myPeerID).bitArray, true);
            }
            else{
                fileBytes = new byte[fileSize];
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
                
                clients.add(new Client(value.listeningPort, value.hostName, myPeerID));
                clients.get(clients.size() - 1).start();
            }
        }
    }

    void startServer(){

        //Start listening on the listening port
        server = new Server(peerInfo.get(myPeerID).listeningPort, myPeerID);
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

                PeerInfo p = new PeerInfo(hostname, listeningPort, hasCompleteFile);

                peerInfo.put(peerId, p);
            }

            reader.close();
            
        }
        catch(FileNotFoundException e){
            e.printStackTrace();
        }
    }

}
