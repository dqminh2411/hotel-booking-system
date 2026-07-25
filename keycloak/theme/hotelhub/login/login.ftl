<#import "template.ftl" as layout>
<@layout.registrationLayout
  eyebrow="Chào mừng trở lại"
  title="Đăng nhập HotelHub"
  description="Tiếp tục để quản lý đặt phòng và khám phá ưu đãi dành cho bạn."
  bodyClass="login">

  <#if realm.password>
    <form id="kc-form-login" class="hh-form" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" novalidate>
      <div class="hh-field">
        <label for="username" class="hh-label">
          <#if !realm.loginWithEmailAllowed>Tên đăng nhập
          <#elseif !realm.registrationEmailAsUsername>Email hoặc tên đăng nhập
          <#else>Địa chỉ email</#if>
        </label>
        <input
          tabindex="1"
          id="username"
          name="username"
          type="text"
          class="hh-input <#if messagesPerField.existsError('username','password')>hh-input-error</#if>"
          autofocus
          autocomplete="username"
          value="${(login.username)!''}"
          aria-invalid="<#if messagesPerField.existsError('username','password')>true<#else>false</#if>"
        />
      </div>

      <div class="hh-field">
        <label for="password" class="hh-label">Mật khẩu</label>
        <div class="hh-password-wrap">
          <input
            tabindex="2"
            id="password"
            name="password"
            type="password"
            class="hh-input hh-input-password <#if messagesPerField.existsError('username','password')>hh-input-error</#if>"
            autocomplete="current-password"
            aria-invalid="<#if messagesPerField.existsError('username','password')>true<#else>false</#if>"
          />
          <button type="button" class="hh-password-toggle" data-toggle-password="password">Hiện</button>
        </div>
      </div>

      <#if messagesPerField.existsError('username','password')>
        <p class="hh-error-text" id="input-error">
          ${kcSanitize(messagesPerField.get('username'))?no_esc}
        </p>
      </#if>

      <div class="hh-row-between">
        <#if realm.rememberMe && !usernameEditDisabled??>
          <label class="hh-checkbox-label">
            <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" <#if login.rememberMe??>checked</#if> />
            Ghi nhớ đăng nhập
          </label>
        <#else>
          <span></span>
        </#if>
        <#if realm.resetPasswordAllowed>
          <a tabindex="5" href="${url.loginResetCredentialsUrl}" class="hh-link">Quên mật khẩu?</a>
        </#if>
      </div>

      <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if> />
      <button tabindex="4" class="hh-primary-btn hh-btn-block" name="login" id="kc-login" type="submit">Đăng nhập</button>
    </form>
  </#if>

  <#if realm.password && social.providers?? && social.providers?has_content>
    <div class="hh-divider"><span>hoặc</span></div>
    <div class="hh-social-list">
      <#list social.providers as p>
        <a id="social-${p.alias}" class="hh-social-btn" href="${p.loginUrl}">
          <#if p.alias == 'google'>
            <svg class="hh-google-icon" viewBox="0 0 48 48" aria-hidden="true">
              <path fill="#FFC107" d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"/>
              <path fill="#FF3D00" d="M6.306 14.691l6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"/>
              <path fill="#4CAF50" d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238C29.211 35.091 26.715 36 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"/>
              <path fill="#1976D2" d="M43.611 20.083H42V20H24v8h11.303a12.04 12.04 0 01-4.087 5.571l.003-.002 6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"/>
            </svg>
          </#if>
          Tiếp tục với ${p.displayName!'Google'}
        </a>
      </#list>
    </div>
  </#if>

  <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
    <p class="hh-footer-text">
      Chưa có tài khoản?
      <a href="${url.registrationUrl}" class="hh-link-strong">Đăng ký miễn phí</a>
    </p>
  </#if>
</@layout.registrationLayout>
