<?php
// Récupérer le cookie depuis les variables d'environnement HTTP
// Quand PHP est exécuté en ligne de commande, les cookies ne sont pas dans $_COOKIE
// mais on peut les récupérer depuis HTTP_COOKIE si disponible

$sessionId = 'Non disponible';

// Essayer de récupérer depuis les variables d'environnement
if (isset($_SERVER['HTTP_COOKIE'])) {
    $cookies = $_SERVER['HTTP_COOKIE'];
    if (preg_match('/SESSIONID=([^;]+)/', $cookies, $matches)) {
        $sessionId = $matches[1];
    }
}

// Compter les visites
$fichierCompteur = '/tmp/compteur_' . md5($sessionId) . '.txt';
if (file_exists($fichierCompteur)) {
    $visites = (int)file_get_contents($fichierCompteur);
    $visites++;
} else {
    $visites = 1;
}
file_put_contents($fichierCompteur, $visites);

?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Test de Session</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 800px;
            margin: 50px auto;
            padding: 20px;
            background: #f5f5f5;
        }
        .session-box {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .session-id {
            background: #e8f4f8;
            padding: 15px;
            border-left: 4px solid #0066cc;
            margin: 20px 0;
            font-family: monospace;
            word-break: break-all;
            font-size: 12px;
        }
        .compteur {
            font-size: 48px;
            color: #0066cc;
            text-align: center;
            margin: 30px 0;
        }
        button {
            background: #0066cc;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 5px;
            cursor: pointer;
            font-size: 16px;
            margin: 5px;
        }
        button:hover {
            background: #0052a3;
        }
    </style>
</head>
<body>
    <div class="session-box">
        <h1> Test de Session HTTP</h1>
        
        <h2>Informations de session</h2>
        <div class="session-id">
            <strong>ID de session:</strong><br>
            <?php echo htmlspecialchars($sessionId); ?>
        </div>
        
        <h2>Compteur de visites</h2>
        <div class="compteur">
            <?php echo $visites; ?>
        </div>
        <p style="text-align: center; color: #666;">
            <?php 
            if ($visites == 1) {
                echo "C'est votre première visite !";
            } else {
                echo "Vous avez visité cette page $visites fois avec cette session.";
            }
            ?>
        </p>
        
        <hr style="margin: 30px 0;">
        
        <h2>Tests</h2>
        <ul>
            <li><strong>Rafraîchir la page</strong> : Le compteur augmente (même session)</li>
            <li><strong>Nouveau navigateur</strong> : Nouveau compteur (nouvelle session)</li>
            <li><strong>Fermer et rouvrir</strong> : Session persiste (cookie)</li>
        </ul>
        
        <button onclick="location.reload()">Rafraîchir</button>
        <button onclick="location.href='/'">Retour accueil</button>
        
        <hr style="margin: 30px 0;">
        
        <h3>Debug Info</h3>
        <pre style="background: #f0f0f0; padding: 10px; font-size: 11px;">
HTTP_COOKIE: <?php echo isset($_SERVER['HTTP_COOKIE']) ? $_SERVER['HTTP_COOKIE'] : 'Non défini'; ?>
        </pre>
    </div>
</body>
</html>