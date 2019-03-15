package ch.hesso.webminig;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * StackOverflow.com crawler, indexing title, tags, questions
 * and useful information in Apache SolR.
 *
 * @author Damien Rochat & Dorian Magnin
 */
public class Crawler extends WebCrawler {

    /**
     * Define crawl seed URL.
     **/
    private final static String CRAWL_SEED = "https://stackoverflow.com/";

    /**
     * Define crawl base domain to limit crawling.
     **/
    private final static String CRAWL_TARGET = "https://stackoverflow.com/";

    /**
     * Define URL pattern filter for indexing.
     * Allow to index only question pages.
     */
    private final static Pattern INDEXING_FILTER = Pattern.compile("^https://stackoverflow.com/questions/[0-9]*/.*$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Define Solr core URL to use for indexing.
     **/
    private final static String SOLR_CORE = "http://localhost:8983/solr";

    /**
     * Define the number of threads to use during crawling.
     **/
    private final static int NUMBER_OF_CRAWLERS = 2;

    /**
     * Regex allowing to filter file extensions.
     **/
    private final static Pattern EXT_FILTERS = Pattern.compile(".*(\\.(css|js|gif|bpm|jpe?g|png"
            + "|mp3|mp4|zip|gz|xml|txt|pdf|))$", Pattern.CASE_INSENSITIVE);

    /**
     * Initialize Solr client on target core.
     **/
    private final static SolrClient solrClient = new ConcurrentUpdateSolrClient
            .Builder(SOLR_CORE)
            .withConnectionTimeout(10000)
            .withSocketTimeout(60000)
            .build();

    /**
     * Run crawler and indexing.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // Configure the crawler
        CrawlConfig config = new CrawlConfig();
        config.setMaxConnectionsPerHost(10);
        config.setConnectionTimeout(4000);
        config.setSocketTimeout(5000);
        config.setCrawlStorageFolder("data");
        config.setIncludeHttpsPages(true);
        config.setIncludeBinaryContentInCrawling(false);
        config.setMaxDepthOfCrawling(-1);
        config.setPolitenessDelay(250);
        config.setUserAgentString("crawler4j/WEM/2019/");
        config.setMaxPagesToFetch(2000);

        // Instantiate controller for this crawl
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        // Add seed URL
        controller.addSeed(CRAWL_SEED);

        // Clear Solr core
        solrClient.deleteByQuery("*:*");
        solrClient.commit();

        // Start crawler
        controller.start(Crawler.class, NUMBER_OF_CRAWLERS);
    }

    /**
     * Ignore pages with filtered file extension.
     * Also ignores out of domain URLs and response code different than 200 (OK).
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();

        return !EXT_FILTERS.matcher(href).matches()
                && referringPage.getStatusCode() == 200
                && href.startsWith(CRAWL_TARGET);
    }

    @Override
    public void visit(Page page) {

        // Ignore non html content
        if (!(page.getParseData() instanceof HtmlParseData)) {
            return;
        }

        // Do not index other pages than question pages.
        String url = page.getWebURL().toString();
        if (!INDEXING_FILTER.matcher(url).matches()) {
            return;
        }

        // Parse HTML content with jsoup
        HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
        Document doc = Jsoup.parse(htmlParseData.getHtml());

        // Get question title
        String title = doc.select("#question-header a").text();

        // Get question content
        String content = doc.select(".question .post-text").text();

        // Get tags
        List<String> tags = doc.select(".post-taglist .post-tag").eachText();

        // Get answered information
        boolean answered = doc.select(".accepted-answer").first() != null;

        // Get question creation date
        String date = doc.select(".question-stats time[itemprop=dateCreated]").attr("datetime");

        // Debug only
        //System.out.printf("---\nNew document: %s\n", url);
        //System.out.printf("Title: %s\n", title);
        //System.out.printf("Tags: %s\n", String.join(", ", tags));
        //System.out.printf("Answered?: %b\n", answered);
        //System.out.printf("Creation: %s\n", date);

        // Add document into Solr
        SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.setField("id", page.hashCode());
        solrDoc.setField("url", url);
        solrDoc.setField("title", title);
        solrDoc.setField("content", content);
        solrDoc.setField("tags", tags);
        solrDoc.setField("answered", answered);
        solrDoc.setField("date", date);

        try {
            solrClient.add("wemlabo1", solrDoc);
            solrClient.commit("wemlabo1");
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
    }
}
