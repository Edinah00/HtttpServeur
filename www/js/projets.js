// Charge et affiche la liste des projets depuis projets.json
document.addEventListener('DOMContentLoaded', function() {
    const listeProjets = document.getElementById('projets-liste');
    
    if (!listeProjets) {
        console.error('Element projets-liste non trouve');
        return;
    }
    
    // Charger le fichier JSON
    fetch('/projets.json')
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Erreur de chargement du fichier projets.json');
            }
            return response.json();
        })
        .then(function(projets) {
            // Vider le message de chargement
            listeProjets.innerHTML = '';
            
            // Si aucun projet
            if (projets.length === 0) {
                listeProjets.innerHTML = '<p class="no-projects">Aucun projet disponible pour le moment.</p>';
                return;
            }
            
            // Créer une carte pour chaque projet
            projets.forEach(function(projet) {
                const carte = document.createElement('div');
                carte.className = 'projet-card';
                
                const titre = document.createElement('h3');
                titre.textContent = projet.nom;
                
                const description = document.createElement('p');
                description.textContent = projet.description;
                
                const lien = document.createElement('a');
                lien.href = '/' + projet.dossier + '/';
                lien.className = 'btn-primary';
                lien.textContent = 'Voir le projet';
                
                carte.appendChild(titre);
                carte.appendChild(description);
                carte.appendChild(lien);
                
                listeProjets.appendChild(carte);
            });
            
            console.log('Projets charges:', projets.length);
        })
        .catch(function(erreur) {
            console.error('Erreur:', erreur);
            listeProjets.innerHTML = '<p class="no-projects">Erreur de chargement des projets. Verifiez que projets.json existe.</p>';
        });
});