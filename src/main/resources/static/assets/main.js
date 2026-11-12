/* 启动 */

(async function init() {
  bindLogin();

  const saved = sessionStorage.getItem('session');
  if (saved) {
    try {
      App.session = JSON.parse(saved);
      await boot();
      $('#login').hidden = true;
      if (!location.hash) navigate(defaultRoute());
      else render();
      return;
    } catch (e) {
      App.session = null;
      sessionStorage.removeItem('session');
    }
  }
  showLogin();
})();
