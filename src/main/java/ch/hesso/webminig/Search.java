package ch.hesso.webminig;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;

/**
 * Permet de faire une recherche dans solr.
 */
public class Search {
    private SolrClient solrClient = SolrUtil.connectToSolrClient("wemlabo1");

    // Args represent the query
    public static void main(String... args) {
        String query = args.length == 0 ? "*" : String.join(" ", args);
        Search search = new Search();
        search.executeQuery(query);
    }

    private void executeQuery(final String query) {
        try {
           /* final QueryResponse response = solr.query(queryParams);
            final SolrDocumentList documents = response.getResults();*/
            SolrQuery q = new SolrQuery(String.format("(title:%s)", query));
            q.setFields("title", "id");
            q.setRows(20);
            final QueryResponse queryResponse = solrClient.query(q);
            final SolrDocumentList documents = queryResponse.getResults();

            System.out.println("\n===========================================");
            System.out.println("QUERY: '" + query + "'");
            System.out.println("NUMBER OF DOCUMENTS FOUND: " + documents.getNumFound());
            System.out.println("TOTAL DOCUMENTS IN INDEX: " + this.countNbDocumentsInIndex());
            System.out.println("===========================================");
            documents.forEach(d -> printFields(d));
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
    }

    private long countNbDocumentsInIndex() throws IOException, SolrServerException {
        SolrQuery q = new SolrQuery("*:*");
        q.setRows(0);
        final QueryResponse queryResponse = this.solrClient.query(q);
        return queryResponse.getResults().getNumFound();
    }

    private static void printFields(SolrDocument document) {
        document.getFieldNames().forEach(s -> System.out.println(s + ": " + document.get(s)));
        System.out.println("===========================================");
    }

}