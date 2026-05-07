package com.bibliohouse.logic;
public class TestSardineURL {
    public static void main(String[] args) throws Exception {
        System.out.println("No passwords available, but we can check URI parsing.");
        java.net.URI uri = new java.net.URI("https://nextcloud05.webo.cloud/remote.php/dav/files/fernando.lago%40oniros.eu/");
        System.out.println("URI path: " + uri.getPath());
        java.net.URI uri2 = new java.net.URI("https://nextcloud05.webo.cloud/remote.php/dav/files/fernando.lago@oniros.eu/");
        System.out.println("URI2 path: " + uri2.getPath());
    }
}
