package com.gzu.adminconsole.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gzu.adminconsole.common.Result;
import com.gzu.adminconsole.dto.cluster.ClusterOverviewVO;
import com.gzu.adminconsole.dto.meta.ActionResultVO;
import com.gzu.adminconsole.model.AlarmEvent;
import com.gzu.adminconsole.model.ClusterNode;
import com.gzu.adminconsole.service.ClusterService;

/**
 * 集群态势感知接口（View 层）。
 */
@RestController
@ConditionalOnProperty(prefix = "admin-console.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${admin-console.api.base-path:/api}/cluster")
public class ClusterController {

    private final ClusterService service;

    public ClusterController(ClusterService service) {
        this.service = service;
    }

    /** 集群态势感知与推演监控大盘（可按告警时间范围过滤，yyyy-MM-dd）。 */
    @GetMapping("/overview")
    public Result<ClusterOverviewVO> overview(@RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return Result.ok(service.overview(start, end));
    }

    /** 容量推演。 */
    @PostMapping("/capacity-simulation")
    public Result<ActionResultVO> simulate() {
        return Result.ok(service.simulateCapacity());
    }

    /* ------------------------------ 容器节点 CRUD ------------------------------ */

    /** 新增容器节点。 */
    @PostMapping("/nodes")
    public Result<ActionResultVO> createNode(@RequestBody ClusterNode node) {
        return Result.ok(service.createNode(node));
    }

    /** 修改容器节点。 */
    @PutMapping("/nodes")
    public Result<ActionResultVO> updateNode(@RequestBody ClusterNode node) {
        return Result.ok(service.updateNode(node));
    }

    /** 删除容器节点。 */
    @DeleteMapping("/nodes/{id}")
    public Result<ActionResultVO> deleteNode(@PathVariable String id) {
        return Result.ok(service.deleteNode(id));
    }

    /* ------------------------------ 告警事件 CRUD ------------------------------ */

    /** 登记一条告警。 */
    @PostMapping("/alarms")
    public Result<ActionResultVO> createAlarm(@RequestBody AlarmEvent alarm) {
        return Result.ok(service.createAlarm(alarm));
    }

    /** 删除告警。 */
    @DeleteMapping("/alarms/{id}")
    public Result<ActionResultVO> deleteAlarm(@PathVariable Long id) {
        return Result.ok(service.deleteAlarm(id));
    }
}
