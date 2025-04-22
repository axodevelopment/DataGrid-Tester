import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;

public class DataGridPusher {

    private static boolean _running = true;

    /*
     * Just using this to push some data into a datagrid via its rest endpoints
     * 
     * This only works through basic auth but can be updated later if need be.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java DataGridPusher <username> <password> <cacheName>");
            System.exit(1);
        }

        var username = args[0];
        var password = args[1];
        var cacheName = args[2];

        var serverURL = "https://localhost:11222/rest/v2/caches/" + cacheName;

        /* As mentioned above currently not supporting TLS */
        disableCertificateValidation();

        //TODO: Add validation
        
        // hand sig notifications
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Received interrupt signal. Exiting...");
            _running = false;
        }));

        System.out.println("🚀 Starting to push and read data every second. Press Ctrl+C to stop.");

        var timer = new Timer();

        //TODO: Since I am just using 1000 period, I probably should consider if the put takes longer then that
        // creating a tick task, 0 delay 1 sec
        timer.scheduleAtFixedRate(new TimerTask() {

            int counter = 0;

            @Override
            public void run() {

                if (!_running) {
                    // sig so out
                    timer.cancel();
                    System.out.println("exiting task...");

                    return;
                }

                try {
                    var key = "key-" + counter;
                    var value = "time-" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    // looks like it is fully rest so just ad /key
                    var url = new URL(serverURL + "/" + key);
                    var conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Authorization", "Basic " + basicAuth(username, password));
                    conn.setRequestProperty("Content-Type", "text/plain");

                    //java ...
                    conn.setDoOutput(true);

                    try (var os = conn.getOutputStream()) {
                        os.write(value.getBytes());
                    }

                    if (conn.getResponseCode() != 204) {
                        System.out.println("⚠️ Unexpected PUT response: " + conn.getResponseCode());
                    } else {
                        System.out.printf("✅ PUT key=%s value=%s\n", key, value);
                    }

                    conn.disconnect();

                    //lets pull it so i can see we actually added the key
                    // I could optimize this a bit i guess by keeping the same conn and and replacing headers
                    // but i don't know the java components well enough for that
                    conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Basic " + basicAuth(username, password));

                    try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        var body = reader.readLine();
                        System.out.printf("🔎 Retrieved key=%s value=%s\n", key, body);
                    }

                    conn.disconnect();

                    counter++;

                } catch (Exception e) {
                    System.err.println("⚠️ Error: " + e.getMessage());
                }
            }
        }, 0, 1000);
    }

    private static String basicAuth(String username, String password) {
        var auth = username + ":" + password;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }

    private static void disableCertificateValidation() throws Exception {
        var trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }
        };

        var sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }
}
