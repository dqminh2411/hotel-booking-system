<#import "template.ftl" as layout>
<@layout.registrationLayout
  eyebrow="Hoàn tất hồ sơ"
  title="Cập nhật thông tin tài khoản"
  description="HotelHub cần thêm vài thông tin trước khi bạn tiếp tục."
  bodyClass="update-profile">

  <form id="kc-update-profile-form" class="hh-form" action="${url.loginAction}" method="post" novalidate>
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
    <#if isAppInitiatedAction??>
      <div class="hh-row-between">
        <button class="hh-primary-btn" type="submit">Lưu thông tin</button>
        <button class="hh-secondary-btn" type="submit" name="cancel-aia" value="true">Hủy</button>
      </div>
    <#else>
      <button class="hh-primary-btn hh-btn-block" type="submit">Tiếp tục</button>
    </#if>
  </form>
</@layout.registrationLayout>
