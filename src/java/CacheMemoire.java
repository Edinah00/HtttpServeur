import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class CacheMemoire {
    
    public static class EntreeCache {
        private final String cheminFichier;
        private final byte[] contenu;
        private final String typeMime;
        private final int tailleContenu;
        private final long dateModificationFichier;
        private final long dateMiseEnCache;
        
        public EntreeCache(String cheminFichier, byte[] contenu, String typeMime, 
                          long dateModificationFichier) {
            this.cheminFichier = cheminFichier;
            this.contenu = contenu;
            this.typeMime = typeMime;
            this.tailleContenu = contenu.length;
            this.dateModificationFichier = dateModificationFichier;
            this.dateMiseEnCache = System.currentTimeMillis();
        }
        
        public String getCheminFichier() {
            return cheminFichier;
        }
        
        public byte[] getContenu() {
            return contenu;
        }
        
        public String getTypeMime() {
            return typeMime;
        }
        
        public int getTailleContenu() {
            return tailleContenu;
        }
        
        public long getDateModificationFichier() {
            return dateModificationFichier;
        }
        
        public long getDateMiseEnCache() {
            return dateMiseEnCache;
        }
        
        public boolean estValide(Path cheminFichier, long ttlMillis) {
            try {
                if (!Files.exists(cheminFichier)) {
                    return false;
                }
                
                FileTime lastModified = Files.getLastModifiedTime(cheminFichier);
                if (lastModified.toMillis() != dateModificationFichier) {
                    return false;
                }
                
                long age = System.currentTimeMillis() - dateMiseEnCache;
                if (age > ttlMillis) {
                    return false;
                }
                
                return true;
                
            } catch (IOException e) {
                return false;
            }
        }
    }
    
    public static class StatistiqueFichier {
        private final String nomFichier;
        private long hits;
        private long miss;
        
        public StatistiqueFichier(String nomFichier) {
            this.nomFichier = nomFichier;
            this.hits = 0;
            this.miss = 0;
        }
        
        public void incrementerHits() {
            hits++;
        }
        
        public void incrementerMiss() {
            miss++;
        }
        
        public String getNomFichier() {
            return nomFichier;
        }
        
        public long getHits() {
            return hits;
        }
        
        public long getMiss() {
            return miss;
        }
        
        public long getTotal() {
            return hits + miss;
        }
        
        public double getTauxHit() {
            long total = getTotal();
            if (total == 0) {
                return 0.0;
            }
            return (double) hits / total * 100.0;
        }
    }
    
    private final int tailleMaximale;
    private final long tailleMaxMemoire;
    private final long ttlMillis;
    private final Map<String, EntreeCache> cache;
    private long nombreHits;
    private long nombreMiss;
    private long tailleActuelle;
    private final Map<String, StatistiqueFichier> statistiquesParFichier;
    
    public CacheMemoire() {
        this(100, 50 * 1024 * 1024, 5 * 60 * 1000);
    }
    
    public CacheMemoire(int tailleMaximale, long tailleMaxMemoire, long ttlMillis) {
        this.tailleMaximale = tailleMaximale;
        this.tailleMaxMemoire = tailleMaxMemoire;
        this.ttlMillis = ttlMillis;
        this.nombreHits = 0;
        this.nombreMiss = 0;
        this.tailleActuelle = 0;
        this.statistiquesParFichier = new LinkedHashMap<>();
        
        this.cache = new LinkedHashMap<String, EntreeCache>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, EntreeCache> eldest) {
                if (size() > tailleMaximale) {
                    tailleActuelle -= eldest.getValue().getTailleContenu();
                    return true;
                }
                return false;
            }
        };
    }
    
    public synchronized EntreeCache obtenir(String cheminFichier) {
        EntreeCache entree = cache.get(cheminFichier);
        
        // Obtenir ou créer les stats pour ce fichier
        StatistiqueFichier stats = statistiquesParFichier.get(cheminFichier);
        if (stats == null) {
            stats = new StatistiqueFichier(cheminFichier);
            statistiquesParFichier.put(cheminFichier, stats);
        }
        
        if (entree == null) {
            nombreMiss++;
            stats.incrementerMiss();
            return null;
        }
        
        Path path = Path.of(entree.getCheminFichier());
        if (!entree.estValide(path, ttlMillis)) {
            cache.remove(cheminFichier);
            tailleActuelle -= entree.getTailleContenu();
            nombreMiss++;
            stats.incrementerMiss();
            return null;
        }
        
        nombreHits++;
        stats.incrementerHits();
        return entree;
    }
    
    public synchronized boolean ajouter(String cheminFichier, byte[] contenu, 
                                       String typeMime, long dateModification) {
        if (contenu.length > tailleMaxMemoire / 2) {
            return false;
        }
        
        while (tailleActuelle + contenu.length > tailleMaxMemoire && !cache.isEmpty()) {
            supprimerPlusAncienne();
        }
        
        EntreeCache ancienne = cache.get(cheminFichier);
        if (ancienne != null) {
            tailleActuelle -= ancienne.getTailleContenu();
        }
        
        EntreeCache nouvelle = new EntreeCache(cheminFichier, contenu, typeMime, dateModification);
        cache.put(cheminFichier, nouvelle);
        tailleActuelle += contenu.length;
        
        return true;
    }
    
    public static boolean doitEtreMisEnCache(String cheminFichier) {
        if (cheminFichier.endsWith(".php")) {
            return false;
        }
        
        String extension = obtenirExtension(cheminFichier);
        switch (extension) {
            case ".html":
            case ".htm":
            case ".css":
            case ".js":
            case ".json":
            case ".jpg":
            case ".jpeg":
            case ".png":
            case ".gif":
            case ".svg":
            case ".ico":
            case ".woff":
            case ".woff2":
            case ".ttf":
            case ".eot":
            case ".txt":
            case ".xml":
            case ".webp":
            case ".bmp":
                return true;
            default:
                return false;
        }
    }
    
    private static String obtenirExtension(String nomFichier) {
        int indexPoint = nomFichier.lastIndexOf('.');
        if (indexPoint > 0) {
            return nomFichier.substring(indexPoint);
        }
        return "";
    }
    
    private void supprimerPlusAncienne() {
        if (cache.isEmpty()) {
            return;
        }
        
        Map.Entry<String, EntreeCache> premiere = cache.entrySet().iterator().next();
        tailleActuelle -= premiere.getValue().getTailleContenu();
        cache.remove(premiere.getKey());
    }
    
    public synchronized void invalider(String cheminFichier) {
        EntreeCache entree = cache.remove(cheminFichier);
        if (entree != null) {
            tailleActuelle -= entree.getTailleContenu();
        }
    }
    
    public synchronized void vider() {
        cache.clear();
        tailleActuelle = 0;
        statistiquesParFichier.clear();
    }
    
    public synchronized int getTaille() {
        return cache.size();
    }
    
    public synchronized long getTailleMemoire() {
        return tailleActuelle;
    }
    
    public synchronized double getTauxHit() {
        long total = nombreHits + nombreMiss;
        if (total == 0) {
            return 0.0;
        }
        return (double) nombreHits / total;
    }
    
    public synchronized void afficherStatistiques() {
        long total = nombreHits + nombreMiss;
        double tauxHit = getTauxHit() * 100;
        
        System.out.println("\nSTATISTIQUES GLOBALES DU CACHE");
        System.out.println("Entrées : " + cache.size() + "/" + tailleMaximale);
        System.out.println("Mémoire : " + formatTaille(tailleActuelle) + "/" + formatTaille(tailleMaxMemoire));
        System.out.println("Hits : " + nombreHits);
        System.out.println("Miss : " + nombreMiss);
        System.out.println("Total : " + total);
        System.out.println("Taux de hit : " + String.format("%.2f%%", tauxHit));
        
        if (!statistiquesParFichier.isEmpty()) {
            afficherStatistiquesParProjet();
        }
    }
    
    public synchronized void afficherStatistiquesParProjet() {
        // Grouper les fichiers par projet
        Map<String, java.util.List<StatistiqueFichier>> parProjet = new LinkedHashMap<>();
        
        for (StatistiqueFichier stat : statistiquesParFichier.values()) {
            String chemin = stat.getNomFichier();
            String projet = extraireProjet(chemin);
            
            if (!parProjet.containsKey(projet)) {
                parProjet.put(projet, new java.util.ArrayList<>());
            }
            parProjet.get(projet).add(stat);
        }
        
        System.out.println("STATISTIQUES PAR PROJET\n");
        
        for (Map.Entry<String, java.util.List<StatistiqueFichier>> entry : parProjet.entrySet()) {
            String projet = entry.getKey();
            java.util.List<StatistiqueFichier> fichiers = entry.getValue();
            
            // Calculer les totaux du projet
            long totalHits = 0;
            long totalMiss = 0;
            for (StatistiqueFichier stat : fichiers) {
                totalHits += stat.getHits();
                totalMiss += stat.getMiss();
            }
            long totalProjet = totalHits + totalMiss;
            double tauxHitProjet = totalProjet > 0 ? (double) totalHits / totalProjet * 100.0 : 0.0;
            
            System.out.println("┌─ PROJET: " + projet);
            System.out.println("│  Total requêtes: " + totalProjet + " (Hits: " + totalHits + 
                             ", Miss: " + totalMiss + ", Taux: " + String.format("%.1f%%", tauxHitProjet) + ")");
            System.out.println("│");
            
            // Afficher chaque fichier du projet
            for (int i = 0; i < fichiers.size(); i++) {
                StatistiqueFichier stat = fichiers.get(i);
                String nomFichier = extraireNomFichier(stat.getNomFichier());
                boolean estDernier = (i == fichiers.size() - 1);
                String prefixe = estDernier ? "└──" : "├──";
                
                System.out.println("│  " + prefixe + " " + nomFichier);
                System.out.println("│      Hits: " + stat.getHits() + 
                                 " | Miss: " + stat.getMiss() + 
                                 " | Total: " + stat.getTotal() + 
                                 " | Taux: " + String.format("%.1f%%", stat.getTauxHit()));
                
                if (!estDernier) {
                    System.out.println("│");
                }
            }
            System.out.println("└─────────────────────────────────────────────\n");
        }
    }
    
    private String extraireProjet(String cheminComplet) {
        // Exemple: /home/user/www/projet1/index.html -> projet1
        // ou www/projet1/index.html -> projet1
        String chemin = cheminComplet.replace("\\", "/");
        
        if (chemin.contains("/www/")) {
            String apresWww = chemin.substring(chemin.indexOf("/www/") + 5);
            int prochainSlash = apresWww.indexOf("/");
            if (prochainSlash > 0) {
                return apresWww.substring(0, prochainSlash);
            }
            return "[racine]";
        }
        
        // Si pas de structure www/, essayer d'extraire le dossier parent
        String[] parties = chemin.split("/");
        if (parties.length > 2) {
            return parties[parties.length - 2];
        }
        
        return "[racine]";
    }
    
    private String extraireNomFichier(String cheminComplet) {
        String chemin = cheminComplet.replace("\\", "/");
        int dernierSlash = chemin.lastIndexOf("/");
        if (dernierSlash >= 0) {
            return chemin.substring(dernierSlash + 1);
        }
        return chemin;
    }
    
    private String formatTaille(long octets) {
        if (octets < 1024) {
            return octets + " B";
        } else if (octets < 1024 * 1024) {
            return String.format("%.2f KB", octets / 1024.0);
        } else {
            return String.format("%.2f MB", octets / (1024.0 * 1024.0));
        }
    }
    
    public synchronized void reinitialiserStatistiques() {
        nombreHits = 0;
        nombreMiss = 0;
    }

    public synchronized long getNombreHits() {
        return nombreHits;
    }

    public synchronized long getNombreMiss() {
        return nombreMiss;
    }

    public synchronized java.util.List<EntreeCache> obtenirToutesLesEntrees() {
        return new java.util.ArrayList<>(cache.values());
    }
}