package com.hellotravel.client.chat.response;

/**
 * 消息应用视图。
 *
 * @param id 公开ID
 * @param seq 稳定序号
 * @param role 来源角色
 * @param status 生成状态
 * @param content 持久正文
 * @param citations 引用JSON
 * @param version 实体版本
 * @author AIGenerator
 */
public record MessageResponse(
        String id,
        long seq,
        String role,
        String status,
        String content,
        String citations,
        long version) {
        }
