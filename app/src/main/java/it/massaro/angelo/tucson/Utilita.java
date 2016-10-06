package it.massaro.angelo.tucson;

/**
 * Created by Angelo on 27/09/2016.
 */



        import android.text.format.DateFormat;

        import java.io.UnsupportedEncodingException;
        import java.net.URLEncoder;
        import java.text.ParseException;
        import java.text.SimpleDateFormat;

        import java.util.Date;
        import java.util.Locale;
        import java.util.Map;

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
        String ret = "";
        String sDate = "";
        try {
            Date date = formatter.parse(yyyyMMddHHmmss);
            sDate = DateFormat.format("dd/MM/yyyy HH:mm:ss", date).toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return sDate;
    }

}
