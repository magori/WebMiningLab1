package ch.hesso.webminig;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public class Crawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg|png|mp3|mp4|zip|gz|txt|pdf|))$");

    private final HttpSolrClient solrClient =
            new HttpSolrClient.Builder("http://localhost:8983/solr")
                    .withConnectionTimeout(10000)
                    .withSocketTimeout(60000)
                    .build();

    @Override
    public boolean shouldVisit(Page page, WebURL url) {
        String href = url.getURL().toLowerCase();
        return !FILTERS.matcher(href).matches()
                && href.startsWith("http://www.commitstrip.com/fr/")
                && page.getStatusCode() == 200;
    }

    @Override
    public void visit(Page page) {
        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String title = Jsoup.parse(htmlParseData.getHtml()).select(".entry-title").text();
            if (!title.isEmpty()) {
                System.out.println(title+" "+page.getWebURL().getURL());

                final SolrInputDocument doc = new SolrInputDocument();
                doc.addField("id", UUID.randomUUID().toString());
                doc.addField("title", title);

                try {
                    final UpdateResponse updateResponse = solrClient.add("wemlabo1", doc);
                    solrClient.commit("wemlabo1");
                } catch (SolrServerException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
