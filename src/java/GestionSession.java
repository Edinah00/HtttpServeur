import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GestionSession {
    
    // Classe représentant une session
    public static class Session {
        private final String id;
        private final long dateCreation;
        private long derniereActivite;
        private final Map<String, Object> attributs;
        
        public Session(String id) {
            this.id = id;
            this.dateCreation = System.currentTimeMillis();
            this.derniereActivite = System.currentTimeMillis();
            this.attributs = new HashMap<>();
        }
        
        public String getId() {
            return id;
        }
        
        public long getDateCreation() {
            return dateCreation;
        }
        
        public long getDerniereActivite() {
            return derniereActivite;
        }
        
        public void mettreAJourActivite() {
            this.derniereActivite = System.currentTimeMillis();
        }
        
        public void setAttribute(String nom, Object valeur) {
            attributs.put(nom, valeur);
        }
        
        public Object getAttribute(String nom) {
            return attributs.get(nom);
        }
        
        public void removeAttribute(String nom) {
            attributs.remove(nom);
        }
        
        public Map<String, Object> getTousLesAttributs() {
            return new HashMap<>(attributs);
        }
        
        public boolean estExpiree(long timeoutMillis) {
            long inactivite = System.currentTimeMillis() - derniereActivite;
            return inactivite > timeoutMillis;
        }
    }
    
    // Gestionnaire de sessions (thread-safe)
    private final Map<String, Session> sessions;
    private final long timeoutSession; // en millisecondes (défaut: 30 minutes)
    
    public GestionSession() {
        this(30 * 60 * 1000); // 30 minutes par défaut
    }
    
    public GestionSession(long timeoutMillis) {
        this.sessions = new ConcurrentHashMap<>();
        this.timeoutSession = timeoutMillis;
        
        // Thread de nettoyage des sessions expirées
        demarrerNettoyageAutomatique();
    }
    
    // Génère un ID de session unique
    private String genererIdSession() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    // Crée une nouvelle session
    public Session creerSession() {
        String id = genererIdSession();
        Session session = new Session(id);
        sessions.put(id, session);
        System.out.println("[SESSION] Nouvelle session créée: " + id);
        return session;
    }
    
    // Récupère une session par son ID
    public Session obtenirSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        
        Session session = sessions.get(sessionId);
        
        if (session != null) {
            // Vérifier si la session est expirée
            if (session.estExpiree(timeoutSession)) {
                supprimerSession(sessionId);
                return null;
            }
            
            // Mettre à jour la dernière activité
            session.mettreAJourActivite();
        }
        
        return session;
    }
    
    // Supprime une session
    public void supprimerSession(String sessionId) {
        if (sessions.remove(sessionId) != null) {
            System.out.println("[SESSION] Session supprimée: " + sessionId);
        }
    }
    
    // Nettoie les sessions expirées
    public void nettoyerSessionsExpirees() {
        int compteur = 0;
        Iterator<Map.Entry<String, Session>> iterator = sessions.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, Session> entry = iterator.next();
            if (entry.getValue().estExpiree(timeoutSession)) {
                iterator.remove();
                compteur++;
            }
        }
        
        if (compteur > 0) {
            System.out.println("[SESSION] " + compteur + " session(s) expirée(s) nettoyée(s)");
        }
    }
    
    // Démarre un thread de nettoyage automatique toutes les 5 minutes
    private void demarrerNettoyageAutomatique() {
        Thread threadNettoyage = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5 * 60 * 1000); // 5 minutes
                    nettoyerSessionsExpirees();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        threadNettoyage.setDaemon(true);
        threadNettoyage.start();
    }
    
    // Retourne le nombre de sessions actives
    public int getNombreSessionsActives() {
        return sessions.size();
    }
    
    // Affiche les statistiques des sessions
    public void afficherStatistiques() {
        System.out.println("\nSTATISTIQUES DES SESSIONS");
        System.out.println("Sessions actives: " + sessions.size());
        System.out.println("Timeout: " + (timeoutSession / 60000) + " minutes");
    }
}