import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class PhpExecutor {
    private static final String COMMANDE_PHP = "php";
    
    // Execute un script PHP via le module PHP interne
    public String executer(String cheminScript, String methode, String chaineQuery, String corps, String cookies) 
            throws Exception {
        
        // Lire le contenu du script PHP
        Path scriptPath = Path.of(cheminScript);
        if (!Files.exists(scriptPath)) {
            throw new FileNotFoundException("Script PHP introuvable: " + cheminScript);
        }
        
        // Construire la commande PHP
        ProcessBuilder constructeurProcessus = new ProcessBuilder(COMMANDE_PHP, cheminScript);
        
        // Configurer les variables d'environnement pour simuler une requête HTTP
        constructeurProcessus.environment().put("REQUEST_METHOD", methode);
        constructeurProcessus.environment().put("SCRIPT_FILENAME", cheminScript);
        
        if (chaineQuery != null && !chaineQuery.isEmpty()) {
            constructeurProcessus.environment().put("QUERY_STRING", chaineQuery);
        }
        
        if ("POST".equalsIgnoreCase(methode) && corps != null) {
            constructeurProcessus.environment().put("CONTENT_LENGTH", String.valueOf(corps.length()));
            constructeurProcessus.environment().put("CONTENT_TYPE", "application/x-www-form-urlencoded");
        }
        
        // Ajouter les cookies HTTP
        if (cookies != null && !cookies.isEmpty()) {
            constructeurProcessus.environment().put("HTTP_COOKIE", cookies);
        }
        
        // Démarrer le processus PHP
        Process processus = constructeurProcessus.start();
        
        // Envoyer le corps de la requête pour POST via stdin
        if ("POST".equalsIgnoreCase(methode) && corps != null) {
            OutputStream entreePHP = processus.getOutputStream();
            entreePHP.write(corps.getBytes());
            entreePHP.flush();
            entreePHP.close();
        }
        
        // Lire la sortie du script PHP
        String sortie = lireFlux(processus.getInputStream());
        String erreurs = lireFlux(processus.getErrorStream());
        
        // Attendre la fin du processus
        int codeRetour = processus.waitFor();
        
        // Si erreur, afficher les messages d'erreur
        if (codeRetour != 0 && !erreurs.isEmpty()) {
            System.err.println("Erreur PHP: " + erreurs);
        }
        
        return sortie;
    }
    
    // Lit le contenu d'un flux d'entrée
    private String lireFlux(InputStream flux) throws IOException {
        BufferedReader lecteur = new BufferedReader(new InputStreamReader(flux));
        StringBuilder constructeur = new StringBuilder();
        String ligne;
        
        while ((ligne = lecteur.readLine()) != null) {
            constructeur.append(ligne).append("\n");
        }
        
        return constructeur.toString();
    }
}