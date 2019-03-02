package ch.hesso.webminig;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class App {
    public static void main(String[] args) throws Exception {
        final int NUMBER_OF_CRAWELRS = 2;

        CrawlConfig config = initialiseConfig();

        /*
         * Instantiate controller for this crawl.
         */
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        /*
         * Add seed URLs
         */
        controller.addSeed("http://www.commitstrip.com/fr/");

        /*
         * Start the crawl.
         */
        controller.start(Crawler.class, NUMBER_OF_CRAWELRS);
    }

    private static CrawlConfig initialiseConfig() {
        CrawlConfig config = new CrawlConfig();
        config.setMaxConnectionsPerHost(10);
        config.setSocketTimeout(5000);
        config.setCrawlStorageFolder("tmp");
        config.setIncludeHttpsPages(true);
        //minimum 250ms for tests
        config.setPolitenessDelay(250);
        config.setUserAgentString("crawler4j/WEM/2019/");//Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:65.0) Gecko/20100101 Firefox/65.0
        //max 2 -3 levels for tests on large website
        config.setMaxDepthOfCrawling(2);
        // -1 for unlimited number of pages
        config.setMaxPagesToFetch(2000);
        return config;
    }

}
