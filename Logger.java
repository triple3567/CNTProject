import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.*;
import java.lang.Math;
import java.nio.file.*;
import java.text.SimpleDateFormat;   

public class Logger {

    int myPeerID;
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    Date date;

    Logger(int myPeerID){

        this.myPeerID = myPeerID;
    }

    public void writeLog(String s){

        String logFile = "log_peer_" + myPeerID + ".log";
        File f = new File(logFile);

        try {

            f.createNewFile();

            BufferedWriter outStream = new BufferedWriter(new FileWriter(logFile, true));
            date = new Date();

            String log = "[" + formatter.format(date) + "]: " + s;

            outStream.append(log);
            outStream.newLine();
            outStream.close();

        }
        catch(IOException e){
            e.printStackTrace();
        }

        
    }
}