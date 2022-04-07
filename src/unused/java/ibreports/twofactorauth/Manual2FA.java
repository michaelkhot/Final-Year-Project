package gmail.twofactorauth;

import java.util.Scanner;

public class Manual2FA implements Handler2FA {

    private Scanner scanner;

    public String getCode() {
        System.out.print("Enter 2FA code received via SMS: ");
        return scanner.nextLine().trim();
    }

    public Manual2FA setScanner(Scanner scanner) {
        this.scanner = scanner;
        return this;
    }
}
