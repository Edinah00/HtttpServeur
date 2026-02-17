import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    private final int port;
    private final String racineDocuments;
    private ServerSocket socketServeur;
    private final Logger journaliseur;
    private final CacheMemoire cache;
    
    public HttpServer(int port, String racineDocuments) {
        this.port = port;
        this.racineDocuments = racineDocuments;
        this.journaliseur = new Logger("serveur.log");
        this.cache = new CacheMemoire(); // Cache par dÃ©faut : 100 entrÃ©es, 50 MB, 5 min TTL
    }
    
    // Demarre le serveur et ecoute les connexions entrantes
    public void demarrer() {
        try {
            socketServeur = new ServerSocket(port);
            System.out.println("==========================================");
            System.out.println("Serveur HTTP demarre avec succes!");
            System.out.println("Port: " + port);
            System.out.println("Racine des documents: " + racineDocuments);
            System.out.println("Acces: http://localhost:" + port);
            System.out.println("==========================================");
            System.out.println("En attente de connexions...\n");
            
            while (true) {
                Socket socketClient = socketServeur.accept();
                System.out.println("Nouvelle connexion: " + socketClient.getInetAddress().getHostAddress());
                ClientHandler gestionnaire = new ClientHandler(socketClient, racineDocuments, journaliseur, cache);
                Thread thread = new Thread(gestionnaire);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("==========================================");
            System.err.println("ERREUR: Impossible de demarrer le serveur");
            System.err.println("Raison: " + e.getMessage());
            System.err.println("==========================================");
            System.err.println("\nVerifiez que:");
            System.err.println("- Le port " + port + " n'est pas deja utilise");
            System.err.println("- Vous avez les droits necessaires");
            System.err.println("- Essayez un autre port (ex: 9000, 3000, 5000)");
        }
    }
    
    // Arrete le serveur et ferme le socket
    public void arreter() {
        try {
            if (socketServeur != null && !socketServeur.isClosed()) {
                socketServeur.close();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'arret: " + e.getMessage());
        }
    }
    
    // Affiche les statistiques du cache
    public void afficherStatistiquesCache() {
        cache.afficherStatistiques();
    }
    
    // Vide le cache
    public void viderCache() {
        cache.vider();
        System.out.println("Cache vidÃ©.");
    }
    
    // Retourne l'instance du cache
    public CacheMemoire getCache() {
        return cache;
    }
    
    public static void main(String[] args) {
        int port = 8888;
        String racineDocuments = "www";
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port invalide, utilisation du port par defaut: 8888");
            }
        }
        if (args.length > 1) {
            racineDocuments = args[1];
        }
        
        System.out.println("Tentative de demarrage sur le port " + port);
        HttpServer serveur = new HttpServer(port, racineDocuments);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\n========== ARRET DU SERVEUR ==========");
            serveur.afficherStatistiquesCache();
        }));
        
        serveur.demarrer();
    }
}