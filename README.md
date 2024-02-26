# Progetto-Reti-dei-Calcolatori
Dopo l’avvio i peer inizieranno automaticamente a comunicare con il server 
Tracker per ottenere l’elenco dei peer attivi e coordinare la condivisione dei file. 
Gli utenti possono monitorare l’output sul terminale per vedere lo stato delle 
operazioni di condivisione e di download. 
Per inserire un nuovo peer, bisogna creare un oggetto di tipo Peers passandogli in 
input la sua porta e la porta del tracker alla quale deve collegarsi, successivamente 
bisogna dargli il comando di avvio tramite il metodo .start().  
È necessario inserire un ciclo per permettere la progressione del codice finché il 
peer non scarica il file. 
Infine, il peer viene aggiunto alla lista dei peer che hanno il file invocando il 
metodo .notifyPeer(“Nome peer aggiunto”). 
