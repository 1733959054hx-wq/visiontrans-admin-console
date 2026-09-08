package com.gzu.adminconsole.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

/**
 * 点击式图形验证码：动态绘制散落汉字，用户按提示顺序点击，后端按坐标 + 容差校验。
 *
 * <p>挑战在服务端内存中保存 5 分钟，校验一次即失效；图片每次随机生成（位置 / 颜色 / 旋转 / 干扰线）。
 */
@Service
public class CaptchaService {

    /** 候选汉字池（选取笔画差异明显、不易混淆的字）。 */
    private static final String CHAR_POOL = "山火水月云风花林海雪峰桥灯船塔书琴棋酒茶泉石竹梅松鹤";
    /** 验证码汉字个数。 */
    private static final int CHAR_COUNT = 3;
    /** 图片尺寸。 */
    private static final int WIDTH = 280;
    private static final int HEIGHT = 110;
    /** 点击判定容差（像素）。 */
    private static final int TOLERANCE = 26;
    /** 有效期（毫秒）。 */
    private static final long TTL_MS = 5 * 60 * 1000;
    /** 目标字符之间的最小间距（像素），防止互相重叠。 */
    private static final double MIN_SPACING = 72;

    /** 用户的一次点击坐标。 */
    public record Point(double x, double y) {
    }

    /** 下发给前端的挑战：验证码 ID + base64 图片 + 提示语。 */
    public record CaptchaChallenge(String id, String image, String hint) {
    }

    private record Challenge(String hint, List<double[]> targets, long expiresAt) {
    }

    private final Map<String, Challenge> store = new ConcurrentHashMap<>();

    /** 生成新的验证码挑战。 */
    public CaptchaChallenge create() {
        List<char[]> picked = new ArrayList<>();
        List<double[]> targets = new ArrayList<>();
        StringBuilder hint = new StringBuilder();
        // 目标字必须互不相同：若提示语出现重复字（如「海 海 林」），用户无法区分该点哪一个
        Set<Character> used = new HashSet<>();
        for (int i = 0; i < CHAR_COUNT; i++) {
            char ch = pickDistinct(used);
            used.add(ch);
            picked.add(new char[] {ch});
            // 保持最小间距的随机落位，避免目标字互相重叠难以辨认
            targets.add(randomSpaced(targets));
            hint.append(ch).append(' ');
        }

        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, new Challenge(hint.toString().trim(), targets, System.currentTimeMillis() + TTL_MS));
        // 顺手清理过期挑战，防止长期运行内存缓慢增长
        store.values().removeIf(c -> c.expiresAt() < System.currentTimeMillis());

        // 干扰字需排除全部目标字，避免图中出现与目标同形的字造成歧义
        return new CaptchaChallenge(id, "data:image/png;base64," + draw(picked, targets, used), hint.toString().trim());
    }

    /**
     * 校验点击是否按顺序命中目标汉字。
     *
     * @return true = 校验通过（挑战随即失效）
     */
    public boolean verify(String id, List<Point> clicks) {
        if (id == null || clicks == null) {
            return false;
        }
        Challenge challenge = store.remove(id);
        if (challenge == null || challenge.expiresAt() < System.currentTimeMillis()) {
            return false;
        }
        if (clicks.size() != challenge.targets().size()) {
            return false;
        }
        for (int i = 0; i < clicks.size(); i++) {
            double[] target = challenge.targets().get(i);
            Point click = clicks.get(i);
            double dx = click.x() - target[0];
            double dy = click.y() - target[1];
            if (dx * dx + dy * dy > (double) TOLERANCE * TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从字池中随机取一个不在 exclude 内的字。
     *
     * <p>字池容量远大于所需字数，guard 仅为兜底，避免极端情况下死循环。
     */
    private char pickDistinct(Set<Character> exclude) {
        char ch = CHAR_POOL.charAt((int) (Math.random() * CHAR_POOL.length()));
        for (int guard = 0; exclude.contains(ch) && guard < 200; guard++) {
            ch = CHAR_POOL.charAt((int) (Math.random() * CHAR_POOL.length()));
        }
        return ch;
    }

    /** 生成与已有字符保持最小间距（72px）的随机位置；重试后仍不满足则放宽返回。 */
    private double[] randomSpaced(List<double[]> existing) {
        for (int attempt = 0; attempt < 60; attempt++) {
            double x = 36 + Math.random() * (WIDTH - 72);
            double y = 36 + Math.random() * (HEIGHT - 72);
            boolean spaced = true;
            for (double[] p : existing) {
                double dx = p[0] - x;
                double dy = p[1] - y;
                if (dx * dx + dy * dy < MIN_SPACING * MIN_SPACING) {
                    spaced = false;
                    break;
                }
            }
            if (spaced) {
                return new double[] {x, y};
            }
        }
        return new double[] {36 + Math.random() * (WIDTH - 72), 36 + Math.random() * (HEIGHT - 72)};
    }

    /** 动态绘制验证码图片：渐变底、贝塞尔干扰线、噪点、干扰字与高辨识度目标字。 */
    private String draw(List<char[]> chars, List<double[]> positions, Set<Character> exclude) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // 对角渐变背景
        g.setPaint(new GradientPaint(0, 0, new Color(0xF8, 0xFA, 0xFF),
                WIDTH, HEIGHT, new Color(0xE4, 0xEE, 0xFE)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // 淡网格
        g.setColor(new Color(0xE2, 0xE8, 0xF0));
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 5; i++) {
            g.drawLine(0, (i + 1) * HEIGHT / 6, WIDTH, (i + 1) * HEIGHT / 6);
        }

        // 干扰元素 1：随机噪点
        for (int i = 0; i < 140; i++) {
            g.setColor(new Color(100 + (int) (Math.random() * 100),
                    120 + (int) (Math.random() * 100), 160 + (int) (Math.random() * 90)));
            int x = (int) (Math.random() * WIDTH);
            int y = (int) (Math.random() * HEIGHT);
            g.fillRect(x, y, 1 + (int) (Math.random() * 2), 1 + (int) (Math.random() * 2));
        }

        // 干扰元素 2：贝塞尔曲线（比椭圆更自然、更难被脚本识别）
        Color[] lineColors = {new Color(0x25, 0x63, 0xEB), new Color(0x0E, 0xA5, 0xE9),
                new Color(0x64, 0x74, 0x8B), new Color(0x38, 0xBD, 0xF8)};
        for (int i = 0; i < 4; i++) {
            g.setColor(lineColors[i % lineColors.length]);
            g.setStroke(new BasicStroke(1.1f + (float) (Math.random() * 0.8),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path = new Path2D.Double();
            path.moveTo(Math.random() * WIDTH * 0.2, Math.random() * HEIGHT);
            path.curveTo(Math.random() * WIDTH, Math.random() * HEIGHT,
                    Math.random() * WIDTH, Math.random() * HEIGHT,
                    WIDTH * (0.8 + Math.random() * 0.2), Math.random() * HEIGHT);
            g.draw(path);
        }

        // 干扰元素 3：淡色干扰汉字（从字符池随机取，不参与校验）
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        for (int i = 0; i < 3; i++) {
            // 干扰字不得与目标字重复：同一个字若在图中出现两次，点击位置将产生歧义
            char decoy = pickDistinct(exclude);
            g.setColor(new Color(0xC7, 0xD2, 0xE3));
            double rotate = (Math.random() - 0.5) * 0.8;
            int x = (int) (Math.random() * (WIDTH - 30));
            int y = 24 + (int) (Math.random() * (HEIGHT - 40));
            g.rotate(rotate, x, y);
            g.drawString(String.valueOf(decoy), x, y);
            g.rotate(-rotate, x, y);
        }

        // 目标汉字：多彩色板 + 投影描边，随机旋转，辨识度高且抗 OCR
        Color[] textColors = {new Color(0x1E, 0x3A, 0x8A), new Color(0x43, 0x38, 0xCA),
                new Color(0x0B, 0x1E, 0x4D), new Color(0x0E, 0x74, 0x90), new Color(0xBE, 0x12, 0x3C)};
        for (int i = 0; i < chars.size(); i++) {
            double[] p = positions.get(i);
            Color color = textColors[(int) (Math.random() * textColors.length)];
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 30 + (int) (Math.random() * 6));
            double rotate = (Math.random() - 0.5) * 0.5;
            g.rotate(rotate, p[0], p[1]);
            g.setFont(font);
            // 投影
            g.setColor(new Color(0x94, 0xA3, 0xB8));
            g.drawString(new String(chars.get(i)), (int) p[0] - 12, (int) p[1] + 12);
            // 主字
            g.setColor(color);
            g.drawString(new String(chars.get(i)), (int) p[0] - 14, (int) p[1] + 10);
            g.rotate(-rotate, p[0], p[1]);
        }
        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }
}
