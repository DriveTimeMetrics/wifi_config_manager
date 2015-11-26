package org.hpsaturn.autowifi.models;

/**
 * Created by Antonio Vanegas @hpsaturn on 11/25/15.
 */
public class StatusResponse {

    public static final String MSG_OK = "ok";
    public static final String MSG_FAIL = "bad request";
    public static final int CODE_OK = 101;
    public static final int CODE_FAIL = 403;

    public String msg;

    public int code;

    public static StatusResponse getSuccessResponse() {
        StatusResponse resp = new StatusResponse();
        resp.msg=MSG_OK;
        resp.code=CODE_OK;
        return resp;
    }

    public static StatusResponse getBadResponse() {
        StatusResponse resp = new StatusResponse();
        resp.msg=MSG_FAIL;
        resp.code=CODE_FAIL;
        return resp;
    }
}
