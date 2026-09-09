package com.gzu.adminconsole.dto.cluster;

import java.util.List;

/**
 * 系统日志视图模型（应用日志 / 错误日志 / 模型推理）。
 */
public record SysLogVO(List<SysLogRow> logs) {

    /** 系统日志行。 */
    public record SysLogRow(Long id,
                            String time,
                            String level,
                            String levelTone,
                            String category,
                            String source,
                            String message) {
    }
}
