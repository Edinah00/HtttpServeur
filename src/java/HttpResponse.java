public class HttpResponse {
    private final int codeStatut;
    private final String messageStatut;
    private String typeContenu;
    private byte[] corps;
    
    public HttpResponse(int codeStatut, String messageStatut) {
        this.codeStatut = codeStatut;
        this.messageStatut = messageStatut;
        this.typeContenu = "text/html";
        this.corps = null;
    }
    
    // Definit le type MIME du contenu
    public void definirTypeContenu(String typeContenu) {
        this.typeContenu = typeContenu;
    }
    
    // Definit le corps de la reponse en bytes
    public void definirCorps(byte[] corps) {
        this.corps = corps;
    }
    
    public int obtenirCodeStatut() {
        return codeStatut;
    }
    
    public String obtenirMessageStatut() {
        return messageStatut;
    }
    
    public String obtenirTypeContenu() {
        return typeContenu;
    }
    
    public byte[] obtenirCorps() {
        return corps;
    }
    
    // Retourne la longueur du contenu
    public int obtenirLongueurContenu() {
        return corps != null ? corps.length : 0;
    }
}