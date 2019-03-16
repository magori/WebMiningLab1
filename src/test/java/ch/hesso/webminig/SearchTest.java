package ch.hesso.webminig;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class SearchTest {
    @Test
    void testMain_notEmpt() throws IOException, SolrServerException {
        Search.main("my textfield in my JFrame java");
    }

    @Test
    void testMain_empty() throws IOException, SolrServerException {
        Search.main();
    }
}