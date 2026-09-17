package com.hellotravel.domain.persistence.model.param;

/**
 * Device完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record DeviceWriteParam(
        com.hellotravel.domain.auth.model.aggregate.DeviceAggregate aggregate) {
        }
