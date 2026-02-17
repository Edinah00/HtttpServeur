import java.io.*;
import java.net.Socket;

public class TestMultiClients {
    
    public static void main(String[] args) {
        String hote = "localhost";
        int port = 8888;
        int nombreClients = 5;
        
        System.out.println("=== TEST MULTI-THREADING ===");
        System.out.println("Lancement de " + nombreClients + " clients simultanement...\n");
        
        // Lancer plusieurs clients en parallele
        for (int i = 1; i <= nombreClients; i++) {
            final int numeroClient = i;
            Thread thread = new Thread(() -> {
                testerClient(hote, port, numeroClient);
            });
            thread.start();
        }
    }
    
    private static void testerClient(String hote, int port, int numero) {
        try {
            System.out.println("[Client " + numero + "] Connexion au serveur...");
            Socket socket = new Socket(hote, port);
            
            // Envoyer une requete HTTP
            PrintWriter sortie = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader entree = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            sortie.println("GET /index.html HTTP/1.0");
            sortie.println("Host: localhost");
            sortie.println();
            
            System.out.println("[Client " + numero + "] Requete envoyee");
            
            // Lire la reponse
            String ligne;
            int lignesRecues = 0;
            while ((ligne = entree.readLine()) != null && lignesRecues < 5) {
                if (lignesRecues == 0) {
                    System.out.println("[Client " + numero + "] Reponse: " + ligne);
                }
                lignesRecues++;
            }
            
            socket.close();
            System.out.println("[Client " + numero + "] Deconnexion reussie\n");
            
        } catch (Exception e) {
            System.err.println("[Client " + numero + "] Erreur: " + e.getMessage());
        }
    }
}
