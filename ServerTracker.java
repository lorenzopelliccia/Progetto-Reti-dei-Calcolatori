import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

//Classe ServerTracker che estende Thread per permettere l'esecuzione concorrente
//In questo modo il Tracker può ascoltare continuamente le connessioni in entrata dai peer
public class ServerTracker extends Thread{

    //Socket del server per accettare connessioni
    private ServerSocket socket;

    //Lista delle porte dei peer registrate dal tracker
    List<Integer> peerPort;

    public ServerTracker(int port, List<Integer> peer) throws IOException {
        //Creo un ServerSocket in ascolto sulla porta specificata
        this.socket = new ServerSocket(port);
        //Inizializzo la lista dei peer con quella passata al costruttore
        this.peerPort = peer;
    }


    //Metodo per notificare l'aggiunta di un nuovo peer alla lista
    public void notifyPeer(Peers peer) {
        //Aggiungo il nuovo peer alla lista
        this.peerPort.add(peer.getPort());
    }



    @Override
    public void run() {
        while(!Thread.interrupted()) {
            try {
                //Si accetta una nuova connessione
                Socket connection = socket.accept();
                //Preparo un ObjectOutputStream per inviare la lista dei peer attraverso la connessione
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                //Invio la lista dei peer a quello che la richiede
                out.writeObject(peerPort);
                //Chiudo la connessione dopo l'invio
                connection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }




}
