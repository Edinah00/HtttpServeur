document.addEventListener('DOMContentLoaded', function() {
    console.log('Projet 1 chargé avec succès!');
    
    const bouton = document.getElementById('btn-demo');
    const message = document.getElementById('message');
    let compteur = 0;
    
    if (bouton) {
        bouton.addEventListener('click', function() {
            compteur++;
            
            const messages = [
                'Bravo ! Vous avez cliqué.',
                'Le serveur HTTP fonctionne parfaitement !',
                'JavaScript est bien chargé et exécuté.',
                'Clic numéro ' + compteur + ' !',
                'Continuez à explorer le projet !'
            ];
            
            const index = (compteur - 1) % messages.length;
            message.textContent = messages[index];
            
            // Animation
            message.style.transform = 'scale(0.8)';
            setTimeout(function() {
                message.style.transform = 'scale(1)';
            }, 200);
        });
    }
});