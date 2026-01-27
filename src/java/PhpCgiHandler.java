import java.io.*;

public class PhpCgiHandler {
    private static final String COMMANDE_PHP_CGI = "php-cgi";
    
    // Execute un script PHP via l'interface CGI
    public String executer(String cheminScript, String methode, String chaineQuery, String corps) 
            throws Exception {
        
        ProcessBuilder constructeurProcessus = new ProcessBuilder(COMMANDE_PHP_CGI, cheminScript);
        
        // Preparation des variables d'environnement CGI
        String[] environnement = constructeurProcessus.environment().keySet().toArray(new String[0]);
        for (String cle : environnement) {
            constructeurProcessus.environment().remove(cle);
        }
        
        constructeurProcessus.environment().put("REQUEST_METHOD", methode);
        constructeurProcessus.environment().put("SCRIPT_FILENAME", cheminScript);
        constructeurProcessus.environment().put("REDIRECT_STATUS", "200");
        
        if (chaineQuery != null && !chaineQuery.isEmpty()) {
            constructeurProcessus.environment().put("QUERY_STRING", chaineQuery);
        }
        
        if ("POST".equalsIgnoreCase(methode) && corps != null) {
            constructeurProcessus.environment().put("CONTENT_LENGTH", String.valueOf(corps.length()));
            constructeurProcessus.environment().put("CONTENT_TYPE", "application/x-www-form-urlencoded");
        }
        
        Process processus = constructeurProcessus.start();
        
        // Envoyer le corps de la requete pour POST
        if ("POST".equalsIgnoreCase(methode) && corps != null) {
            OutputStream entreePHP = processus.getOutputStream();
            entreePHP.write(corps.getBytes());
            entreePHP.flush();
            entreePHP.close();
        }
        
        // Lire la sortie de php-cgi
        String sortie = lireFlux(processus.getInputStream());
        
        // Attendre la fin du processus
        processus.waitFor();
        
        // Extraire le contenu HTML de la sortie CGI
        return extraireContenuHtml(sortie);
    }
    
    // Lit le contenu d'un flux d'entree
    private String lireFlux(InputStream flux) throws IOException {
        BufferedReader lecteur = new BufferedReader(new InputStreamReader(flux));
        StringBuilder constructeur = new StringBuilder();
        String ligne;
        
        while ((ligne = lecteur.readLine()) != null) {
            constructeur.append(ligne).append("\n");
        }
        
        return constructeur.toString();
    }
    
    // Extrait le contenu HTML apres les en-tetes CGI
    private String extraireContenuHtml(String sortieCgi) {
        int indexSeparateur = sortieCgi.indexOf("\n\n");
        if (indexSeparateur != -1) {
            return sortieCgi.substring(indexSeparateur + 2);
        }
        
        indexSeparateur = sortieCgi.indexOf("\r\n\r\n");
        if (indexSeparateur != -1) {
            return sortieCgi.substring(indexSeparateur + 4);
        }
        
        return sortieCgi;
    }
}