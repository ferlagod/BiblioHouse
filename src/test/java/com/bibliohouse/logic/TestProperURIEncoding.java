package com.bibliohouse.logic;
public class TestProperURIEncoding {
    public static void main(String[] args) throws Exception {
        String username = "fernando.lago@oniros.eu";
        String encoded = new java.net.URI(null, null, username, null).getRawPath();
        System.out.println("Properly encoded username for path: " + encoded);
    }
}
