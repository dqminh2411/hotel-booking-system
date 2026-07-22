<#import "template.ftl" as layout>
<@layout.registrationLayout
  eyebrow="Đã có lỗi xảy ra"
  title="Không thể xác thực"
  description=""
  bodyClass="error">

  <div class="hh-alert hh-alert-error" role="alert">
    ${kcSanitize(message.summary)?no_esc}
  </div>

  <#if client?? && (client.baseUrl)?has_content>
    <a class="hh-primary-btn hh-btn-block" href="${client.baseUrl}">Quay lại HotelHub</a>
  </#if>
</@layout.registrationLayout>
