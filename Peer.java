import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.*;
import java.lang.Math;


public class Peer {
    
    public class PeerInfo{

        String hostName;
        int listeningPort;
        boolean hasCompleteFile;
    
        PeerInfo(String h, int l, boolean b){
    
            hostName = h;
            listeningPort = l;
            hasCompleteFile = b;
        }
    }

    int myPeerID;

    //common file
    int numOfPreferredNeighbors;
    int unchokingInterval;
    int optimisticUnchokingInterval;
    String fileName;
    int fileSize;
    int pieceSize;
    int numPieces;

    //peer info file
    Map<Integer, PeerInfo> peerInfo = new HashMap<>(); 

    Peer(int peerID){
        myPeerID = peerID;

        startup();

        writeLog("test1");
        writeLog("test2");
    }

    void startup(){
        readCommonFile();
        readPeerInfoFile();

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
    void writeLog(String s){

        String logFile = "log_peer_" + myPeerID + ".log";
        File f = new File(logFile);

        try {

            f.createNewFile();

            BufferedWriter outStream = new BufferedWriter(new FileWriter(logFile, true));

            outStream.append(s);
            outStream.newLine();
            outStream.close();

        }
        catch(IOException e){
            e.printStackTrace();
        }

        
    }
}
