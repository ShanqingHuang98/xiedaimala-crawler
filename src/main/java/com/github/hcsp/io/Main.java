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

            if (!link.contains("sina.com")) {
                //这是我们不感兴趣的
                continue;
            } else {
                CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(link);

                try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                    System.out.println(link);
                    System.out.println(response1.getStatusLine());
                    HttpEntity entity1 = response1.getEntity();

                    String html = EntityUtils.toString(entity1);
                    //这是我们感兴趣的“新闻”，不感兴趣的广告不包含在内

                    Document doc = Jsoup.parse(html);

                    ArrayList<Element> links = doc.select("a");

                    for (Element aTag : links) {
                        linkPool.add(aTag.attr("href"));
                    }
                    //假如这是一个新闻的详情，就存入数据库
                    ArrayList<Element> articleTags = doc.select("article");
                    if (!articleTags.isEmpty()) {
                        for (Element articleTag : articleTags) {
                            System.out.println("");
                        }

                    }
                }
            }
        }
    }
}
