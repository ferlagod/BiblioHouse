package com.bibliohouse.logic;
public class TestURIEncoding {
    public static void main(String[] args) throws Exception {
        java.net.URI uri = new java.net.URI("https://nextcloud05.webo.cloud/remote.php/dav/files/fernando.lago%40oniros.eu/");
        System.out.println("URI toASCIIString: " + uri.toASCIIString());
        System.out.println("URI toString: " + uri.toString());
        java.net.URI uri2 = new java.net.URI("https://nextcloud05.webo.cloud/remote.php/dav/files/fernando.lago@oniros.eu/");
        System.out.println("URI2 toASCIIString: " + uri2.toASCIIString());
        System.out.println("URI2 toString: " + uri2.toString());
        
        System.out.println("URI 1 Path: " + uri.getPath());
        System.out.println("URI 2 Path: " + uri2.getPath());
    }
}
