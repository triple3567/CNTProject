/*
 *                     CEN5501C Project2
 * This is the program starting remote processes.
 * This program was only tested on CISE SunOS environment.
 * If you use another environment, for example, linux environment in CISE 
 * or other environments not in CISE, it is not guaranteed to work properly.
 * It is your responsibility to adapt this program to your running environment.
 */

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/*
 * The StartRemotePeers class begins remote peer processes. 
 * It reads configuration file PeerInfo.cfg and starts remote peer processes.
 * You must modify this program a little bit if your peer processes are written in C or C++.
 * Please look at the lines below the comment saying IMPORTANT.
 */
public class KillRemotePeers {

	public Vector<RemotePeerInfo> peerInfoVector;
	
	public void getConfiguration()
	{
		String st;
		int i1;
		peerInfoVector = new Vector<RemotePeerInfo>();
		try {
			BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while((st = in.readLine()) != null) {
				
				 String[] tokens = st.split("\\s+");
		    	 //System.out.println("tokens begin ----");
			     //for (int x=0; x<tokens.length; x++) {
			     //    System.out.println(tokens[x]);
			     //}
		         //System.out.println("tokens end ----");
			    
			     peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
			
			}
			
			in.close();
		}
		catch (Exception ex) {
			System.out.println(ex.toString());
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			KillRemotePeers myStart = new KillRemotePeers();
			myStart.getConfiguration();
					
			// get current path
			String path = System.getProperty("user.dir");
			
			// start clients at remote hosts
			for (int i = 0; i < myStart.peerInfoVector.size(); i++) {
				RemotePeerInfo pInfo = (RemotePeerInfo) myStart.peerInfoVector.elementAt(i);
				
				System.out.println("Kill remote peer " + pInfo.peerId +  " at " + pInfo.peerAddress );
				
				// *********************** IMPORTANT *************************** //
				// If your program is JAVA, use this line.
				Runtime.getRuntime().exec("ssh " + pInfo.username + "@" + pInfo.peerAddress + " cd " + path + "; pkill -f \"java peerProcess\"");
				
				// If your program is C/C++, use this line instead of the above line. 
				//Runtime.getRuntime().exec("ssh " + pInfo.peerAddress + " cd " + path + "; ./peerProcess " + pInfo.peerId);

				TimeUnit.SECONDS.sleep(2);
			}		
			System.out.println("killing all remote peers has done." );

		}
		catch (Exception ex) {
			System.out.println(ex);
		}
	}

}
