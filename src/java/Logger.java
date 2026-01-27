import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private final String cheminFichier;
    private int dernierCodeReponse;
    
    public Logger(String cheminFichier) {
        this.cheminFichier = cheminFichier;
        this.dernierCodeReponse = 0;
    }
    
    // Enregistre une requete HTTP dans le fichier de log
    public synchronized void enregistrer(String adresseIP, String methode, String ressource, 
                                         int codeReponse) {
        try {
            FileWriter ecrivain = new FileWriter(cheminFichier, true);
            BufferedWriter tampon = new BufferedWriter(ecrivain);
            
            String horodatage = obtenirHorodatage();
            String entree = String.format("[%s] %s - %s %s - Code: %d%n", 
                                         horodatage, adresseIP, methode, ressource, codeReponse);
            
            tampon.write(entree);
            tampon.close();
            
        } catch (IOException e) {
            System.err.println("Erreur ecriture log: " + e.getMessage());
        }
    }
    
    // Met a jour le dernier code de reponse pour le log en cours
    public void mettreAJourDernierCode(int codeReponse) {
        this.dernierCodeReponse = codeReponse;
        
        try {
            File fichier = new File(cheminFichier);
            if (!fichier.exists()) {
                return;
            }
            
            RandomAccessFile raf = new RandomAccessFile(fichier, "rw");
            long longueur = raf.length();
            
            if (longueur > 0) {
                raf.seek(longueur - 1);
                
                // Remonter jusqu'a la derniere ligne
                long position = longueur - 1;
                while (position > 0) {
                    raf.seek(position);
                    if (raf.read() == '\n' && position < longueur - 1) {
                        break;
                    }
                    position--;
                }
                
                if (position > 0) {
                    raf.seek(position + 1);
                }
                
                String derniereLigne = raf.readLine();
                if (derniereLigne != null && derniereLigne.contains("Code: 0")) {
                    String nouvelleLigne = derniereLigne.replace("Code: 0", 
                                                                 "Code: " + codeReponse);
                    
                    if (position > 0) {
                        raf.seek(position + 1);
                    } else {
                        raf.seek(0);
                    }
                    
                    raf.writeBytes(nouvelleLigne);
                }
            }
            
            raf.close();
            
        } catch (IOException e) {
            System.err.println("Erreur mise a jour log: " + e.getMessage());
        }
    }
    
    // Genere un horodatage au format standard
    private String obtenirHorodatage() {
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatDate.format(new Date());
    }
}