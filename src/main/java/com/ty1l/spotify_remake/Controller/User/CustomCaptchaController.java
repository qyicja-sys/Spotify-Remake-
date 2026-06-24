package com.ty1l.spotify_remake.Controller.User;

import com.ty1l.spotify_remake.utility.LocalCaptchaCache;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/captcha")
public class CustomCaptchaController {

    private static final String RANDOM_IMAGE_URL = "https://picsum.photos/310/155";

    /**
     * 阶段一：获取滑块
     */
    @GetMapping("/get")
    public Map<String, Object> getCaptcha() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 发起网络请求，把网上的随机图片读入到 Java 内存中
            URL url = new URL(RANDOM_IMAGE_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 设置超时，防止网络卡死
            connection.setReadTimeout(5000);

            // 开启重定向支持（因为 picsum 靠重定向分发随机图）
            connection.setInstanceFollowRedirects(true);

            // 2. 将网络图片流直接转化为内存中的图片对象
            BufferedImage backgroundImage = ImageIO.read(connection.getInputStream());
            if (backgroundImage == null) {
                throw new RuntimeException("读取网络图片失败");
            }
            String token = UUID.randomUUID().toString().replaceAll("-", "");
            int targetX = (int) (Math.random() * 200) + 50; // 模拟随机缺口位置
            // 固定的 Y 坐标
            int targetY = 40;
            // ========================================================
            // 4. 【核心模拟抠图】：在内存中把背景图抠出一个小滑块
            // 真实 AJ-Captcha 底层会在这里使用 Graphics2D 画出拼图形状（如带凹凸的拼图块）
            // 我们这里简单切一个 45x45 的正方形滑块来演示核心逻辑
            BufferedImage sliderImage = backgroundImage.getSubimage(targetX, targetY, 45, 45);
            // ========================================================

            // 5. 将内存中的两张图片（原图、切片）直接转为 Base64 字符串
            String bgBase64 = convertToBase64(backgroundImage, "png");
            String sliderBase64 = convertToBase64(sliderImage, "png");

            // 6. 【核心标记】：存入本地内存缓存（上一课写的工具类），2分钟有效
            String cacheKey = "captcha:" + token;
            LocalCaptchaCache.put(cacheKey, String.valueOf(targetX), 120);

            System.out.println("【测试福利】当前生成的 Token: " + token + " -> 正确的 userX 应该是: " + targetX);

            result.put("token", token);
            result.put("backgroundImage", "data:image/png;base64," + bgBase64);
            result.put("sliderImage", "data:image/png;base64," + sliderBase64);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("msg", "生成验证码失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 阶段二：第一阶段验证（滑块对齐了）
     */
    @PostMapping("/check")
    public Map<String, Object> checkCaptcha(@RequestBody Map<String, String> requestData) {
        String token = requestData.get("token");
        int userX = Integer.parseInt(requestData.get("userX"));

        Map<String, Object> response = new HashMap<>();
        String cacheKey = "captcha:" + token;

        // 1. 从本地内存中取出标准答案
        String standardXStr = LocalCaptchaCache.get(cacheKey);
        if (standardXStr == null) {
            response.put("success", false);
            response.put("msg", "验证码已过期，请刷新重试");
            return response;
        }

        // 2. 比对误差
        int standardX = Integer.parseInt(standardXStr);
        if (Math.abs(userX - standardX) <= 3) {

            // 3. 【状态升级】：校验通过，将内存中的状态改写为 "SUCCESS"，过期时间缩短到 60 秒
            LocalCaptchaCache.put(cacheKey, "SUCCESS", 60);

            response.put("success", true);
            response.put("captchaVerification", token); // 将 token 作为凭证给前端
            response.put("msg", "验证通过");
        } else {
            response.put("success", false);
            response.put("msg", "验证失败");
        }
        return response;
    }

        /**
         * 核心工具方法：将内存中的 BufferedImage 直接转换为 Base64 字符串（不落盘、不写硬盘）
         *
         * @param image      内存中的图片对象
         * @param formatName 图片格式，如 "png" 或 "jpg"
         * @return 纯 Base64 编码字符串
         * @throws Exception 转换异常
         */
        /**
         * 辅助方法：将内存中的 BufferedImage 转换为 Base64 字符串（不落盘）
         */
        private String convertToBase64(BufferedImage image, String formatName) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // 将图片数据写入内存输出流
            ImageIO.write(image, formatName, baos);
            byte[] bytes = baos.toByteArray();
            // 转换为 Base64
            return Base64.getEncoder().encodeToString(bytes);
        }
}