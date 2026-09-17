package com.hellotravel.domain.persistence.model.param;

/**
 * OutboxEvent清理参数，仅用于后台受控物理清理。
 *
 * @param id 可信内部主键
 * @author AIGenerator
 */
public record OutboxEventRemoveParam(Long id) {
}
