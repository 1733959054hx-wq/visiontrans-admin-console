package com.gzu.adminconsole.jingchen.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.config.RequireRole;
import com.gzu.adminconsole.jingchen.common.MerchantConstants;
import com.gzu.adminconsole.jingchen.dto.MerchantSubtitleRequest;
import com.gzu.adminconsole.jingchen.entity.MerchantSubtitleEntity;
import com.gzu.adminconsole.jingchen.service.MerchantSubtitleService;

/**
 * 商户视频字幕接口（jingchen 模块）：按视频逐语种的字幕 CRUD。
 *
 * <p>路由挂在视频之下：{@code /merchant/videos/{videoId}/subtitles}；
 * 新增前先用文件接口上传 SRT / VTT 取得地址，再提交档案。
 * 类级 {@code @RequireRole(MERCHANT)}：仅商户令牌可访问。
 */
@RestController
@RequireRole(MerchantConstants.ROLE_CODE)
@RequestMapping("${admin-console.api.base-path:/api}" + MerchantConstants.API_PREFIX + "/videos/{videoId}/subtitles")
public class MerchantSubtitleController {

    private final MerchantSubtitleService service;

    public MerchantSubtitleController(MerchantSubtitleService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<MerchantSubtitleEntity>> list(@PathVariable Long videoId) {
        return Result.ok(service.list(videoId));
    }

    @PostMapping
    public Result<MerchantSubtitleEntity> create(@PathVariable Long videoId,
                                                 @RequestBody MerchantSubtitleRequest req) {
        return Result.ok(service.create(videoId, req));
    }

    @PutMapping("/{id}")
    public Result<MerchantSubtitleEntity> update(@PathVariable Long videoId, @PathVariable Long id,
                                                 @RequestBody MerchantSubtitleRequest req) {
        return Result.ok(service.update(videoId, id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long videoId, @PathVariable Long id) {
        service.delete(videoId, id);
        return Result.ok(true);
    }
}
