import java.io.IOException;
import java.util.ArrayList;

import static java.lang.System.exit;

public class Main {

    public static void main(String[] args) throws IOException {


        //Il main qui viene utilizzato a coordinare le interazioni tra peer e tracker
        ArrayList<Integer> peers = new ArrayList<Integer>();
        ServerTracker server = new ServerTracker(49153, peers);
        server.start();
        Peers peer = new Peers(49154, 49153);
        Peers peer2 = new Peers(49155, 49153);
        Peers peer3 = new Peers(49156, 49153);


        //Attende che il primo peer completi il download del file prima di procedere.
        //Questo ciclo while impedisce la progressione del codice finché la variabile downloaded del peer non diventa true
        peer.start();
        while(!peer.getDownloaded()) { };
        server.notifyPeer(peer);

        peer2.start();
        while(!peer2.getDownloaded()) { };
        server.notifyPeer(peer2);

        peer3.start();
        while(!peer3.getDownloaded()) { };
        server.notifyPeer(peer3);
        exit(0);





    }

}
