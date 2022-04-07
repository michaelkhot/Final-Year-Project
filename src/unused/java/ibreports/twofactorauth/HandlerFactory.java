package gmail.twofactorauth;

public class HandlerFactory {
    public enum Type {
        MANUAL
    }

    public static Handler2FA make(Type type) {
        switch (type) {
            case MANUAL:
            default:
                return new Manual2FA();
        }
    }
}
