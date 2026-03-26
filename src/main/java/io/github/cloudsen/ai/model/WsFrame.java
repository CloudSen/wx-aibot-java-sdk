package io.github.cloudsen.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 通用 WebSocket 帧。
 *
 * @param <T> body 类型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WsFrame<T> {

    private String cmd;
    private WsHeaders headers;
    private T body;
    private Integer errcode;
    private String errmsg;

    public WsFrame() {
    }

    public WsFrame(String cmd, WsHeaders headers, T body) {
        this.cmd = cmd;
        this.headers = headers;
        this.body = body;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public WsHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(WsHeaders headers) {
        this.headers = headers;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public Integer getErrcode() {
        return errcode;
    }

    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public String getReqId() {
        return headers == null ? null : headers.getReqId();
    }

    public <U> WsFrame<U> withBody(U newBody) {
        WsFrame<U> frame = new WsFrame<>();
        frame.setCmd(cmd);
        frame.setHeaders(headers);
        frame.setBody(newBody);
        frame.setErrcode(errcode);
        frame.setErrmsg(errmsg);
        return frame;
    }
}
