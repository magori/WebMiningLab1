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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * StackOverflow.com crawler, indexing title, tags, questions
 * and useful information in Apache SolR.
 *
 * @author Damien Rochat & Dorian Magnin
 */
public class Crawler extends WebCrawler {
    private static final List<String> PAGES_VISITED =  Collections.synchronizedList(new ArrayList<>());

    /**
     * Define crawl seed URL.
     **/
    private static final String CRAWL_SEED = "https://stackoverflow.com/";

    /**
     * Define crawl base domain to limit crawling.
     **/
    private static final String CRAWL_TARGET = "https://stackoverflow.com/";

    /**
     * Define URL pattern filter for indexing.
     * Allow to index only question pages.
     */
    private static final Pattern INDEXING_FILTER = Pattern.compile("^https://stackoverflow.com/questions/[0-9]*/.*$", Pattern.CASE_INSENSITIVE);

    /**
     * Define the number of threads to use during crawling.
     **/
    private static final int NUMBER_OF_CRAWLERS = 8;

    /**
     * Regex allowing to filter file extensions.
     **/
    private static final Pattern EXT_FILTERS = Pattern
            .compile(".*(\\.(css|js|gif|bpm|jpe?g|png|mp3|mp4|zip|gz|xml|txt|pdf|))$", Pattern.CASE_INSENSITIVE);

    /**
     * Initialize Solr client on target core.
     **/
    private static final HttpSolrClient solrClient = SolrUtil.connectToSolrClient("wemlabo1");

    /**
     * Run crawler and indexing.
     *
     * @param args
     *
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
        config.setPolitenessDelay(1000);
        config.setUserAgentString("crawler4j/WEM/2019/");
        config.setMaxPagesToFetch(10000);

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
     * Ignores:
     * - Pages with filtered file extension
     * - Out of domain pages
     * - Already visited pages, but with different URL parameters
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();

        return !EXT_FILTERS.matcher(href).matches()
                && href.startsWith(CRAWL_TARGET)
                && !PAGES_VISITED.contains(href.split("\\?")[0]);
    }

    /**
     * Index useful information of StackOverflow questions.
     */
    @Override
    public void visit(Page page) {

        // Ignore non html content
        if (!(page.getParseData() instanceof HtmlParseData)) {
            System.out.println(page.getContentType());
            return;
        }

        // Get URL without parameters
        String url = page.getWebURL().toString().split("\\?")[0];
        PAGES_VISITED.add(url);

        // Do not index other pages than question pages.
        if (!INDEXING_FILTER.matcher(url).matches()) {
            return;
        }

        // Debug only
        System.out.printf("Indexing %s\n", url);

        // Parse HTML content with jsoup
        HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
        Document doc = Jsoup.parse(htmlParseData.getHtml());

        // Get question title
        String title = doc.select("#question-header a").text();

        // Get question content
        String content = doc.select(".question .post-text").text();

        // Get tags
        List<String> tags = doc.select(".post-taglist .post-tag").eachText();

        // Get upvotes
        int upvotes = Integer.parseInt(doc.select(".question div[itemprop=upvoteCount]").text());

        // Get answered information
        boolean answered = doc.select(".accepted-answer").first() != null;

        // Get question creation date
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date date = null;
        try {
            date = dateFormatter.parse(doc.select(".question-stats time[itemprop=dateCreated]").attr("datetime"));
        } catch (ParseException e) {
            // ignoring
        }

        // Debug only
        //System.out.printf("---\nNew document: %s\n", url);
        //System.out.printf("Title: %s\n", title);
        //System.out.printf("Tags: %s\n", String.join(", ", tags));
        //System.out.printf("Upvotes?: %b\n", upvotes);
        //System.out.printf("Answered?: %b\n", answered);
        //System.out.printf("Creation: %s\n", date);

        // Add document into Solr
        SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.setField(Fields.ID, url.hashCode());
        solrDoc.setField(Fields.URL, url);
        solrDoc.setField(Fields.TITLE, title);
        solrDoc.setField(Fields.CONTENT, content);
        solrDoc.setField(Fields.TAGS, tags);
        solrDoc.setField(Fields.UPVOTES, upvotes);
        solrDoc.setField(Fields.ANSWERED, answered);
        solrDoc.setField(Fields.DATE, date);

        try {
            solrClient.add(solrDoc);
            solrClient.commit();
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
    }
}
