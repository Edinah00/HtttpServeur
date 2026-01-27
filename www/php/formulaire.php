<?php
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $nom = isset($_POST['nom']) ? $_POST['nom'] : 'Inconnu';
    echo "<h2>Bonjour, $nom!</h2>";
} else {
?>
    <form method="POST">
        <label>Nom: <input type="text" name="nom"></label>
        <button type="submit">Envoyer</button>
    </form>
<?php
}
?>