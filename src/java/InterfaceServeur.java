import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;

public class InterfaceServeur extends JFrame {
    
    private JTextField champPort;
    private JTextField champRacine;
    private JButton btnDemarrer;
    private JButton btnArreter;
    private JLabel lblStatut;
    
    private JTextArea zoneLogs;
    private JScrollPane scrollLogs;
    
    private JLabel lblTailleCache;
    private JLabel lblMemoireCache;
    private JLabel lblHitsCache;
    private JLabel lblMissCache;
    private JLabel lblTauxHit;
    private JProgressBar barreMemoire;
    private JProgressBar barreTauxHit;
    
    private DefaultTableModel modelTableCache;
    private JTable tableCache;
    
    private JLabel lblNombreRequetes;
    private JLabel lblUptime;
    
    private HttpServer serveur;
    private Thread threadServeur;
    private Timer timerStats;
    private long heureDebut;
    private PrintStream streamOriginal;
    
    public InterfaceServeur() {
        setTitle("Serveur HTTP - Gestion et Monitoring");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComposants();
        redirectSystemOut();
        
        setVisible(true);
    }
    
    private void redirectSystemOut() {
        streamOriginal = System.out;
        
        OutputStream outputStream = new OutputStream() {
            private StringBuilder buffer = new StringBuilder();
            
            @Override
            public void write(int b) throws IOException {
                streamOriginal.write(b);
                buffer.append((char) b);
                if (b == '\n') {
                    final String text = buffer.toString();
                    buffer = new StringBuilder();
                    SwingUtilities.invokeLater(() -> {
                        zoneLogs.append(text);
                        zoneLogs.setCaretPosition(zoneLogs.getDocument().getLength());
                    });
                }
            }
        };
        
        System.setOut(new PrintStream(outputStream, true));
    }
    
    private void initComposants() {
        JTabbedPane onglets = new JTabbedPane();
        
        onglets.addTab("Serveur", creerPanelServeur());
        onglets.addTab("Cache (Temps Réel)", creerPanelCache());
        onglets.addTab("Statistiques", creerPanelStatistiques());
        
        add(onglets);
    }
    
    private JPanel creerPanelServeur() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel panelConfig = new JPanel(new GridBagLayout());
        panelConfig.setBorder(BorderFactory.createTitledBorder("Configuration"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panelConfig.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        champPort = new JTextField("8888", 10);
        panelConfig.add(champPort, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panelConfig.add(new JLabel("Racine documents:"), gbc);
        gbc.gridx = 1;
        champRacine = new JTextField("www", 20);
        panelConfig.add(champRacine, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JPanel panelBoutons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        btnDemarrer = new JButton("Démarrer");
        btnDemarrer.addActionListener(e -> demarrerServeur());
        
        btnArreter = new JButton("Arrêter");
        btnArreter.setEnabled(false);
        btnArreter.addActionListener(e -> arreterServeur());
        
        lblStatut = new JLabel("● Arrêté");
        lblStatut.setFont(new Font("Dialog", Font.BOLD, 14));
        
        panelBoutons.add(btnDemarrer);
        panelBoutons.add(btnArreter);
        panelBoutons.add(Box.createHorizontalStrut(20));
        panelBoutons.add(lblStatut);
        
        panelConfig.add(panelBoutons, gbc);
        
        JPanel panelLogs = new JPanel(new BorderLayout());
        panelLogs.setBorder(BorderFactory.createTitledBorder("Journal d'activité (Temps Réel)"));
        
        zoneLogs = new JTextArea();
        zoneLogs.setEditable(false);
        zoneLogs.setFont(new Font("Monospaced", Font.PLAIN, 11));
        zoneLogs.setBackground(Color.BLACK);
        zoneLogs.setForeground(Color.WHITE);
        scrollLogs = new JScrollPane(zoneLogs);
        
        panelLogs.add(scrollLogs, BorderLayout.CENTER);
        
        JButton btnViderLogs = new JButton("Vider les logs");
        btnViderLogs.addActionListener(e -> zoneLogs.setText(""));
        panelLogs.add(btnViderLogs, BorderLayout.SOUTH);
        
        panel.add(panelConfig, BorderLayout.NORTH);
        panel.add(panelLogs, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel creerPanelCache() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel panelStats = new JPanel(new GridLayout(2, 1, 5, 5));
        
        JPanel panelMetriques = new JPanel(new GridLayout(1, 3, 10, 0));
        panelMetriques.setBorder(BorderFactory.createTitledBorder("Métriques du Cache (Rafraîchissement: 500ms)"));
        
        JPanel panelTaille = creerPanelMetrique("Entrées", "0/100");
        lblTailleCache = (JLabel) ((JPanel)panelTaille.getComponent(1)).getComponent(0);
        panelMetriques.add(panelTaille);
        
        JPanel panelMemoire = creerPanelMetrique("Mémoire", "0 MB / 50 MB");
        lblMemoireCache = (JLabel) ((JPanel)panelMemoire.getComponent(1)).getComponent(0);
        panelMetriques.add(panelMemoire);
        
        JPanel panelTaux = creerPanelMetrique("Taux de Hit", "0%");
        lblTauxHit = (JLabel) ((JPanel)panelTaux.getComponent(1)).getComponent(0);
        panelMetriques.add(panelTaux);
        
        panelStats.add(panelMetriques);
        
        JPanel panelDetails = new JPanel(new GridLayout(1, 2, 10, 0));
        
        JPanel panelHitsMiss = new JPanel(new GridLayout(2, 2, 5, 5));
        panelHitsMiss.setBorder(BorderFactory.createTitledBorder("Accès au Cache"));
        
        panelHitsMiss.add(new JLabel("Cache Hits:"));
        lblHitsCache = new JLabel("0");
        lblHitsCache.setFont(new Font("Dialog", Font.BOLD, 20));
        panelHitsMiss.add(lblHitsCache);
        
        panelHitsMiss.add(new JLabel("Cache Miss:"));
        lblMissCache = new JLabel("0");
        lblMissCache.setFont(new Font("Dialog", Font.BOLD, 20));
        panelHitsMiss.add(lblMissCache);
        
        panelDetails.add(panelHitsMiss);
        
        JPanel panelBarres = new JPanel(new GridLayout(2, 1, 0, 10));
        panelBarres.setBorder(BorderFactory.createTitledBorder("Utilisation"));
        
        JPanel panelBarreMem = new JPanel(new BorderLayout(5, 0));
        panelBarreMem.add(new JLabel("Mémoire:"), BorderLayout.WEST);
        barreMemoire = new JProgressBar(0, 100);
        barreMemoire.setStringPainted(true);
        panelBarreMem.add(barreMemoire, BorderLayout.CENTER);
        
        JPanel panelBarreHit = new JPanel(new BorderLayout(5, 0));
        panelBarreHit.add(new JLabel("Taux Hit:"), BorderLayout.WEST);
        barreTauxHit = new JProgressBar(0, 100);
        barreTauxHit.setStringPainted(true);
        panelBarreHit.add(barreTauxHit, BorderLayout.CENTER);
        
        panelBarres.add(panelBarreMem);
        panelBarres.add(panelBarreHit);
        
        panelDetails.add(panelBarres);
        panelStats.add(panelDetails);
        
        JPanel panelTable = new JPanel(new BorderLayout());
        panelTable.setBorder(BorderFactory.createTitledBorder("Contenu du Cache"));
        
        String[] colonnes = {"Fichier", "Taille", "Type MIME", "Dernière utilisation"};
        modelTableCache = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tableCache = new JTable(modelTableCache);
        tableCache.setFont(new Font("Monospaced", Font.PLAIN, 11));
        tableCache.setRowHeight(25);
        
        JScrollPane scrollTable = new JScrollPane(tableCache);
        panelTable.add(scrollTable, BorderLayout.CENTER);
        
        JPanel panelActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton btnViderCache = new JButton("Vider le Cache");
        btnViderCache.addActionListener(e -> viderCache());
        
        JButton btnRafraichir = new JButton("Rafraîchir Maintenant");
        btnRafraichir.addActionListener(e -> rafraichirAffichageCache());
        
        panelActions.add(btnViderCache);
        panelActions.add(btnRafraichir);
        
        panelTable.add(panelActions, BorderLayout.SOUTH);
        
        panel.add(panelStats, BorderLayout.NORTH);
        panel.add(panelTable, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel creerPanelStatistiques() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Nombre total de requêtes:"), gbc);
        gbc.gridx = 1;
        lblNombreRequetes = new JLabel("0");
        lblNombreRequetes.setFont(new Font("Dialog", Font.BOLD, 20));
        panel.add(lblNombreRequetes, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Durée de fonctionnement:"), gbc);
        gbc.gridx = 1;
        lblUptime = new JLabel("00:00:00");
        lblUptime.setFont(new Font("Dialog", Font.BOLD, 20));
        panel.add(lblUptime, gbc);
        
        return panel;
    }
    
    private JPanel creerPanelMetrique(String titre, String valeurInitiale) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JLabel lblTitre = new JLabel(titre, SwingConstants.CENTER);
        lblTitre.setBorder(new EmptyBorder(5, 5, 2, 5));
        
        JPanel panelValeur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblValeur = new JLabel(valeurInitiale);
        lblValeur.setFont(new Font("Dialog", Font.BOLD, 18));
        panelValeur.add(lblValeur);
        
        panel.add(lblTitre, BorderLayout.NORTH);
        panel.add(panelValeur, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void demarrerServeur() {
        try {
            int port = Integer.parseInt(champPort.getText());
            String racine = champRacine.getText();
            
            serveur = new HttpServer(port, racine);
            
            threadServeur = new Thread(() -> serveur.demarrer());
            threadServeur.start();
            
            btnDemarrer.setEnabled(false);
            btnArreter.setEnabled(true);
            champPort.setEnabled(false);
            champRacine.setEnabled(false);
            
            lblStatut.setText("● En marche");
            
            heureDebut = System.currentTimeMillis();
            
            // Rafraîchissement toutes les 500ms pour voir le cache en temps réel
            timerStats = new Timer(500, e -> rafraichirStatistiques());
            timerStats.start();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, 
                "Port invalide!", 
                "Erreur", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void arreterServeur() {
        if (serveur != null) {
            serveur.arreter();
            
            if (timerStats != null) {
                timerStats.stop();
            }
            
            btnDemarrer.setEnabled(true);
            btnArreter.setEnabled(false);
            champPort.setEnabled(true);
            champRacine.setEnabled(true);
            
            lblStatut.setText("● Arrêté");
        }
    }
    
    private void rafraichirStatistiques() {
        if (serveur != null) {
            rafraichirAffichageCache();
            
            long duree = System.currentTimeMillis() - heureDebut;
            long heures = duree / 3600000;
            long minutes = (duree % 3600000) / 60000;
            long secondes = (duree % 60000) / 1000;
            lblUptime.setText(String.format("%02d:%02d:%02d", heures, minutes, secondes));
        }
    }
    
    private void rafraichirAffichageCache() {
        if (serveur == null) return;
        
        CacheMemoire cache = serveur.getCache();
        
        lblTailleCache.setText(cache.getTaille() + "/100");
        
        long memoireOctets = cache.getTailleMemoire();
        String memoireStr = formatTaille(memoireOctets) + " / 50 MB";
        lblMemoireCache.setText(memoireStr);
        
        double tauxHit = cache.getTauxHit() * 100;
        lblTauxHit.setText(String.format("%.1f%%", tauxHit));
        
        long hits = cache.getNombreHits();
        long miss = cache.getNombreMiss();
        lblHitsCache.setText(String.valueOf(hits));
        lblMissCache.setText(String.valueOf(miss));
        
        int pourcentageMem = (int)((memoireOctets * 100) / (50 * 1024 * 1024));
        barreMemoire.setValue(pourcentageMem);
        barreTauxHit.setValue((int)tauxHit);
        
        // AFFICHAGE RÉEL DU CACHE
        modelTableCache.setRowCount(0);
        
        java.util.List<CacheMemoire.EntreeCache> entrees = cache.obtenirToutesLesEntrees();
        for (CacheMemoire.EntreeCache entree : entrees) {
            String fichier = new java.io.File(entree.getCheminFichier()).getName();
            String taille = formatTaille(entree.getTailleContenu());
            String type = entree.getTypeMime();
            long tempsPasse = (System.currentTimeMillis() - entree.getDateMiseEnCache()) / 1000;
            String utilisation = "Il y a " + tempsPasse + " sec";
            
            modelTableCache.addRow(new Object[]{fichier, taille, type, utilisation});
        }
        
        lblNombreRequetes.setText(String.valueOf(hits + miss));
    }
    
    private void viderCache() {
        if (serveur != null) {
            serveur.viderCache();
            rafraichirAffichageCache();
            JOptionPane.showMessageDialog(this, 
                "Le cache a été vidé avec succès!", 
                "Cache", 
                JOptionPane.INFORMATION_MESSAGE);
        }
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
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InterfaceServeur());
    }
}