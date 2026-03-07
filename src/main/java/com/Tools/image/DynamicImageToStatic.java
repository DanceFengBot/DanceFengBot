package com.Tools.image;

import org.apache.commons.io.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class DynamicImageToStatic {

    // 下载，转换并返回链接以便绘图时使用
    public static String convert(String imageUrl, String savePath) throws Exception {
        // 下载图片到临时文件
        File tempFile = new File(savePath);
        try (InputStream in = new URL(imageUrl).openStream()) {
            FileUtils.copyInputStreamToFile(in, tempFile);
        }
        String staticImagePath = savePath.replace(".webp", ".png");
        File staticImageFile = new File(staticImagePath);
        // 使用ImageIO进行格式转换
        javax.imageio.ImageIO.write(javax.imageio.ImageIO.read(tempFile), "png", staticImageFile);
        // 返回临时文件的绝对路径
        return "file://" + staticImageFile.getAbsolutePath();
    }
}
