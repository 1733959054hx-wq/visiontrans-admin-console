package com.gzu.adminconsole.jingchen.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantFileVO;
import com.gzu.adminconsole.jingchen.service.MerchantFileService;

/**
 * 商户文件上传 / 取回接口（jingchen 模块）。
 *
 * <p>素材、视频、字幕、入驻资质统一走这里做<b>真实</b> multipart 上传：
 * 服务端校验类型与大小后重命名落盘，返回访问地址；原始文件名由业务表自行保存。
 * 取回接口为内联预览（图片/视频/字幕直接预览，其余按附件下载）。
 *
 * <p><b>取回为公开能力地址</b>（{@code isPublicPath} 放行 {@code /merchant/files/} 段）：
 * 文件名是不可猜测的 32 位 UUID,移动端 / 用户端凭链接即可展示商户发布的图片与视频,
 * 无需携带商户令牌;上传(POST)仍需商户令牌,类级 {@code @RequireRole(MERCHANT)}。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/files")
public class MerchantFileController {

    private final MerchantFileService service;

    public MerchantFileController(MerchantFileService service) {
        this.service = service;
    }

    /** 上传文件（kind: image / video / subtitle / doc）。 */
    @PostMapping
    public Result<MerchantFileVO> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam("kind") String kind) {
        return Result.ok(service.store(file, kind));
    }

    /** 取回文件内容（inline：图片/视频/字幕预览，其余按附件下载）。 */
    @GetMapping("/{name:.+}")
    public ResponseEntity<Resource> download(@PathVariable("name") String name) {
        Resource resource = service.load(name);
        MediaType media = service.mediaTypeOf(name);
        boolean inline = media.getType().equals("image") || media.getType().equals("video")
                || media.getType().equals("text");
        String disposition = ContentDisposition.inline().filename("merchant-file", StandardCharsets.UTF_8).build().toString();
        if (!inline) {
            disposition = ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString();
        }
        return ResponseEntity.ok()
                .contentType(media)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }
}
