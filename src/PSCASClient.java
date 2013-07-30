//*************************************************************
//* * HR * PSCASClient * HR * **
//*************************************************************
//** (C) 2003, Cal Poly State University San Luis Obispo **
//** based on code (C) 2001, Yale University. **
//** 12/03/2007 G. Weir added Portal Deep Link Code **
//*************************************************************
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PSCASClient {

    private static final String CAS_SERVER_VALIDATE_URL = "https://sso.school.edu/cas/serviceValidate";
    private static final String LOG_FILE_LOCATION       = "\\tmp\\pscas_signon_log.txt";

    private static Pattern      PATTERN_CAS_VALIDATION_RESPONSE_SUCCESS = Pattern.compile("<cas:user>(\\w+)</cas:user>");

    public static String validate(final String fullUrl, final String ticket, final String unused) throws IOException {
        BufferedReader in = null;
        String userId = null;

        try {

            final int ticketParamIndex = fullUrl.lastIndexOf("&ticket=");
            String service = null;

            if (ticketParamIndex != -1) {
                service = URLEncoder.encode(fullUrl.substring(0, ticketParamIndex), "UTF-8");
            } else {
                service = URLEncoder.encode(fullUrl, "UTF-8");
            }

            //***************************************
            //** Check for Portal Deep Links **
            //***************************************
            int beginIndex = service.indexOf("%2520");
            while (beginIndex != -1) {
                service = service.substring(0, beginIndex + 1) + service.substring(beginIndex + 3);
                beginIndex = service.indexOf("%2520");
            }

            final URL u = new URL(CAS_SERVER_VALIDATE_URL + "?ticket=" + ticket + "&service=" + service);
            logMessage("Submitting CAS validation request: " + u.toString());

            in = new BufferedReader(new InputStreamReader(u.openStream()));
            String line = null;

            final StringBuilder builder = new StringBuilder();

            while ((line = in.readLine()) != null) {
                if (line.trim().length() > 0) {
                    builder.append(line);
                    builder.append("\n");
                }
            }

            line = builder.toString();
            logMessage("CAS validation response: " + line);

            final Matcher matcher = PATTERN_CAS_VALIDATION_RESPONSE_SUCCESS.matcher(line);
            if (matcher.find()) {
                userId = matcher.group(1).toUpperCase();
                logMessage("CAS validated userId: " + userId);
            } else if (line.contains("cas:authenticationFailure")) {
                logMessage("CAS failed to validate the authentication request. Please review the CAS validation response and the CAS server log files");
            }
        } catch (final Exception e) {
            e.printStackTrace();
            logException(e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return userId;
    }

    private static void logException(final Exception ex) throws IOException {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
        final Date now = new Date();
        BufferedWriter os = null;
        try {
            os = new BufferedWriter(new FileWriter(LOG_FILE_LOCATION, true));
            os.write(formatter.format(now));
            os.write(ex.getMessage());
            os.write("\n");
            ex.printStackTrace(new PrintWriter(os));
            os.write("\n");

        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    private static void logMessage(final String message) throws IOException {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
        final Date now = new Date();
        BufferedWriter os = null;
        try {
            os = new BufferedWriter(new FileWriter(LOG_FILE_LOCATION, true));
            os.write(formatter.format(now));
            os.write(message);
            os.write("\n");

        } finally {
            if (os != null) {
                os.close();
            }
        }
    }
}
