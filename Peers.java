import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

//Classe Peers che estende Thread per permettere l'esecuzione concorrente
public class Peers extends Thread {

    //Dimensione del file da condivedere tra i peers
    private static int fileSize;

    //Porta del tracker
    private int trackerPort;

    //Percorso del file da condividere
    private byte[] fileToShare;

    //Socket del server per accettare le connessioni
    private ServerSocket socket;

    //Contatore statico per assegnare un ID unico ad ogni peer
    private static int cnt = 1;

    //ID del peer
    private int id;

    //Porta su cui il peer è in ascolto
    private int port;

    //Flag per indicare se il file è stato scaricato
    //La parola chiave volatile assicura che ogni lettura della variabile downloaded da parte di un thread ottenga l'ultimo valore scritto da qualsiasi thread
    //Senza volatile, i thread potrebbero leggere valori vecchi della variabile downloaded, portando a una visione inconsistente dello stato del download tra diversi thread
    private volatile boolean downloaded;



    public Peers(int port, int trackerPort) throws IOException {
        //Creo un ServerSocket in ascolto sulla porta specificata
        this.socket = new ServerSocket(port);
        //Assegno l'ID al peer e incremento il contatore
        this.id = cnt++;
        //Imposto la porta del tracker
        this.trackerPort = trackerPort;
        this.port = port;
        this.downloaded = false;



    }

    public void run() {
        int sliceSize;
        //Loop che continua fino a quando il thread non viene interrotto
        while(!Thread.interrupted()) {
            try {
                if (fileToShare == null) {
                    //Se è il primo peer ad entrare legge il file da disco
                    if (this.id == 1) {
                        fileToShare = Files.readAllBytes(Path.of("C:\\Users\\pierl\\OneDrive\\Desktop\\Testo esercizio.txt"));
                        //Imposto la dimensione del file
                        fileSize = fileToShare.length;
                        System.out.println("Il primo peer legge " + fileToShare.length + " Byte presenti nel file");
                    } else {

                        //Altrimenti recupera la lista dei peers dal tracker e divide il file in parti.
                        List<Integer> peers = getListOfPeers(trackerPort);
                        System.out.println("Il peer con ID " + this.id + " si connette al tracker sulla porta " + trackerPort + " per ottenere la lista dei peer attivi da cui scaricare parti del file");

                        //Calcolo la dimensione di ogni porzione di file basandosi sulla dimensione totale del file e sul numero di peer da cui scaricare
                        sliceSize = fileSize / peers.size();
                        fileToShare = new byte[fileSize];
                        int j = 0;
                        for (Integer peer : peers) {
                            //Inizializzo un array temporaneo per ospitare la porzione di file ricevuta dal peer corrente
                            byte[] temp;
                            //Faccio in modo che temp richieda e riceva la porzione di file dal peer corrente
                            temp = returnFile(peer, sliceSize);
                            //Ciclo su ogni byte della porzione ricevuta
                            for(int i = 0; i < sliceSize; i++) {
                                //Controllo se l'indice corrente è ancora entro i limiti della dimensione totale del file
                                if(j < fileSize) {
                                    //In caso affermativo assegno il byte corrente dalla porzione ricevuta alla posizione corrente nell'array del file completo
                                    fileToShare[j] = temp[i];
                                    j++;
                                }
                            }
                            System.out.println("Il peer con ID " + this.id + " legge " + sliceSize + " byte dal peer sulla porta: " + peer);
                        }
                        System.out.println("Il peer con ID " + this.id + " scarica " + fileToShare.length + " byte");



                    }

                    downloaded = true;


                }



                else {

                    //Il server accetta una connessione in arrivo da un altro peer. Questo blocco si blocca e attende fino a quando un peer non si connette
                    Socket connection = socket.accept();


                    //Recupera la lista dei peers attualmente connessi al tracker. Questo è necessario per determinare come dividere il file in parti uguali tra i peers
                    List<Integer> peers = getListOfPeers(trackerPort);
                    System.out.println("Il peer con ID " + this.id + " si connette al tracker sulla porta " + trackerPort + " per aggiornare la lista dei peer attivi e contribuire condividendo parti del file\n");


                    //Calcola la dimensione di ciascuna porzione di file in base al numero totale di peers. Questo assicura che ogni peer riceva una parte equa del file
                    sliceSize = fileSize / peers.size();

                    //Se l'ID del peer corrente moltiplicato per la dimensione della porzione supera la dimensione totale del file,
                    //significa che questo peer è l'ultimo della lista e potrebbe non dover inviare una porzione completa
                    //Pertanto, la dimensione della sua porzione viene regolata di conseguenza
                    if ((this.id - 1) * sliceSize + sliceSize > fileSize) {
                        //Calcolo la nuova dimensione della fetta sottraendo la quantità di byte già assegnata ai peer precedenti
                        //(identificati da (this.id - 1) * sliceSize) dalla dimensione totale del file
                        sliceSize = fileSize - (this.id - 1) * sliceSize;
                    }

                    //Crea un array di byte temporaneo per memorizzare la porzione di file da inviare.
                    byte[] temp = new byte[sliceSize];

                    //Prepara un flusso di output per inviare dati al peer connesso. Questo flusso sarà usato per inviare la porzione di file
                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                    
                    //Ciclo attraverso ogni byte della porzione di file destinata a essere inviata.
                    for(int i = 0; i < sliceSize; i++) {
                        //Copio il byte corrispondente dal file condiviso all'array di byte temporaneo
                        //L'indice del byte da copiare è calcolato in base all'ID del peer e alla dimensione della porzione,
                        //garantendo che ogni peer riceva la parte corretta del file.
                        temp[i] = fileToShare[(this.id - 1) * sliceSize + i];
                    }
                    //Invio l'array di byte, che ora contiene la porzione di file, al peer connesso
                    outputStream.write(temp);


                }

            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);

            }
        }
    }



    public byte[] returnFile(int port, int size) throws IOException {
        //Creo una nuova socket
        Socket socketStream = new Socket();

        //Creo un endpoint di rete utilizzando la porta specificata. Questo identifica il peer da cui scaricare la porzione di file
        InetSocketAddress peersAddress = new InetSocketAddress(port);

        //Connette il socket all'indirizzo del peer. Questo stabilisce una connessione TCP al peer specificato
        socketStream.connect(peersAddress);

        //Ottengo lo stream di input dal socket connesso. Questo stream sarà utilizzato per ricevere dati dal peer
        InputStream stream = socketStream.getInputStream();

        //Creo un array di byte della dimensione specificata. Questo buffer sarà utilizzato per memorizzare i dati ricevuti dal peer
        byte[] buffer = new byte[size];

        //Leggo i dati dallo stream di input e li memorizza nel buffer. Questo passo effettivamente scarica la porzione di file
        //La funzione read() blocca l'esecuzione fino a quando i dati non sono disponibili per essere letti
        stream.read(buffer);

        //Chiudo lo stream di input e il socket. Questo rilascia le risorse di rete associate a questa connessione
        stream.close();
        socketStream.close();

        //Restituisco il buffer contenente i dati scaricati
        return buffer;
        
    }




    //Metodo per ottenere la lista dei peers dal serverTracker
    public List<Integer> getListOfPeers(int trackerPort) throws IOException, ClassNotFoundException {
        //Stabilisco una connessone con il tracker, specifico l'indirizzo del tracker a cui il peer desidera connettersi per ottenere la lista dei peer attivi
        //L'InetSocketAddress combina sia l'indirizzo IP (implicitamente definito) che la porta del Tracker
        InetSocketAddress serverAddress = new InetSocketAddress(trackerPort);

        //Creo un oggetto Socket e passando alla connect l'InetSocketAddress creato precedentemente si stabilisce una connessione TCP tra il peer e il tracker sulla porta specificata
        Socket connectionTracker = new Socket();
        connectionTracker.connect(serverAddress);


        //Dopo aver stabilito la connessione recupero l'input stream della socket, in questo modo all'interno dell'oggetto letto c'è una lista di Peers che rappresenta la lista dei peer attivi inviata dal Tracker
        ObjectInputStream stream = new ObjectInputStream (connectionTracker.getInputStream());

        //Il cast è necessario perché il metodo readObject() restituisce un riferimento di tipo Object
        List<Integer> peers = (List<Integer>) stream.readObject();


        //Chiudo lo stream e il socket per liberare le risorse e terminare la connessione al Tracker
        stream.close();
        connectionTracker.close();

        //Restituisco la lista di peer attivi ricevuta dal Tracker
        return peers;

    }


    public boolean getDownloaded() {
        return this.downloaded;
    }

    public int getPort() {
        return this.port;
    }










}

