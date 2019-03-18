package ch.hesso.webminig;

import org.apache.solr.client.solrj.impl.HttpSolrClient;

/**
 * SolR helper class.
 *
 * @author Damien Rochat & Dorian Magnin
 */
public class SolrUtil {

    /**
     * Create a new Solr client with the provided core.
     *
     * @param core
     * @return
     */
    public static HttpSolrClient connectToSolrClient(final String core) {
        String url = "http://localhost:8983/solr";

        if (core != null) {
            url = url + "/" + core;
        }
        return new HttpSolrClient.Builder(url)
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .build();
    }
}
