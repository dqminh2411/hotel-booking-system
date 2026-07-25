<#import "template.ftl" as layout>
<@layout.registrationLayout
  eyebrow="Đăng ký thành công"
  title="Kiểm tra hộp thư của bạn"
  description="Tài khoản đã được tạo và đang chờ xác thực."
  bodyClass="verify-email">

  <div class="hh-success-box">
    <span class="hh-success-icon">✓</span>
    <p>
      Chúng tôi đã gửi liên kết xác thực tới <strong>${(user.email)!''}</strong>.
      Hãy mở email và nhấn vào liên kết để kích hoạt tài khoản.
    </p>
  </div>

  <#if !isAppInitiatedAction??>
    <form id="kc-verify-email-form" class="hh-form-inline" action="${url.loginAction}" method="post">
      <p class="hh-footer-text">
        Chưa nhận được email?
        <button class="hh-link-strong hh-link-button" type="submit" name="resend" value="true">Gửi lại</button>
      </p>
    </form>
  </#if>
</@layout.registrationLayout>
