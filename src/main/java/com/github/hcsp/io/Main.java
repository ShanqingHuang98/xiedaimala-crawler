package com.github.hcsp.io;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {

        // 待处理的链接池
        List<String> linkPool = new ArrayList<>();
        //已经处理的链接池
        Set<String> processedLinks = new HashSet<>();
        linkPool.add("https://sina.cn");

        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }

            String link = linkPool.remove(linkPool.size() - 1);

            if (processedLinks.contains(link)) {
                continue;
            }
            // 我们只关心news.sina的，要排除登录页面
            if (isInterestingLink(link)) {
                Document doc = httpGetAdnParseHtml(link);
                //用java8的表达式简化
                doc.select("a").stream().map(aTag->aTag.attr("href")).forEach(linkPool::add);
                //假如这是一个新闻的详情，就存入数据库
                StoreIntoDatabaseIfItIsNewsPage(doc);
                processedLinks.add(link);

            } else {
                //这是我们不感兴趣的
            }
        }
    }

    private static void StoreIntoDatabaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAdnParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        // 怕被发现是爬虫，所以要加这个
        if (link.startsWith("//")) {
            link = "https:" + link;
            System.out.println(link);
        }

        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36");


        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            //这是我们感兴趣的“新闻”，不感兴趣的广告不包含在内
            return Jsoup.parse(html);
        }
    }

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
