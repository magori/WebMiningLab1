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

    public static void main(String... args) throws IOException, SolrServerException {
        String searchText = args.length == 0 ? "*" : String.join(" ", args);
        Search search = new Search();
        search.executeQuery(searchText);
    }

    private void executeQuery(final String searchText) throws IOException, SolrServerException {
        SolrQuery q = new SolrQuery(String.format("(%s:%s)^3 (%s:'%s')^2 (%s:%s)^1", Fields.TITLE, searchText, Fields.CONTENT, searchText, Fields.TAGS,
                                                  searchText));
        q.setFields(Fields.TITLE, Fields.CONTENT, Fields.TAGS, Fields.UPVOTES, Fields.ANSWERED, Fields.URL, Fields.DATE, Fields.ID);
        q.setRows(20);
        final QueryResponse queryResponse = solrClient.query(q);
        final SolrDocumentList documents = queryResponse.getResults();

        System.out.println("\n------------------------------------------");
        System.out.println("SEARCH: '" + searchText + "'");
        System.out.println("NUMBER OF DOCUMENTS FOUND: " + documents.getNumFound());
        System.out.println("TOTAL DOCUMENTS " + this.countNbDocumentsInIndex());
        System.out.println("---------------------------------------------\n");
        documents.forEach(Search::printFields);
    }

    private long countNbDocumentsInIndex() throws IOException, SolrServerException {
        SolrQuery q = new SolrQuery("*:*");
        q.setRows(0);
        final QueryResponse queryResponse = this.solrClient.query(q);
        return queryResponse.getResults().getNumFound();
    }

    private static void printFields(SolrDocument document) {
        document.getFieldNames().forEach(s -> System.out.println(s + ": " + document.get(s)));
        System.out.println("________________________________________________________________________");
    }

}