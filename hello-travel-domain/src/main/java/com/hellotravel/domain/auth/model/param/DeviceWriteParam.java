package com.hellotravel.domain.auth.model.param;

import com.hellotravel.domain.auth.model.aggregate.DeviceAggregate;

/**
 * Device完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record DeviceWriteParam(DeviceAggregate aggregate) {
}
