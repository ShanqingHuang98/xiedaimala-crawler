package com.github.hcsp.io;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Crawler {

    private CrawlerDao dao = new JdbcCrawlerDao();

    public void run() throws SQLException, IOException {
        String link;

        // 从数据库加载下一个连接，如果能加载到，则进行循环。
        while ((link = dao.getNextLinkThenDelete()) != null) {
            //询问数据库当前连接是否已被处理过
            if (dao.isLinkProcessed(link)) {
                continue;
            }

            if (isInterestingLink(link)) {
                System.out.println(link);
                Document doc = httpGetAdnParseHtml(link);

                parseUrlsFromPageAndStoreIntoDatabase(doc);

                StoreIntoDatabaseIfItIsNewsPage( doc, link);

                dao.updateDatabase( link, "INSERT INTO LINKS_ALREADY_PROCESSED (link) values(?)");
            }
        }
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");

            if (href.startsWith("//")) {
                href = "https:" + href;
            }

            if (!href.toLowerCase().startsWith("javascript")) {
                dao.updateDatabase(href, "INSERT INTO LINKS_TO_BE_PROCESSED (link) values(?)");
            }
        }
    }


    private void StoreIntoDatabaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                dao.insertNewsIntoDatabase( link, title, content);
            }
        }
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    private static Document httpGetAdnParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            //这是我们感兴趣的“新闻”，不感兴趣的广告不包含在内
            return Jsoup.parse(html);
        }
    }

    // 我们只关心news.sina的，要排除登录页面
    private static boolean isInterestingLink(String link) {
        return (isNewPage(link) || isIndexPage(link) && isNotLoginPage(link));
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }


    private static boolean isNewPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }
}
