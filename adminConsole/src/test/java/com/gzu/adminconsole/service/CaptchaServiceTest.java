package com.gzu.adminconsole.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gzu.adminconsole.service.CaptchaService.CaptchaChallenge;
import com.gzu.adminconsole.service.CaptchaService.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 点击式验证码单元测试。
 *
 * <p>覆盖两类容易回归的行为：
 * <ul>
 *   <li>校验语义：必须按顺序命中目标字、超出容差拒绝、挑战一次性。</li>
 *   <li>防重叠：目标字落位两两间距足够 —— 历史版本曾因画布过小、随机取点失败后
 *       退化成完全随机位置而导致汉字重叠，这里用数百次抽样把该行为钉死。</li>
 * </ul>
 */
class CaptchaServiceTest {

    /** 判定为「不重叠」的最小圆心距：字宽约等于字号（最大 34），低于此值字形必然相交。 */
    private static final double MIN_DISTANCE = 39.0;

    private final CaptchaService service = new CaptchaService();

    @Test
    void hintContainsThreeDistinctCharacters() {
        CaptchaChallenge challenge = service.create();

        String[] chars = challenge.hint().split(" ");
        assertEquals(3, chars.length, "提示语应为 3 个目标字");
        Set<String> distinct = new HashSet<>(List.of(chars));
        assertEquals(chars.length, distinct.size(),
                () -> "目标字必须互不相同，否则用户无法区分该点哪一个：" + challenge.hint());
        assertTrue(challenge.image().startsWith("data:image/png;base64,"), "应返回 base64 PNG");
    }

    @Test
    void verifyAcceptsClicksInCorrectOrder() {
        CaptchaChallenge challenge = service.create();
        double[][] targets = targetsOf(challenge.id());

        assertTrue(service.verify(challenge.id(), toPoints(targets)), "按序点击应校验通过");
    }

    @Test
    void verifyRejectsOutOfToleranceClicks() {
        CaptchaChallenge challenge = service.create();
        double[][] targets = targetsOf(challenge.id());

        double[][] shifted = {
                {targets[0][0] + 40, targets[0][1]},
                {targets[1][0], targets[1][1]},
                {targets[2][0], targets[2][1]},
        };
        assertFalse(service.verify(challenge.id(), toPoints(shifted)), "超出容差应拒绝");
    }

    @Test
    void verifyRejectsWrongOrder() {
        CaptchaChallenge challenge = service.create();
        double[][] targets = targetsOf(challenge.id());

        double[][] swapped = {targets[1], targets[0], targets[2]};
        assertFalse(service.verify(challenge.id(), toPoints(swapped)), "点击顺序错误应拒绝");
    }

    /** 挑战必须一次性：校验过（无论成功失败）后不能再用同一个 id 通过。 */
    @Test
    void challengeIsSingleUse() {
        CaptchaChallenge challenge = service.create();
        double[][] targets = targetsOf(challenge.id());

        assertTrue(service.verify(challenge.id(), toPoints(targets)));
        assertFalse(service.verify(challenge.id(), toPoints(targets)), "同一挑战不得重复校验通过");
    }

    @Test
    void verifyRejectsUnknownIdAndWrongClickCount() {
        CaptchaChallenge challenge = service.create();

        assertFalse(service.verify("not-exist", List.of(new Point(0, 0))), "未知挑战 id 应拒绝");
        assertFalse(service.verify(challenge.id(), List.of()), "点击数量不足应拒绝");
        assertFalse(service.verify(challenge.id(), null), "空点击应拒绝");
    }

    /** 大量抽样确保目标字永不重叠（历史上曾因退化随机位置出现重叠）。 */
    @Test
    void targetsNeverOverlap() {
        double worst = Double.MAX_VALUE;
        for (int i = 0; i < 500; i++) {
            CaptchaChallenge challenge = service.create();
            double[][] targets = targetsOf(challenge.id());
            for (int a = 0; a < targets.length; a++) {
                for (int b = a + 1; b < targets.length; b++) {
                    double dx = targets[a][0] - targets[b][0];
                    double dy = targets[a][1] - targets[b][1];
                    worst = Math.min(worst, Math.sqrt(dx * dx + dy * dy));
                }
            }
        }
        final double minDistance = worst;
        assertTrue(minDistance >= MIN_DISTANCE,
                () -> "目标字出现重叠：抽样中的最小圆心距 " + String.format("%.1f", minDistance)
                        + "px 小于安全阈值 " + MIN_DISTANCE + "px");
    }

    private static List<Point> toPoints(double[][] targets) {
        List<Point> points = new ArrayList<>();
        for (double[] target : targets) {
            points.add(new Point(target[0], target[1]));
        }
        return points;
    }

    /**
     * 取出挑战内记录的命中坐标。
     *
     * <p>坐标本就属于内部状态，测试通过反射读取以避免为了可测性向生产代码暴露这些细节。
     */
    @SuppressWarnings("unchecked")
    private double[][] targetsOf(String id) {
        try {
            Field field = CaptchaService.class.getDeclaredField("store");
            field.setAccessible(true);
            Map<String, ?> store = (Map<String, ?>) field.get(service);
            Object challenge = store.get(id);
            if (challenge == null) {
                throw new IllegalStateException("挑战已被消费：" + id);
            }
            Method targets = challenge.getClass().getDeclaredMethod("targets");
            targets.setAccessible(true);
            return ((List<double[]>) targets.invoke(challenge)).toArray(new double[0][]);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("读取验证码命中坐标失败", ex);
        }
    }
}
