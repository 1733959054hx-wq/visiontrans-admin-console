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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.gzu.adminconsole.common.BusinessException;

/**
 * 点击式图形验证码：动态绘制散落汉字，用户按提示顺序点击，后端按坐标 + 容差校验。
 *
 * <p>挑战在服务端内存中保存 5 分钟，校验一次即失效；图片每次随机生成（位置 / 颜色 / 旋转 / 干扰线）。
 *
 * <p><b>防重叠策略</b>：所有汉字（目标字 + 干扰字）按 {@link #gridSlots(int)} 划分的网格
 * 一格一个落位，格内随机抖动。相比"随机取点 + 最小间距重试"，网格分区从数学上保证任意两字
 * 不重叠，不会出现重试失败后退化成完全随机位置的情况。
 */
@Service
public class CaptchaService {

    /**
     * 候选汉字池：山水意象字 + 500 余个常用汉字。
     *
     * <p>字池大小直接决定"来回就那几个字"的观感 —— 早期只有 30 字，每次随机取
     * 6 个（3 目标 + 3 干扰），很快就会反复见到同一批字。现扩充为常用字集合，
     * 并刻意剔除笔画过多（&gt;16 画）与生僻字，保证 28~34px 字号下清晰可辨、
     * 用户一眼认得出（这一点比单纯堆数量更重要）。
     *
     * <p>均为 GB2312 一级常用字，系统默认中文字体即可渲染，不会出现方框（tofu）。
     */
    private static final String CHAR_POOL =
            // 山水风物（观感最好，保留为骨干）
            "山火水月云风花林海雪峰桥灯船塔书琴棋酒茶泉石竹梅松鹤"
            // 高频常用字
            + "的一是不了人我在有他这为之大来以个中上们到说国和地也子时道出而要于"
            + "就下得可你年生自会那后能对着事其里所去行过家十用发天如然作方成者多"
            + "日都三小军二无同么经法当起与好看学进种将还分此心前面又定见只主没公"
            + "从本动口高现头长儿回位爱门问力明由两先实全开手王新奇书光想文总老少"
            + "路男女边听点语间很走元话再身教数安原员住张照最向真设立选城市场"
            // 自然 / 方位 / 数量
            + "东西南北左右前后里外旁千万亿半单双星云雨雷冰霜露河湖波涛岸岛谷川野"
            + "原森草叶树枝苗禾田土泥沙玉金银铜铁春夏秋冬季节气寒暖凉热早晚午夜晨阴晴"
            // 人物 / 身体 / 动物
            + "头脑脸眼耳牙足腿背行走跑跳坐站睡醒吃喝读写唱笑哭念算父母兄弟姐妹亲"
            + "友师徒军民农工商兵医护猫狗鸡鸭鹅猪牛马羊驴鹿虎狮象猴蛇鸟鱼虫"
            // 颜色 / 性状
            + "红黄蓝绿青白黑紫灰粉大小多少长短高低宽窄厚薄新旧快慢真假好坏美丑圆"
            + "方平直轻重软硬香甜苦辣酸"
            // 器物 / 生活
            + "门窗桌椅床碗筷杯盘刀叉勺锅盆瓶罐箱包袋伞鞋帽衣裤袜被枕钟梳毛巾纸笔"
            + "报画鼓笛铃锁钥匙"
            // 补充高频字
            + "让带给送收买卖找放拿提拉推打拍抱抬角层段块片条双对群些每各另再最更"
            + "才刚已却而或如因但并于首同例比及至组团系统网络数据信息内容服务产品"
            + "用户账户密码登录注册搜索查询添加删除修改保存提交取消确认返回首页菜单"
            + "设置帮助关于联系地址电话";
    /** 验证码汉字个数。 */
    private static final int CHAR_COUNT = 3;
    /** 图片尺寸（比字符实际占位大出足够余量，配合网格分区保证不重叠）。 */
    private static final int WIDTH = 320;
    private static final int HEIGHT = 150;
    /** 点击判定容差（像素）。 */
    private static final int TOLERANCE = 26;
    /** 有效期（毫秒）。 */
    private static final long TTL_MS = 5 * 60 * 1000;
    /** 干扰汉字个数。 */
    private static final int DECOY_COUNT = 3;
    /** 落位网格：目标字与干扰字合计 6 个，正好一格一个。 */
    private static final int GRID_COLS = 3;
    private static final int GRID_ROWS = 2;
    /** 目标字字号区间（CJK 单字宽度约等于字号，用于居中换算与留白计算）。 */
    private static final int FONT_MIN = 28;
    private static final int FONT_MAX = 34;

    /** 用户的一次点击坐标。 */
    public record Point(double x, double y) {
    }

    /** 下发给前端的挑战：验证码 ID + base64 图片 + 提示语。 */
    public record CaptchaChallenge(String id, String image, String hint) {
    }

    private record Challenge(String hint, List<double[]> targets, long expiresAt) {
    }

    private final Map<String, Challenge> store = new ConcurrentHashMap<>();

    /** 挑战驻留上限：超出先清理过期项，仍超限则拒绝下发，防止未登录刷接口打爆堆内存。 */
    private static final int MAX_CHALLENGES = 2000;

    /** 生成新的验证码挑战。 */
    public CaptchaChallenge create() {
        if (store.size() >= MAX_CHALLENGES) {
            long now = System.currentTimeMillis();
            store.values().removeIf(c -> c.expiresAt() < now);
            if (store.size() >= MAX_CHALLENGES) {
                throw new BusinessException("系统繁忙，请稍后再试");
            }
        }
        List<char[]> picked = new ArrayList<>();
        StringBuilder hint = new StringBuilder();
        // 目标字必须互不相同：若提示语出现重复字（如「海 海 林」），用户无法区分该点哪一个
        Set<Character> used = new HashSet<>();
        for (int i = 0; i < CHAR_COUNT; i++) {
            char ch = pickDistinct(used);
            used.add(ch);
            picked.add(new char[] {ch});
            hint.append(ch).append(' ');
        }

        // 目标字与干扰字各占一个网格格位，从数学上保证任意两字不重叠
        List<double[]> slots = gridSlots(CHAR_COUNT + DECOY_COUNT);
        List<double[]> targets = new ArrayList<>(slots.subList(0, CHAR_COUNT));
        List<double[]> decoys = new ArrayList<>(slots.subList(CHAR_COUNT, slots.size()));

        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, new Challenge(hint.toString().trim(), targets, System.currentTimeMillis() + TTL_MS));
        // 顺手清理过期挑战，防止长期运行内存缓慢增长
        store.values().removeIf(c -> c.expiresAt() < System.currentTimeMillis());

        // 干扰字需排除全部目标字，避免图中出现与目标同形的字造成歧义
        return new CaptchaChallenge(id, "data:image/png;base64," + draw(picked, targets, used, decoys),
                hint.toString().trim());
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

    /**
     * 网格分区落位：把画布切成 {@code GRID_COLS × GRID_ROWS} 个格子并打乱顺序，
     * 每个字符独占一格、在格内随机抖动 —— 从数学上保证任意两字不重叠。
     *
     * <p>旧实现是"随机取点 + 最小间距（72px）重试"，在 280×110 的画布上 3 个目标字
     * 常因可用区域不足而重试失败、退化成完全随机位置（表现为重叠）；干扰字更是完全随机
     * 放置、不做任何避让，会直接压在目标字上。
     */
    private List<double[]> gridSlots(int count) {
        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                cells.add(new int[] {c, r});
            }
        }
        Collections.shuffle(cells);

        double cellW = (double) WIDTH / GRID_COLS;
        double cellH = (double) HEIGHT / GRID_ROWS;
        // 抖动幅度 = 格子尺寸 - 字号 - 边距，保证字形（含旋转与投影）完整落在格内
        double jitterX = Math.max(0, cellW - FONT_MAX - 8);
        double jitterY = Math.max(0, cellH - FONT_MAX - 6);

        List<double[]> slots = new ArrayList<>();
        for (int i = 0; i < Math.min(count, cells.size()); i++) {
            int[] cell = cells.get(i);
            double cx = (cell[0] + 0.5) * cellW + (Math.random() - 0.5) * jitterX;
            double cy = (cell[1] + 0.5) * cellH + (Math.random() - 0.5) * jitterY;
            slots.add(new double[] {cx, cy});
        }
        return slots;
    }

    /**
     * 动态绘制验证码图片：渐变底、贝塞尔干扰线、噪点、干扰字与高辨识度目标字。
     *
     * @param positions      目标字中心坐标（同时用于点击校验）
     * @param decoyPositions 干扰字中心坐标（由网格分配，不会压住目标字）
     */
    private String draw(List<char[]> chars, List<double[]> positions, Set<Character> exclude,
                        List<double[]> decoyPositions) {
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
        for (int i = 0; i < 200; i++) {
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

        // 干扰元素 3：淡色干扰汉字（按网格格位绘制，不参与校验，不会压住目标字）
        Set<Character> taken = new HashSet<>(exclude);
        for (double[] p : decoyPositions) {
            // 干扰字不得与目标字 / 其它干扰字重复：同一个字若在图中出现两次，点击位置将产生歧义
            char decoy = pickDistinct(taken);
            taken.add(decoy);
            int size = FONT_MIN - 6 + (int) (Math.random() * 5);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
            g.setColor(new Color(0xC7, 0xD2, 0xE3));
            double rotate = (Math.random() - 0.5) * 0.8;
            g.rotate(rotate, p[0], p[1]);
            drawCentered(g, String.valueOf(decoy), p[0], p[1], size);
            g.rotate(-rotate, p[0], p[1]);
        }

        // 目标汉字：多彩色板 + 投影描边，随机旋转，辨识度高且抗 OCR
        Color[] textColors = {new Color(0x1E, 0x3A, 0x8A), new Color(0x43, 0x38, 0xCA),
                new Color(0x0B, 0x1E, 0x4D), new Color(0x0E, 0x74, 0x90), new Color(0xBE, 0x12, 0x3C)};
        for (int i = 0; i < chars.size(); i++) {
            double[] p = positions.get(i);
            Color color = textColors[(int) (Math.random() * textColors.length)];
            int size = FONT_MIN + (int) (Math.random() * (FONT_MAX - FONT_MIN + 1));
            double rotate = (Math.random() - 0.5) * 0.5;
            g.rotate(rotate, p[0], p[1]);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
            // 投影
            g.setColor(new Color(0x94, 0xA3, 0xB8));
            drawCentered(g, new String(chars.get(i)), p[0] + 2, p[1] + 2, size);
            // 主字
            g.setColor(color);
            drawCentered(g, new String(chars.get(i)), p[0], p[1], size);
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

    /**
     * 以 (cx, cy) 为字形<b>中心</b>绘制单个汉字。
     *
     * <p>{@code drawString} 的锚点是基线左端：CJK 单字宽度约等于字号，基线约在字形
     * 垂直中心下方 0.35 倍字号处。按此换算才能真正居中 —— 旧代码用固定的
     * {@code -14 / +10} 偏移，字号变化时会偏心，是重叠与点击偏移的诱因之一。
     */
    private static void drawCentered(Graphics2D g, String s, double cx, double cy, int size) {
        g.drawString(s, (float) (cx - size * 0.5), (float) (cy + size * 0.35));
    }
}
