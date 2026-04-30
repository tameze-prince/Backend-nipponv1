package prod.nipponhubv1.nipponhubv1.Exception;

/**
 * Exception métier centralisée.
 * Évite les try/catch répétitifs dans les services.
 */
public class OtakuException extends RuntimeException {

    private final int statusCode;

    public OtakuException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    // Factories sémantiques
    public static OtakuException notFound(String entity, Long id) {
        return new OtakuException(entity + " introuvable (id=" + id + ")", 404);
    }
    public static OtakuException conflict(String message) {
        return new OtakuException(message, 409);
    }
    public static OtakuException forbidden(String message) {
        return new OtakuException(message, 403);
    }
    public static OtakuException badRequest(String message) {
        return new OtakuException(message, 400);
    }
    public static OtakuException serviceUnavailable(String message) {
        return new OtakuException(message, 503);
    }

    public int getStatusCode() { return statusCode; }
}
