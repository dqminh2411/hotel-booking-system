<#-- HotelHub custom login theme layout.
     Pages we fully own (login.ftl, register.ftl, login-verify-email.ftl,
     info.ftl, error.ftl) import this file and call registrationLayout with
     our own eyebrow/title/description params.

     IMPORTANT: some pages we do NOT override (e.g. login-reset-password.ftl,
     terms.ftl, webauthn/OTP pages) are inherited from the parent "keycloak"
     theme. Those inherited .ftl files still do <#import "template.ftl"> and
     Keycloak's template resolver always resolves that import to OUR
     template.ftl (child theme wins), NOT the parent's. So this macro must
     also accept every parameter name the stock Keycloak base theme passes
     (bodyClass, displayInfo, displayMessage, displayRequiredFields,
     displayWide, showAnotherWayIfPresent) or those inherited pages crash
     with "Macro has no parameter with name ...". We accept and mostly
     ignore the ones we don't use, only actually using displayMessage. -->
<#macro registrationLayout
  eyebrow=""
  title=""
  description=""
  bodyClass=""
  displayInfo=false
  displayMessage=true
  displayRequiredFields=false
  displayWide=false
  showAnotherWayIfPresent=true>
<!DOCTYPE html>
<html lang="${(locale.currentLanguageTag)!'vi'}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="noindex, nofollow">
  <title>HotelHub</title>
  <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css">
</head>
<body class="hh-body hh-page-${bodyClass}">
  <div class="hh-shell">
    <aside class="hh-aside">
      <a class="hh-brand hh-brand-inverse" href="${(client.baseUrl)!'/'}">
        <span class="hh-brand-badge hh-brand-badge-inverse">H</span>
        HotelHub
      </a>
      <div class="hh-aside-content">
        <p class="hh-aside-eyebrow">Du lịch theo cách của bạn</p>
        <h2 class="hh-aside-title">Một tài khoản, mọi hành trình.</h2>
        <p class="hh-aside-desc">
          Khám phá nơi lưu trú phù hợp và quản lý chuyến đi đơn giản hơn cùng HotelHub.
        </p>
        <ul class="hh-benefits">
          <li><span class="hh-benefit-check">✓</span> Lưu thông tin để đặt phòng nhanh hơn</li>
          <li><span class="hh-benefit-check">✓</span> Quản lý mọi chuyến đi tại một nơi</li>
          <li><span class="hh-benefit-check">✓</span> Nhận ưu đãi dành riêng cho thành viên</li>
        </ul>
      </div>
      <p class="hh-aside-footer">© 2026 HotelHub. Travel made simple.</p>
    </aside>

    <main class="hh-main">
      <div class="hh-card-wrap">
        <a class="hh-brand hh-brand-mobile" href="${(client.baseUrl)!'/'}">
          <span class="hh-brand-badge">H</span>
          HotelHub
        </a>

        <section class="hh-card">
          <#-- eyebrow/title/description: used by our own pages. Pages
               inherited from the parent theme won't pass these, so fall
               back to the generic Keycloak page title/message instead. -->
          <#if eyebrow?has_content><p class="hh-eyebrow">${eyebrow}</p></#if>
          <#if title?has_content>
            <h1 class="hh-title">${title}</h1>
          <#elseif !eyebrow?has_content && !description?has_content>
            <h1 class="hh-title">HotelHub</h1>
          </#if>
          <#if description?has_content><p class="hh-description">${description}</p></#if>

          <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
            <div class="hh-alert hh-alert-${message.type}" role="alert">
              ${kcSanitize(message.summary)?no_esc}
            </div>
          </#if>

          <div class="hh-body">
            <#nested>
          </div>
        </section>
      </div>
    </main>
  </div>

  <script src="${url.resourcesPath}/js/app.js"></script>
</body>
</html>
</#macro>
