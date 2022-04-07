package gmail.twofactorauth;

import java.util.Scanner;

public interface Handler2FA {
    String getCode();
    Handler2FA setScanner(Scanner scanner);
}
