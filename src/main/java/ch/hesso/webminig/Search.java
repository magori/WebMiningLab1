package ch.hesso.webminig;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Small application to query StackOverflow questions
 * indexed in Solr core.
 *
 * @author Damien Rochat & Dorian Magnin
 */

public class Search {

    /**
     * Initialize Solr client on target core.
     **/
    private SolrClient solrClient = SolrUtil.connectToSolrClient("wemlabo1");

    /**
     * Run index query app.
     *
     * @param args
     *
     * @throws Exception
     */
    public static void main(String[] args) throws IOException, SolrServerException {
        Search search = new Search();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {

            // Read user input
            System.out.println("====================");
            System.out.print("Enter something (wildcard allowed): ");
            String searchText = br.readLine();

            // Run query
            search.executeQuery(searchText);
        }
    }

    /**
     * Run query and display results.
     *
     * @param searchText
     *
     * @throws IOException
     * @throws SolrServerException
     */
    private void executeQuery(final String searchText) throws IOException, SolrServerException {

        // Run query with more importance on title and tags
        SolrQuery q = new SolrQuery(String.format("(%s:\"%s\")^4 (%s:%s)^3 (%s:'%s')^2 (%s:%s)^1",
                                                  Fields.TITLE, searchText,
                                                  Fields.TITLE, searchText,
                                                  Fields.TAGS, searchText,
                                                  Fields.CONTENT, searchText));

        // Select fields and limit results
        q.setFields(Fields.TITLE, Fields.TAGS, Fields.UPVOTES, Fields.ANSWERED, Fields.URL, Fields.DATE, Fields.SCORE);
        q.setRows(20);

        // Get documents
        final QueryResponse queryResponse = solrClient.query(q);
        final SolrDocumentList documents = queryResponse.getResults();

        // Display results
        System.out.printf("Query: '%s'\n", searchText);
        System.out.printf("Documents found: %d\n", documents.getNumFound());
        System.out.printf("Indexed documents: %d\n", this.countNbDocumentsInIndex());
        if (documents.getNumFound() > 0) {
            System.out.println("Results:");
            documents.forEach(Search::printFields);
        } else {
            System.out.println("No result");
        }
    }

    /**
     * Count the number of documents in the index.
     *
     * @return
     *
     * @throws IOException
     * @throws SolrServerException
     */
    private long countNbDocumentsInIndex() throws IOException, SolrServerException {
        SolrQuery q = new SolrQuery("*:*");
        q.setRows(0);
        final QueryResponse queryResponse = this.solrClient.query(q);
        return queryResponse.getResults().getNumFound();
    }

    /**
     * Print a document details.
     *
     * @param document
     */
    private static void printFields(SolrDocument document) {
        System.out.println("-----");
        document.getFieldNames().forEach(fieldName -> System.out.printf("%s: %s%n", fieldName, document.get(fieldName)));
    }
}
