public class PeerProcess {

    public static void main(String[] args){

        System.out.println(args[0]);

        Peer q = new Peer(Integer.parseInt(args[0]));
    }
}