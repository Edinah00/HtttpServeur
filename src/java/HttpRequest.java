public class HttpRequest {
    private final String methode;
    private final String chemin;
    private final String version;
    private String[] nomsEntetes;
    private String[] valeursEntetes;
    private int nombreEntetes;
    private String corps;
    
    public HttpRequest(String methode, String chemin, String version) {
        this.methode = methode;
        this.chemin = chemin;
        this.version = version;
        this.nomsEntetes = new String[50];
        this.valeursEntetes = new String[50];
        this.nombreEntetes = 0;
        this.corps = "";
    }
    
    // Ajoute un en-tete HTTP a la requete
    public void ajouterEntete(String nom, String valeur) {
        if (nombreEntetes < nomsEntetes.length) {
            nomsEntetes[nombreEntetes] = nom;
            valeursEntetes[nombreEntetes] = valeur;
            nombreEntetes++;
        }
    }
    
    // Recupere la valeur d'un en-tete par son nom
    public String obtenirEntete(String nom) {
        for (int i = 0; i < nombreEntetes; i++) {
            if (nomsEntetes[i].equalsIgnoreCase(nom)) {
                return valeursEntetes[i];
            }
        }
        return null;
    }
    
    // Definit le corps de la requete (pour POST)
    public void definirCorps(String corps) {
        this.corps = corps;
    }
    
    public String obtenirMethode() {
        return methode;
    }
    
    public String obtenirChemin() {
        return chemin;
    }
    
    public String obtenirVersion() {
        return version;
    }
    
    public String obtenirCorps() {
        return corps;
    }
}