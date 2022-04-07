package gmail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CredentialManager {

    static final Logger logger = LoggerFactory.getLogger(CredentialManager.class);

    private static final Path CREDENTIALS_FILEPATH = Paths.get("./src/main/resources/ib_credentials.json");

    public static final String USERNAME_KEY = "ib_username";
    public static final String PASSWORD_KEY = "ib_password";

    private JSONObject requestCredentials(Scanner scanner) {
        System.out.print("IB Username: ");
        String username = scanner.nextLine();

        System.out.print("IB Password: ");
        String password = scanner.nextLine();

        JSONObject credentials = new JSONObject();
        credentials.put(USERNAME_KEY, username);
        credentials.put(PASSWORD_KEY, password);
        logger.info("New credentials taken from user input");

        return credentials;
    }

    private boolean doSaveToFile(Scanner scanner) {
        do {
            System.out.print("Save these credentials to file? [y]/n: ");
            String input = scanner.nextLine().trim();
            if (input.equals("") || input.equals("y")) return true;
            if (input.equals("n")) return false;
        } while (true);
    }

    private boolean doLoadFromFile(Scanner scanner) {
        do {
            System.out.print("Use these credentials to authenticate? [y]/n: ");
            String input = scanner.nextLine().trim();
            if (input.equals("") || input.equals("y")) return true;
            if (input.equals("n")) return false;
        } while (true);
    }

    private void saveCredentials(JSONObject credentials) throws IOException {
        Files.write(CREDENTIALS_FILEPATH, credentials.toString(4).getBytes());
        logger.info("Credentials saved to {}", CREDENTIALS_FILEPATH.toAbsolutePath());
    }

    public JSONObject loadCredentials() throws IOException {
        String jsonText = new String(Files.readAllBytes(CREDENTIALS_FILEPATH));
        logger.info("Credentials loaded from {}", CREDENTIALS_FILEPATH.toAbsolutePath());
        return new JSONObject(jsonText);
    }

    public JSONObject getCredentials(Scanner scanner) {
        JSONObject credentials;

        if (Files.exists(CREDENTIALS_FILEPATH)) {
            System.out.println("Found file: " + CREDENTIALS_FILEPATH.getFileName());
            logger.debug("Credentials file exists: {}", CREDENTIALS_FILEPATH.getFileName());
            if (doLoadFromFile(scanner)) {
                try {
                    return loadCredentials();
                } catch (IOException e) {
                    System.out.println("Failed to read credentials from file, enter manually instead");
                    logger.error(e.toString());
                }
            }
        }

        credentials = requestCredentials(scanner);

        if (doSaveToFile(scanner)) {
            try {
                saveCredentials(credentials);
            } catch (IOException e) {
                System.out.println("Failed to save credentials to file");
                logger.error(e.toString());
            }
        }

        return credentials;
    }

}
