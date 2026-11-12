package edu.lab.select.web;

import edu.lab.select.service.ApiException;
import edu.lab.select.service.TokenStore;

public abstract class BaseController {

    protected final TokenStore tokens;

    protected BaseController(TokenStore tokens) {
        this.tokens = tokens;
    }

    protected TokenStore.Session session(String token) {
        TokenStore.Session s = tokens.get(token);
        if (s == null) throw new ApiException("登录已失效，请重新登录");
        return s;
    }

    protected Long self(String token, String role) {
        TokenStore.Session s = session(token);
        if (!role.equals(s.role())) throw new ApiException("当前身份无权访问该功能");
        return s.id();
    }
}
