package io.bdrc.iiif.presentation;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.http.HttpServletRequest;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;

import io.bdrc.auth.AuthProps;

public class GeoLocation {

    private static final String DBLocation = AuthProps.getProperty("geolite_countryDB");
    private static DatabaseReader dbReader;
    public static final String GEO_CACHE_KEY = "GeoDB";
    private static final String CHINA = "China";

    public static String getCountryName(String ip) {
        try {
            dbReader = (DatabaseReader) ServiceCache.getObjectFromCache(GEO_CACHE_KEY);
            if (dbReader == null) {
                File database = new File(DBLocation);
                dbReader = new DatabaseReader.Builder(database).build();
                ServiceCache.put(dbReader, GEO_CACHE_KEY);
            }
            InetAddress ipAddress = InetAddress.getByName(ip);
            CountryResponse response = dbReader.country(ipAddress);
            return response.getCountry().getName();
        } catch (IOException | GeoIp2Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isFromChina(HttpServletRequest request) {
        boolean accessible = false;
        String test = GeoLocation.getCountryName(request.getRemoteAddr());
        if (test == null || CHINA.equalsIgnoreCase(test)) {
            accessible = true;
        }
        return accessible;
    }
}
