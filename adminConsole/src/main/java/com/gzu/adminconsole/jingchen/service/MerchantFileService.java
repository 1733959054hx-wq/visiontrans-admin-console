package com.gzu.adminconsole.jingchen.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gzu.adminconsole.common.BusinessException;
import com.gzu.adminconsole.jingchen.config.JingchenProperties;
import com.gzu.adminconsole.jingchen.dto.MerchantFileVO;

/**
 * 商户文件存储服务（jingchen 模块）：素材 / 视频 / 字幕 / 入驻资质的真实上传落盘。
 *
 * <p>文件一律重命名为 {@code 32位uuid.扩展名} 存入配置目录（{@code admin-console.jingchen.upload-dir}），
 * 原始文件名仅作为展示元数据由业务表自行保存；取回时按同名规则校验防止路径穿越。
 * 文件访问接口 {@code GET /merchant/files/{storedName}} 需商户令牌，前端以 blob 方式取回预览。
 */
@Service
public class MerchantFileService {

    /** 落盘文件名格式：32 位 hex + 小写扩展名（校验用，杜绝 ../ 穿越与任意路径）。 */
    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f]{32}\\.[a-z0-9]{1,8}$");

    /** 各业务类型的扩展名白名单与单文件上限（MB）。 */
    private static final Map<String, Set<String>> ALLOWED_EXT = Map.of(
            "image", Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp"),
            "video", Set.of("mp4", "mov", "m4v", "webm", "avi", "mkv"),
            "subtitle", Set.of("srt", "vtt"),
            "doc", Set.of("pdf", "jpg", "jpeg", "png"));

    private static final Map<String, Long> KIND_MAX_MB = Map.of(
            "image", 20L, "video", 500L, "subtitle", 5L, "doc", 20L);

    private static final Map<String, String> MEDIA_TYPES = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"), Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"), Map.entry("bmp", "image/bmp"),
            Map.entry("mp4", "video/mp4"), Map.entry("mov", "video/quicktime"),
            Map.entry("m4v", "video/x-m4v"), Map.entry("webm", "video/webm"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("srt", "text/plain"), Map.entry("vtt", "text/vtt"));

    private final JingchenProperties properties;
    private final String apiBasePath;

    public MerchantFileService(JingchenProperties properties,
                               @Value("${admin-console.api.base-path:/api}") String apiBasePath) {
        this.properties = properties;
        this.apiBasePath = apiBasePath;
    }

    /** 保存上传文件：校验类型 / 大小 → 落盘 → 返回访问地址与元数据。 */
    public MerchantFileVO store(MultipartFile file, String kind) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        Set<String> exts = ALLOWED_EXT.get(kind);
        if (exts == null) {
            throw new BusinessException(400, "不支持的文件业务类型：" + kind);
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extOf(original);
        if (!exts.contains(ext)) {
            throw new BusinessException(400, "该业务不支持此文件类型（." + ext + "），允许：" + exts);
        }
        long maxBytes = Math.min(KIND_MAX_MB.get(kind), properties.getMaxUploadMb()) * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(400, "文件超过大小限制（" + KIND_MAX_MB.get(kind) + "MB）");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = resolve(storedName);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败：" + e.getMessage());
        }
        long bytes = sizeOf(storedName);
        return new MerchantFileVO(apiBasePath + "/merchant/files/" + storedName,
                Path.of(original).getFileName().toString(), bytes, (bytes + 1023) / 1024, ext);
    }

    /** 按 storedName 取回文件资源；名称不合法或文件不存在视为 404。 */
    public Resource load(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) {
            throw new BusinessException(404, "文件不存在");
        }
        Path path = resolve(storedName);
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(404, "文件不存在");
        }
        return new FileSystemResource(path);
    }

    /** storedName 对应的媒体类型（未知类型按字节流下载）。 */
    public MediaType mediaTypeOf(String storedName) {
        String ext = extOf(storedName);
        String mime = MEDIA_TYPES.get(ext);
        return mime == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(mime);
    }

    /** 读取落盘文件的真实字节数（供业务表回填大小，不信任前端报数）。 */
    public long sizeOf(String storedName) {
        try {
            return Files.size(resolve(storedName));
        } catch (IOException e) {
            return 0L;
        }
    }

    /** 从访问地址截取落盘文件名（url 末段），非上传地址返回 null。 */
    public String storedNameOf(String url) {
        if (url == null || !url.contains("/merchant/files/")) {
            return null;
        }
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private Path resolve(String storedName) {
        return Paths.get(properties.getUploadDir()).resolve(storedName).normalize();
    }

    private static String extOf(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
