import javax.xml.parsers.DocumentBuilderFactory;
public class TestEpub {
    public static void main(String[] args) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            System.out.println("Success!");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
