package com.example.tikiCrawler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import com.opencsv.CSVWriter;

public class TikiCrawlerJava {
    private static final String LAPTOP_PAGE_URL = "https://tiki.vn/api/v2/products?limit=48&include=advertisement&aggregations=1&category=8095&page=%d&urlKey=laptop";
    private static final String PRODUCT_URL = "https://tiki.vn/api/v2/products/%s";
    private static final String PRODUCT_ID_FILE = "C:\\Users\\OS\\eclipse-workspace\\mid-project-614590434_dataCrawlonlineMarket\\crawler\\tikiCrawler\\src\\main\\java\\com\\example\\tikiCrawler\\data\\product-id.txt";
    private static final String PRODUCT_DATA_FILE = "C:\\Users\\OS\\eclipse-workspace\\mid-project-614590434_dataCrawlonlineMarket\\crawler\\tikiCrawler\\src\\main\\java\\com\\example\\tikiCrawler\\data\\product.txt";
    private static final String PRODUCT_FILE = "C:\\Users\\OS\\eclipse-workspace\\mid-project-614590434_dataCrawlonlineMarket\\crawler\\tikiCrawler\\src\\main\\java\\com\\example\\tikiCrawler\\data\\product.csv";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.96 Safari/537.36";

    public static void main(String[] args) {
        try {
            // Crawl product IDs
            List<String> productList = crawlProductId();
            saveProductId(productList);

            // Crawl product details for each ID
            List<String> productDetails = crawlProduct(productList);
            saveRawProduct(productDetails);

            // Adjust JSON and save to CSV
            List<JSONObject> productJsonList = adjustProductDetails(productDetails);
            saveProductListToCSV(productJsonList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> crawlProductId() {
        List<String> productList = new ArrayList<>();
        int page = 1;

        while (true) {
            try {
                System.out.println("Crawl page: " + page);
                URL url = new URL(String.format(LAPTOP_PAGE_URL, page));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) break;

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();

                JSONArray products = new JSONObject(response.toString()).getJSONArray("data");
                if (products.length() == 0) break;

                for (int i = 0; i < products.length(); i++) {
                    JSONObject product = products.getJSONObject(i);
                    String productId = String.valueOf(product.getInt("id"));
                    String productName = product.getString("name"); // Lấy tên sản phẩm
                    String thumbnailUrl = product.has("thumbnail_url") ? product.getString("thumbnail_url") : "No image";
                    Integer productPrice = product.getInt("price");
                    Double ratingAvg = product.getDouble("rating_average");
                    String productUrlPath = product.has("url_path") ? product.getString("url_path") : "No direct path";
                    System.out.println("Product ID: " + productId + ", Name: " + productName + ", Price: " + productPrice + ", Rating: " + ratingAvg + ", Source: tiki.vn/" + productUrlPath); // In ID và tên sản phẩm

                    productList.add(productId + ", " + productName + ", " + thumbnailUrl + ", " + productPrice + ", " + ratingAvg + ", " + "tiki.vn/"+productUrlPath);
                }

                page++;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        return productList;
    }

    private static void saveProductId(List<String> productList) throws IOException {
        Files.write(Paths.get(PRODUCT_ID_FILE), String.join("\n", productList).getBytes());
        System.out.println("Save file: " + PRODUCT_ID_FILE);
    }

//    private static List<String> crawlProduct(List<String> productList) {
//        List<String> productDetailList = new ArrayList<>();
//
//        for (String productId : productList) {
//            try {
//                URL url = new URL(String.format(PRODUCT_URL, productId));
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.setRequestMethod("GET");
//                conn.setRequestProperty("User-Agent", USER_AGENT);
//
//                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                    StringBuilder response = new StringBuilder();
//                    String line;
//                    while ((line = br.readLine()) != null) response.append(line);
//                    br.close();
//
//                    productDetailList.add(response.toString());
//                    System.out.println("Crawl product: " + productId);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        return productDetailList;
//    }

    private static List<String> crawlProduct(List<String> productList) {
        List<String> productDetailList = new ArrayList<>();

        for (String productEntry : productList) {
            String productId = productEntry.split(",")[0]; // Tách ID sản phẩm từ chuỗi productEntry
            try {
                URL url = new URL(String.format(PRODUCT_URL, productId));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);
                    br.close();

                    productDetailList.add(response.toString());
                    System.out.println("Crawl product: " + productId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return productDetailList;
    }

    private static void saveRawProduct(List<String> productDetailList) throws IOException {
        Files.write(Paths.get(PRODUCT_DATA_FILE), String.join("\n", productDetailList).getBytes());
        System.out.println("Save file: " + PRODUCT_DATA_FILE);
    }

//    private static List<JSONObject> adjustProductDetails(List<String> productDetails) {
//        List<JSONObject> adjustedProducts = new ArrayList<>();
//        String[] flattenFields = { "badges", "inventory", "categories", "rating_summary",
//                "brand", "seller_specifications", "current_seller", "other_sellers",
//                "configurable_options", "configurable_products", "specifications",
//                "product_links", "services_and_promotions", "promotions",
//                "stock_item", "installment_info" };
//
//        for (String productDetail : productDetails) {
//            JSONObject productJson = new JSONObject(productDetail);
//
//            if (!productJson.has("id")) continue;
//
//            for (String field : flattenFields) {
//                if (productJson.has(field)) {
//                    productJson.put(field, productJson.getJSONArray(field).toString().replace("\n", ""));
//                }
//            }
//
//            adjustedProducts.add(productJson);
//        }
//
//        return adjustedProducts;
//    }

    private static List<JSONObject> adjustProductDetails(List<String> productDetails) {
        List<JSONObject> adjustedProducts = new ArrayList<>();
        String[] flattenFields = { "badges", "inventory", "categories", "rating_summary",
                "brand", "seller_specifications", "current_seller", "other_sellers",
                "configurable_options", "configurable_products", "specifications",
                "product_links", "services_and_promotions", "promotions",
                "stock_item", "installment_info" };

        for (String productDetail : productDetails) {
            // Kiểm tra nếu productDetail bắt đầu bằng '{'
            if (productDetail.trim().startsWith("{")) {
                JSONObject productJson = new JSONObject(productDetail);

                if (!productJson.has("id")) continue;

                for (String field : flattenFields) {
                    if (productJson.has(field)) {
                        Object fieldValue = productJson.get(field);

                        // Kiểm tra nếu field là JSONArray, nếu đúng thì chuyển thành chuỗi
                        if (fieldValue instanceof JSONArray) {
                            productJson.put(field, ((JSONArray) fieldValue).toString().replace("\n", ""));
                        } else {
                            // Xử lý các kiểu dữ liệu khác nếu cần hoặc chuyển thành chuỗi mặc định
                            productJson.put(field, fieldValue.toString());
                        }
                    }
                }

                adjustedProducts.add(productJson);
            } else {
                System.out.println("Invalid JSON format: " + productDetail);
            }
        }

        return adjustedProducts;
    }


    private static void saveProductListToCSV(List<JSONObject> productJsonList) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(PRODUCT_FILE))) {
            boolean headerWritten = false;

            for (JSONObject product : productJsonList) {
                if (!headerWritten) {
                    writer.writeNext(product.keySet().toArray(new String[0]));
                    headerWritten = true;
                }

                List<String> values = new ArrayList<>();
                product.keys().forEachRemaining(key -> values.add(String.valueOf(product.get(key))));
                writer.writeNext(values.toArray(new String[0]));
            }
        }

        System.out.println("Save file: " + PRODUCT_FILE);
    }
}
