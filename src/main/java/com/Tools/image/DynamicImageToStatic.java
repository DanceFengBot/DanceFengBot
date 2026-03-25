package com.Tools.image;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class DynamicImageToStatic {

    // 下载，转换并返回链接以便绘图时使用
    public static String convert(String imageUrl, String savePath) {
        // 下载图片到临时文件
        File tempFile = new File(savePath);
        try (InputStream in = new URL(imageUrl).openStream()) {
            FileUtils.copyInputStreamToFile(in, tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Download File Failed: " + imageUrl + e);
        }
        if (!tempFile.exists() || tempFile.length() == 0) {
            throw new RuntimeException("Download File is Empty: " + imageUrl);
        }
        String staticImagePath = savePath.replace(".webp", ".png");
        File staticImageFile = new File(staticImagePath);
        // 使用ImageIO进行格式转换
        try {
            BufferedImage image = ImageIO.read(tempFile);
            if (image == null) {
                // 解码失败，可能格式仍不支持或文件损坏
                throw new RuntimeException("Cannot read Image File: " + imageUrl);
            }
            ImageIO.write(image, "png", staticImageFile);
        } catch (IOException e) {
            throw new RuntimeException("Convert Image Failed: " + imageUrl, e);
        }
        // 返回临时文件的绝对路径
        return "file://" + staticImageFile.getAbsolutePath();
    }

    @Test
    public void testConvert() {
        String webpUrl = "https://ts-asset.oss-cn-shanghai.aliyuncs.com/images/resource/dancecube/resource_171002_1727660752402_5014.webp"; // 替换为实际的webp图片URL
        String savePath = "C:/tmp/test_image.webp"; // 替换为实际的保存路径
        try {
            String staticImagePath = convert(webpUrl, savePath);
            System.out.println("Converted Image Path: " + staticImagePath);
            System.out.println(staticImagePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
