package com.hellotravel.adaptor.chat.input;

import com.hellotravel.adaptor.chat.input.stream.SyncStreamPublisher;
import com.hellotravel.adaptor.common.HttpIdentity;
import com.hellotravel.adaptor.common.HttpResults;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 提供SSE订阅HTTP入口，连接发布交由专属传输组件管理。
 *
 * @author AIGenerator
 */
@Controller
public final class SyncStreamController {

  private final SyncStreamPublisher publisher;

  /**
   * 注入SSE订阅发布协作。
   *
   * @param publisher 有界订阅发布器
   * @author AIGenerator
   */
  public SyncStreamController(SyncStreamPublisher publisher) {
    this.publisher = publisher;
  }

  /**
   * 建立绑定当前页面会话的SSE订阅。
   *
   * @param after 已处理的持久事件序号
   * @param request HTTP请求及认证上下文
   * @return 已注册的SSE订阅
   * @author AIGenerator
   */
  @GetMapping(value = "/api/v1/events", produces = "text/event-stream")
  @ResponseBody
  public SseEmitter subscribe(@RequestParam long after, HttpServletRequest request) {
    try {
      // 1. 从受信HTTP上下文读取当前账号与页面凭据。
      Long userId = HttpIdentity.user(request);
      String sessionId = request.getHeader("X-Session-ID");
      String accessToken = HttpIdentity.access(request);
      // 2. 注册有界订阅，发布和轮询不在Controller中执行。
      return publisher.subscribe(userId, sessionId, accessToken, after);
    } catch (Exception exception) {
      // 3. 将输入边界异常转换为短生命周期错误事件，避免向协议外抛出异常。
      return failedSubscription(exception);
    }
  }

  private SseEmitter failedSubscription(Exception exception) {
    // 1. 分类当前失败，并使用SSE协议返回可恢复提示。
    var failure = HttpResults.capture(exception);
    // 2. 建立短生命周期SSE响应，避免错误路径留下长期订阅。
    SseEmitter emitter = new SseEmitter(1000L);
    try {
      // 3. 写入错误事件后主动结束连接。
      emitter.send(SseEmitter.event().name("error").data(failure));
    } catch (Exception sendFailure) {
      // 4. 客户端已断开时吞掉传输异常，保持协议边界不再抛出。
    } finally {
      // 5. 无论写入是否成功都结束短生命周期错误订阅。
      emitter.complete();
    }
    // 返回可安全关闭的SSE响应。
    return emitter;
  }
}
