package at.mana.util;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class HashUtil {

    private static final String MD5_ALGORITHM = "MD5";
    private static final char[] hexDigits = "0123456789ABCDEF".toCharArray();

    public static String hash( String value ) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(MD5_ALGORITHM);
            byte[] hashed = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            return asString(hashed);
        } catch ( Exception e ) { return null; }
    }

    public static String hash( String ... value ) {
        StringBuilder builder = new StringBuilder();
        //Arrays.stream(value).forEach( v -> builder.append( hash( v ) ) );
        return Arrays.stream(value).reduce( (a,b) ->  hash( hash(a) + hash(b) ) ).get();
        //return builder.toString();
    }

    private static String asString( final byte[] hashed ) {
        StringBuilder builder = new StringBuilder(2 * hashed.length);
        for (byte b : hashed ) {
            builder.append(hexDigits[b >> 4 & 15]).append(hexDigits[b & 15]);
        }
        return builder.toString();
    }



}
