<#import "template.ftl" as layout>
<@layout.registrationLayout
  eyebrow="Tạo tài khoản"
  title="Bắt đầu cùng HotelHub"
  description="Đăng ký để lưu hành trình và quản lý các đặt phòng của bạn."
  bodyClass="register">

  <form id="kc-register-form" class="hh-form" action="${url.registrationAction}" method="post" novalidate>
    <div class="hh-field">
      <label for="fullName" class="hh-label">Họ và tên</label>
      <input
        id="fullName"
        name="fullName"
        type="text"
        class="hh-input <#if messagesPerField.existsError('fullName')>hh-input-error</#if>"
        value="${(param.fullName)!''}"
        autocomplete="name"
      />
      <#if messagesPerField.existsError('fullName')>
        <p class="hh-error-text">${kcSanitize(messagesPerField.get('fullName'))?no_esc}</p>
      </#if>
    </div>

    <div class="hh-grid-2">
      <div class="hh-field">
        <label for="email" class="hh-label">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          class="hh-input <#if messagesPerField.existsError('email')>hh-input-error</#if>"
          value="${(param.email)!''}"
          autocomplete="email"
        />
        <#if messagesPerField.existsError('email')>
          <p class="hh-error-text">${kcSanitize(messagesPerField.get('email'))?no_esc}</p>
        </#if>
      </div>

      <div class="hh-field">
        <label for="phone" class="hh-label">Số điện thoại</label>
        <input
          id="phone"
          name="phone"
          type="tel"
          class="hh-input <#if messagesPerField.existsError('phone')>hh-input-error</#if>"
          value="${(param.phone)!''}"
          autocomplete="tel"
        />
        <#if messagesPerField.existsError('phone')>
          <p class="hh-error-text">${kcSanitize(messagesPerField.get('phone'))?no_esc}</p>
        </#if>
      </div>
    </div>

    <div class="hh-field">
      <label for="password" class="hh-label">Mật khẩu</label>
      <div class="hh-password-wrap">
        <input
          id="password"
          name="password"
          type="password"
          class="hh-input hh-input-password <#if messagesPerField.existsError('password')>hh-input-error</#if>"
          autocomplete="new-password"
        />
        <button type="button" class="hh-password-toggle" data-toggle-password="password">Hiện</button>
      </div>
      <#if messagesPerField.existsError('password')>
        <p class="hh-error-text">${kcSanitize(messagesPerField.get('password'))?no_esc}</p>
      </#if>
    </div>

    <div class="hh-field">
      <label for="password-confirm" class="hh-label">Xác nhận mật khẩu</label>
      <div class="hh-password-wrap">
        <input
          id="password-confirm"
          name="password-confirm"
          type="password"
          class="hh-input hh-input-password <#if messagesPerField.existsError('password-confirm')>hh-input-error</#if>"
          autocomplete="new-password"
        />
        <button type="button" class="hh-password-toggle" data-toggle-password="password-confirm">Hiện</button>
      </div>
      <#if messagesPerField.existsError('password-confirm')>
        <p class="hh-error-text">${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}</p>
      </#if>
    </div>

    <p class="hh-hint">Mật khẩu cần từ 8 ký tự, có ít nhất một chữ hoa, một chữ thường và một số.</p>

    <button class="hh-primary-btn hh-btn-block" type="submit">Tạo tài khoản</button>
  </form>

  <p class="hh-footer-text">
    Đã có tài khoản?
    <a href="${url.loginUrl}" class="hh-link-strong">Đăng nhập</a>
  </p>
</@layout.registrationLayout>
