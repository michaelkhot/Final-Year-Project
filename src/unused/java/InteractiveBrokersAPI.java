import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class InteractiveBrokersAPI {

    private static HttpURLConnection connection;

    public static void main(String[] args) {
        /*
         * BufferedReader reader;
         * String line;
         * StringBuffer responseContent = new StringBuffer();
         * try {
         * URL url = new URL("https://localhost:5000");
         * connection = (HttpURLConnection) url.openConnection();
         *
         * // Request setup
         * connection.setRequestMethod("GET");
         * connection.setConnectTimeout(5000);
         * connection.setReadTimeout(5000);
         *
         * int status = connection.getResponseCode();
         * // System.out.println(status);
         *
         * if (status > 299) {
         * reader = new BufferedReader(new
         * InputStreamReader(connection.getErrorStream()));
         * while ((line = reader.readLine()) != null) {
         * responseContent.append(line);
         * }
         * reader.close();
         * } else {
         * reader = new BufferedReader(new
         * InputStreamReader(connection.getInputStream()));
         * while ((line = reader.readLine()) != null) {
         * responseContent.append(line);
         * }
         * reader.close();
         * }
         * System.out.println(responseContent.toString());
         * } catch (MalformedURLException e) {
         * e.printStackTrace();
         * } catch (IOException e) {
         * e.printStackTrace();
         * } finally {
         * connection.disconnect();
         * }
         */

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://localhost:5000")).build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println)
                .join();
    }
}

/*
 * figure out where to put SSL cert: vertx.jks when making request - if not,
 * just ignore since we connect to localhost only
 */