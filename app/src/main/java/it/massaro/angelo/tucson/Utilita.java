package it.massaro.angelo.tucson;

/**
 * Created by Angelo on 27/09/2016.
 */



        import android.content.Context;
        import android.net.ConnectivityManager;
        import android.net.NetworkInfo;
        import android.text.format.DateFormat;

        import java.io.UnsupportedEncodingException;
        import java.net.URLEncoder;
        import java.text.ParseException;
        import java.text.SimpleDateFormat;

        import java.util.Date;
        import java.util.Locale;
        import java.util.Map;
        import java.util.TimeZone;

/**
 * Created by admwks on 27/09/2016.
 */

public class Utilita {

    private static final char PARAMETER_DELIMITER = '&';
    private static final char PARAMETER_EQUALS_CHAR = '=';
    public static StringBuilder createQueryStringForParameters(Map<String, String> parameters) {
        StringBuilder parametersAsQueryString = new StringBuilder();
        if (parameters != null) {
            boolean firstParameter = true;

            for (String parameterName : parameters.keySet()) {
                if (!firstParameter) {
                    parametersAsQueryString.append(PARAMETER_DELIMITER);
                }

                try {
                    parametersAsQueryString.append(parameterName)
                            .append(PARAMETER_EQUALS_CHAR)
                            .append( URLEncoder.encode(parameters.get(parameterName),"UTF8") );
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                firstParameter = false;
            }
        }
        return parametersAsQueryString;
    }


    public static String getReadableDate(String yyyyMMddHHmmss)
    {
        yyyyMMddHHmmss = yyyyMMddHHmmss.replace("-", "");
        yyyyMMddHHmmss = yyyyMMddHHmmss.replace(" ", "");
        yyyyMMddHHmmss = yyyyMMddHHmmss.replace(":", "");
        yyyyMMddHHmmss = yyyyMMddHHmmss.replace("/", "");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        //String sDate = "";
        String reportDate = "";
        try {
            Date date = formatter.parse(yyyyMMddHHmmss);
            reportDate = sdf.format(date);
            //sDate = DateFormat.format("dd/MM/yyyy HH:mm:ss", date).toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return reportDate;
    }




}
