package ch.hesso.webminig;

import org.apache.solr.client.solrj.impl.HttpSolrClient;

public class SolrUtil {
    public static HttpSolrClient connectToSolrClient() {
        return connectToSolrClient(null);
    }

    public static HttpSolrClient connectToSolrClient(final String core) {
        String url = "http://localhost:8983/solr";

        try {
            if (core != null) {
                url = url + "/" + core;
            }
            return new HttpSolrClient.Builder(url)
                    .withConnectionTimeout(10000)
                    .withSocketTimeout(60000)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
