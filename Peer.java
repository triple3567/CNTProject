import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Peer {
    
    int myPeerID;

    //common file
    int numOfPreferredNeighbors;
    int unchokingInterval;
    int optimisticUnchokingInterval;
    String fileName;
    int fileSize;
    int pieceSize;

    Peer(int peerID){
        myPeerID = peerID;

        startup();
    }

    void startup(){
        readCommonFile();
        readPeerInfoFile();
    }

    void readCommonFile(){

        File commonFile = new File("Common.cfg");

        try{
            Scanner reader = new Scanner(commonFile);

            while (reader.hasNext()) {
                
                reader.useDelimiter(" ");
            }
    
            reader.close();
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
        }

        

    }
    
    void readPeerInfoFile(){

    }

}
