document.addEventListener('DOMContentLoaded', function () {
  document.querySelectorAll('[data-toggle-password]').forEach(function (button) {
    var targetId = button.getAttribute('data-toggle-password');
    var input = document.getElementById(targetId);
    if (!input) return;

    button.addEventListener('click', function () {
      var isPassword = input.getAttribute('type') === 'password';
      input.setAttribute('type', isPassword ? 'text' : 'password');
      button.textContent = isPassword ? 'Ẩn' : 'Hiện';
      button.setAttribute('aria-label', isPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
    });
  });
});
