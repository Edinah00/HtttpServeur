import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpClient implements Runnable {
    private final Socket socketClient;
    private final String racineDocuments;
    private final Logger journaliseur;
    private final CacheMemoire cache;
    private final GestionSession gestionSession;
    
    public HttpClient(Socket socket, String racineDocuments, Logger journaliseur, CacheMemoire cache, GestionSession gestionSession) {
        this.socketClient = socket;
        this.racineDocuments = racineDocuments;
        this.journaliseur = journaliseur;
        this.cache = cache;
        this.gestionSession = gestionSession;
    }
    
    @Override
    public void run() {
        try {
            BufferedReader entree = new BufferedReader(
                new InputStreamReader(socketClient.getInputStream())
            );
            OutputStream sortie = socketClient.getOutputStream();
            
            HttpRequest requete = analyserRequete(entree);
            
            if (requete == null) {
                envoyerReponseErreur(sortie, 400, "Bad Request");
                return;
            }
            
            // Gérer la session
            String sessionId = extraireCookieSession(requete);
            GestionSession.Session session = null;
            
            if (sessionId != null) {
                session = gestionSession.obtenirSession(sessionId);
            }
            
            if (session == null) {
                // Créer une nouvelle session
                session = gestionSession.creerSession();
            }
            
            journaliseur.enregistrer(
                socketClient.getInetAddress().getHostAddress(),
                requete.obtenirMethode(),
                requete.obtenirChemin(),
                0
            );
            
            HttpResponse reponse = traiterRequete(requete);
            
            // Ajouter le cookie de session
            reponse.ajouterEntete("Set-Cookie", "SESSIONID=" + session.getId() + "; Path=/; HttpOnly");
            
            envoyerReponse(sortie, reponse);
            
            journaliseur.mettreAJourDernierCode(reponse.obtenirCodeStatut());
            
        } catch (IOException e) {
            System.err.println("Erreur traitement client: " + e.getMessage());
        } finally {
            try {
                socketClient.close();
            } catch (IOException e) {
                System.err.println("Erreur fermeture socket: " + e.getMessage());
            }
        }
    }
    
    // Extrait l'ID de session depuis le cookie
    private String extraireCookieSession(HttpRequest requete) {
        String cookieHeader = requete.obtenirEntete("Cookie");
        if (cookieHeader == null) {
            return null;
        }
        
        // Chercher SESSIONID dans les cookies
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            cookie = cookie.trim();
            if (cookie.startsWith("SESSIONID=")) {
                return cookie.substring("SESSIONID=".length());
            }
        }
        
        return null;
    }
    
    // Analyse la requete HTTP recue du client
    private HttpRequest analyserRequete(BufferedReader entree) throws IOException {
        String ligneRequete = entree.readLine();
        if (ligneRequete == null || ligneRequete.isEmpty()) {
            return null;
        }
        
        String[] parties = ligneRequete.split(" ");
        if (parties.length < 3) {
            return null;
        }
        
        String methode = parties[0];
        String chemin = parties[1];
        String version = parties[2];
        
        HttpRequest requete = new HttpRequest(methode, chemin, version);
        
        // Lecture des en-tetes
        String ligne;
        while ((ligne = entree.readLine()) != null && !ligne.isEmpty()) {
            int indexDeuxPoints = ligne.indexOf(':');
            if (indexDeuxPoints > 0) {
                String nomEntete = ligne.substring(0, indexDeuxPoints).trim();
                String valeurEntete = ligne.substring(indexDeuxPoints + 1).trim();
                requete.ajouterEntete(nomEntete, valeurEntete);
            }
        }
        
        // Lecture du corps pour POST
        if ("POST".equalsIgnoreCase(methode)) {
            String longueurContenu = requete.obtenirEntete("Content-Length");
            if (longueurContenu != null) {
                int longueur = Integer.parseInt(longueurContenu);
                char[] corps = new char[longueur];
                entree.read(corps, 0, longueur);
                requete.definirCorps(new String(corps));
            }
        }
        
        return requete;
    }
    
    // Traite la requete et retourne la reponse appropriee
    private HttpResponse traiterRequete(HttpRequest requete) {
        String chemin = requete.obtenirChemin();
        
        // Retirer les parametres de requete
        int indexQuery = chemin.indexOf('?');
        String chaineQuery = "";
        if (indexQuery != -1) {
            chaineQuery = chemin.substring(indexQuery + 1);
            chemin = chemin.substring(0, indexQuery);
        }
        
        // Securite: empecher l'acces aux repertoires parents
        if (chemin.contains("..")) {
            return new HttpResponse(403, "Forbidden");
        }
                
        // Chemin par defaut
        if (chemin.equals("/")) {
            chemin = "/index.php";  // Essayer PHP d'abord
            Path cheminPhp = Paths.get(racineDocuments, chemin);
            if (!Files.exists(cheminPhp)) {
                chemin = "/index.html";  // Sinon HTML
            }
        }

        // Si le chemin se termine par /, chercher index.php puis index.html
        if (chemin.endsWith("/")) {
            Path cheminPhp = Paths.get(racineDocuments, chemin + "index.php");
            if (Files.exists(cheminPhp)) {
                chemin = chemin + "index.php";
            } else {
                chemin = chemin + "index.html";
            }
        }
        
        Path cheminFichier = Paths.get(racineDocuments, chemin);
        
        // Verifier si c'est un script PHP
        if (chemin.endsWith(".php")) {
            return traiterRequetePhp(cheminFichier, requete, chaineQuery);
        }
        
        // Servir fichier statique
        return traiterFichierStatique(cheminFichier);
    }
    
    // Gere les fichiers statiques (HTML, CSS, JS, images)
    private HttpResponse traiterFichierStatique(Path cheminFichier) {
        try {
            if (!Files.exists(cheminFichier)) {
                return new HttpResponse(404, "Not Found");
            }
            
            if (!Files.isRegularFile(cheminFichier)) {
                return new HttpResponse(403, "Forbidden");
            }
            
            String cheminAbsolu = cheminFichier.toAbsolutePath().toString();
            String nomFichier = cheminFichier.getFileName().toString();
            
            // Vérifier si le fichier doit être mis en cache
            if (CacheMemoire.doitEtreMisEnCache(cheminAbsolu)) {
                // Essayer de récupérer depuis le cache
                CacheMemoire.EntreeCache entreeCache = cache.obtenir(cheminAbsolu);
                
                if (entreeCache != null) {
                    System.out.println("[CACHE HIT] ✓ " + nomFichier + " - Servi depuis le cache (RAM)");
                    HttpResponse reponse = new HttpResponse(200, "OK");
                    reponse.definirTypeContenu(entreeCache.getTypeMime());
                    reponse.definirCorps(entreeCache.getContenu());
                    return reponse;
                }
                
                System.out.println("[CACHE MISS] ✗ " + nomFichier + " - Lecture depuis le disque + mise en cache");
                byte[] contenu = Files.readAllBytes(cheminFichier);
                String typeContenu = obtenirTypeContenu(cheminFichier.toString());
                long dateModification = Files.getLastModifiedTime(cheminFichier).toMillis();
                
                cache.ajouter(cheminAbsolu, contenu, typeContenu, dateModification);
                
                HttpResponse reponse = new HttpResponse(200, "OK");
                reponse.definirTypeContenu(typeContenu);
                reponse.definirCorps(contenu);
                
                return reponse;
            } else {
                // Fichier ne doit pas être mis en cache - lecture directe
                byte[] contenu = Files.readAllBytes(cheminFichier);
                String typeContenu = obtenirTypeContenu(cheminFichier.toString());
                
                HttpResponse reponse = new HttpResponse(200, "OK");
                reponse.definirTypeContenu(typeContenu);
                reponse.definirCorps(contenu);
                
                return reponse;
            }
            
        } catch (IOException e) {
            return new HttpResponse(500, "Internal Server Error");
        }
    }
    
    // Execute un script PHP via le module PHP interne
    private HttpResponse traiterRequetePhp(Path cheminFichier, HttpRequest requete, String chaineQuery) {
        if (!Files.exists(cheminFichier)) {
            return new HttpResponse(404, "Not Found");
        }
        
        PhpHandler gestionnairePhp = new PhpHandler();
        try {
            // Récupérer les cookies depuis la requête
            String cookies = requete.obtenirEntete("Cookie");
            
            String sortie = gestionnairePhp.executer(
                cheminFichier.toAbsolutePath().toString(),
                requete.obtenirMethode(),
                chaineQuery,
                requete.obtenirCorps(),
                cookies
            );
            
            HttpResponse reponse = new HttpResponse(200, "OK");
            reponse.definirTypeContenu("text/html");
            reponse.definirCorps(sortie.getBytes());
            
            return reponse;
            
        } catch (Exception e) {
            System.err.println("Erreur execution PHP: " + e.getMessage());
            return new HttpResponse(500, "Internal Server Error");
        }
    }
    
    // Determine le type MIME en fonction de l'extension du fichier
    private String obtenirTypeContenu(String nomFichier) {
        if (nomFichier.endsWith(".html") || nomFichier.endsWith(".htm")) {
            return "text/html";
        } else if (nomFichier.endsWith(".css")) {
            return "text/css";
        } else if (nomFichier.endsWith(".js")) {
            return "application/javascript";
        } else if (nomFichier.endsWith(".json")) {
            return "application/json";
        } else if (nomFichier.endsWith(".jpg") || nomFichier.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (nomFichier.endsWith(".png")) {
            return "image/png";
        } else if (nomFichier.endsWith(".gif")) {
            return "image/gif";
        } else if (nomFichier.endsWith(".txt")) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }
    
    // Envoie la reponse HTTP au client
    private void envoyerReponse(OutputStream sortie, HttpResponse reponse) throws IOException {
        PrintWriter writer = new PrintWriter(sortie, false);
        
        // Ligne de statut
        writer.print("HTTP/1.0 " + reponse.obtenirCodeStatut() + " " + 
                     reponse.obtenirMessageStatut() + "\r\n");
        
        // En-tetes standards
        writer.print("Content-Type: " + reponse.obtenirTypeContenu() + "\r\n");
        writer.print("Content-Length: " + reponse.obtenirLongueurContenu() + "\r\n");
        writer.print("Connection: close\r\n");
        
        // En-têtes personnalisés (comme Set-Cookie)
        for (java.util.Map.Entry<String, String> entete : reponse.obtenirEntetes().entrySet()) {
            writer.print(entete.getKey() + ": " + entete.getValue() + "\r\n");
        }
        
        writer.print("\r\n");
        writer.flush();
        
        // Corps
        if (reponse.obtenirCorps() != null) {
            sortie.write(reponse.obtenirCorps());
            sortie.flush();
        }
    }
    
    // Envoie une reponse d'erreur simple
    private void envoyerReponseErreur(OutputStream sortie, int codeStatut, String message) 
            throws IOException {
        HttpResponse reponse = new HttpResponse(codeStatut, message);
        envoyerReponse(sortie, reponse);
    }
}