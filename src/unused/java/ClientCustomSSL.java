import java.io.File;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

public class ClientCustomSSL {

    public final static void main(String[] args) throws Exception {

        // Creating SSLContextBuilder object
        SSLContextBuilder SSLBuilder = SSLContexts.custom();

        // Loading the Keystore file
        File SSLCert = new File(
                "C:/Users/Michael/Documents/UCL Computer Science/Third Year/COMP0029 Final Year Project/Final-Year-Project/src/main/resources/vertx.jks");
        SSLBuilder = SSLBuilder.loadTrustMaterial(SSLCert,
                "mywebapi".toCharArray());

        // Building the SSLContext usiong the build() method
        SSLContext sslContext = SSLBuilder.build();

        // Creating SSLConnectionSocketFactory object
        SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(sslContext,
                new NoopHostnameVerifier());

        // Creating HttpClientBuilder
        HttpClientBuilder clientbuilder = HttpClients.custom();

        // Setting the SSLConnectionSocketFactory
        clientbuilder = clientbuilder.setSSLSocketFactory(sslConSocFactory);

        // Building the CloseableHttpClient
        CloseableHttpClient httpclient = clientbuilder.build();

        // Creating the HttpGet request
        HttpGet httpget = new HttpGet("https://localhost:5000/v1/api/portfolio/accounts");

        // Executing the request
        HttpResponse httpresponse = httpclient.execute(httpget);

        // printing the status line
        System.out.println(httpresponse.getStatusLine());

        // Retrieving the HttpEntity and displaying the no.of bytes read
        HttpEntity entity = httpresponse.getEntity();
        if (entity != null) {
            // System.out.println(EntityUtils.toByteArray(entity).length);

            String responseString = EntityUtils.toString(entity, "UTF-8");
            System.out.println(responseString);
        }
    }
}