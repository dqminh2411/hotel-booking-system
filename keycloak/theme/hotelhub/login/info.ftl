<#import "template.ftl" as layout>
<@layout.registrationLayout
  eyebrow="Thông báo"
  title=""
  description=""
  bodyClass="info">

  <div class="hh-success-box">
    <span class="hh-success-icon">✓</span>
    <p>${kcSanitize(message.summary)?no_esc}</p>
  </div>

  <#if pageRedirectUri?has_content>
    <a class="hh-primary-btn hh-btn-block" href="${pageRedirectUri}">Tiếp tục</a>
  <#elseif actionUri?has_content>
    <a class="hh-primary-btn hh-btn-block" href="${actionUri}">Tiếp tục</a>
  <#elseif (client.baseUrl)?has_content>
    <a class="hh-primary-btn hh-btn-block" href="${client.baseUrl}">Quay lại HotelHub</a>
  </#if>
</@layout.registrationLayout>
