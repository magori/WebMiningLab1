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
    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|bpm|jpe?g|png|mp3|mp4|zip|gz|xml|txt|pdf|))$");

    private final HttpSolrClient solrClient = SolrUtil.connectToSolrClient();

    @Override
    public boolean shouldVisit(Page page, WebURL url) {
        checkIsHtml(url);
        return !FILTERS.matcher(url.getURL().toLowerCase()).matches()
                && page.getStatusCode() >= 200 && page.getStatusCode() < 300;
    }

    private void checkIsHtml(final WebURL url) {
        String[] vals = url.getURL().toLowerCase().split("\\.(\\w*)");
        Pattern ext = Pattern.compile("\\.(\\w*)$");
        if (vals.length > 1 && !"html".equals(vals[vals.length - 1])) {
            //System.out.println(url.getURL().toLowerCase());
            if (ext.matcher(url.getURL().toLowerCase()).matches()) {
                //System.out.println(ext.matcher(url.getURL().toLowerCase()).group());
                // System.out.println(vals[vals.length - 1]);
            }
        }
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
