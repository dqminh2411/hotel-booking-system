<#import "template.ftl" as layout>
<@layout.registrationLayout
  eyebrow="Bảo mật tài khoản"
  title="Đặt mật khẩu mới"
  description="Vui lòng đặt mật khẩu mới để tiếp tục sử dụng HotelHub."
  bodyClass="update-password">

  <form id="kc-passwd-update-form" class="hh-form" action="${url.loginAction}" method="post" novalidate>
    <div class="hh-field">
      <label for="password-new" class="hh-label">Mật khẩu mới</label>
      <div class="hh-password-wrap">
        <input
          id="password-new"
          name="password-new"
          type="password"
          class="hh-input hh-input-password <#if messagesPerField.existsError('password-new')>hh-input-error</#if>"
          autofocus
          autocomplete="new-password"
        />
        <button type="button" class="hh-password-toggle" data-toggle-password="password-new">Hiện</button>
      </div>
      <#if messagesPerField.existsError('password-new')>
        <p class="hh-error-text">${kcSanitize(messagesPerField.get('password-new'))?no_esc}</p>
      </#if>
    </div>

    <div class="hh-field">
      <label for="password-confirm" class="hh-label">Xác nhận mật khẩu mới</label>
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

    <#if isAppInitiatedAction??>
      <div class="hh-row-between">
        <button class="hh-primary-btn" type="submit">Cập nhật mật khẩu</button>
        <button class="hh-secondary-btn" type="submit" name="cancel-aia" value="true">Hủy</button>
      </div>
    <#else>
      <button class="hh-primary-btn hh-btn-block" type="submit">Cập nhật mật khẩu</button>
    </#if>
  </form>
</@layout.registrationLayout>
