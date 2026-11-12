/* 核心：请求、路由、阶段导轨、页签、提示 */

const App = {
  session: null,
  boot: null,
  accounts: null,
  route: '',
  cache: {},
};

const VIEWS = { student: {}, mentor: {}, admin: {} };

/* ------------------------------------------------------------ 工具 */

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

function esc(s) {
  if (s === null || s === undefined) return '';
  return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function n(v, dash = '—') {
  return v === null || v === undefined ? dash : v;
}

/* 线稿图标：统一 24×24、2px 描边、圆头圆角，跟字体气质一致 */
const svg = d => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${d}</svg>`;

const ICON = {
  arrow: svg('<path d="m9 6 6 6-6 6"/>'),
  arrowLeft: svg('<path d="m15 6-6 6 6 6"/>'),
  check: svg('<path d="m5 12.5 4.5 4.5L19 7"/>'),
  plus: svg('<path d="M12 5v14M5 12h14"/>'),
  close: svg('<path d="M6 6l12 12M18 6 6 18"/>'),
};

/* 侧栏页签图标 */
const NAV_ICON = {
  's/overview': svg('<rect x="3" y="3" width="7.5" height="7.5" rx="2"/><rect x="13.5" y="3" width="7.5" height="7.5" rx="2"/><rect x="3" y="13.5" width="7.5" height="7.5" rx="2"/><rect x="13.5" y="13.5" width="7.5" height="7.5" rx="2"/>'),
  's/labs': svg('<path d="M3 21h18"/><path d="M5 21V8.5L12 4l7 4.5V21"/><path d="M9.5 21v-5h5v5"/>'),
  's/intent': svg('<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M9 13h6M9 17h4"/>'),
  's/result': svg('<circle cx="12" cy="12" r="9"/><path d="m8.2 12.2 2.6 2.6 5-5.4"/>'),
  's/notice': svg('<path d="M3 11v2a1 1 0 0 0 1 1h2.2l5.3 4V6L6.2 10H4a1 1 0 0 0-1 1z"/><path d="M16.5 8.5a5 5 0 0 1 0 7"/><path d="M19.5 6a9 9 0 0 1 0 12"/>'),
  'm/overview': svg('<path d="M6 20v-6"/><path d="M12 20V7"/><path d="M18 20v-9"/>'),
  'm/profile': svg('<circle cx="12" cy="8" r="4"/><path d="M4.5 21a7.5 7.5 0 0 1 15 0"/>'),
  'm/applicants': svg('<circle cx="9.5" cy="8" r="3.6"/><path d="M2.8 20a6.7 6.7 0 0 1 13.4 0"/><path d="M16.5 5.2a3.6 3.6 0 0 1 0 6.6"/><path d="M18.2 14.4A6.7 6.7 0 0 1 22 20"/>'),
  'm/score': svg('<path d="m12 3.5 2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17.5 6.6 20.4l1-6.1L3.2 10l6.1-.9z"/>'),
  'm/decision': svg('<rect x="3" y="3" width="18" height="18" rx="5"/><path d="m7.8 12.2 2.9 2.9 5.5-6.2"/>'),
  'a/dashboard': svg('<rect x="3" y="3" width="18" height="18" rx="4"/><path d="M3 9.5h18"/><path d="M9.5 21V9.5"/>'),
  'a/base': svg('<ellipse cx="12" cy="6" rx="7.5" ry="3"/><path d="M4.5 6v6c0 1.7 3.4 3 7.5 3s7.5-1.3 7.5-3V6"/><path d="M4.5 12v6c0 1.7 3.4 3 7.5 3s7.5-1.3 7.5-3v-6"/>'),
  'a/stages': svg('<circle cx="12" cy="12" r="9"/><path d="M12 7.2V12l3.2 2"/>'),
  'a/adjust': svg('<path d="M4 8h13"/><path d="m14 4.5 3.5 3.5L14 11.5"/><path d="M20 16H7"/><path d="m10 12.5 3.5 3.5L10 19.5"/>'),
  'a/notice': svg('<path d="M3 11v2a1 1 0 0 0 1 1h2.2l5.3 4V6L6.2 10H4a1 1 0 0 0-1 1z"/><path d="M16.5 8.5a5 5 0 0 1 0 7"/><path d="M19.5 6a9 9 0 0 1 0 12"/>'),
};

/* ------------------------------------------------------------ 像素头像与实训室配色 */

const AVA_COLOR = ['#F2C14E', '#5FD68A', '#6FA8F5', '#F07FA8', '#B08CF0', '#4FD1C5', '#F2A25C', '#9AE06A'];

function avSeed(s) {
  let h = 2166136261;
  const t = String(s || '?');
  for (let i = 0; i < t.length; i++) { h ^= t.charCodeAt(i); h = Math.imul(h, 16777619); }
  return h >>> 0;
}

/* 5×5 镜像像素头像：由姓名散列决定，同一个人永远是同一张脸 */
function pixelAvatar(name, px = 20) {
  const h = avSeed(name);
  const bg = AVA_COLOR[(h >>> 5) % AVA_COLOR.length];
  let bits = '';
  for (let y = 0; y < 5; y++) {
    for (let x = 0; x < 3; x++) {
      if (((h >>> ((y * 3 + x) % 28)) & 1) === 0) continue;
      bits += `<rect x="${x}" y="${y}" width="1" height="1"/>`;
      if (x < 2) bits += `<rect x="${4 - x}" y="${y}" width="1" height="1"/>`;
    }
  }
  if (!bits) bits = '<rect x="2" y="1" width="1" height="3"/>';
  return `<svg class="pxa" viewBox="0 0 5 5" width="${px}" height="${px}" shape-rendering="crispEdges" aria-hidden="true"><rect width="5" height="5" fill="${bg}"/><g fill="#2D3748">${bits}</g></svg>`;
}

const LAB_COLOR = {
  '202': '#4FD1C5', '204': '#6FA8F5', '206': '#F2A25C', '402': '#B08CF0',
  '404': '#5FD68A', '406': '#F2C14E', '505': '#F07FA8',
};
/* 浅底上写字用的同色深版，色板与进度条仍用亮版 */
const LAB_INK = {
  '202': '#0D7A6E', '204': '#2A5CB4', '206': '#A85610', '402': '#6A3CA8',
  '404': '#1B7A46', '406': '#8A6208', '505': '#A83258',
};

function labColor(code) { return LAB_COLOR[String(code)] || '#F2C14E'; }
function labInk(code) { return LAB_INK[String(code)] || '#8A6208'; }
function labVars(code) {
  return `--lab:${labInk(code)};--lab-fill:${labColor(code)}`;
}

function fmtDate(iso) {
  if (!iso) return '';
  const s = String(iso);
  return s.slice(5, 10).replace('-', '-');
}

function fmtDateTime(iso) {
  if (!iso) return '';
  const s = String(iso);
  return s.slice(5, 10) + ' ' + s.slice(11, 16);
}

/* 把毫秒差写成「2 天 3 小时」这样的粗粒度 */
function fmtSpan(ms) {
  const m = Math.max(0, Math.round(ms / 60000));
  const d = Math.floor(m / 1440), h = Math.floor((m % 1440) / 60), mm = m % 60;
  if (d) return `${d} 天 ${h} 小时`;
  if (h) return `${h} 小时 ${mm} 分`;
  return `${mm} 分`;
}

function fmtFull(iso) {
  if (!iso) return '';
  const s = String(iso);
  return s.slice(0, 10) + ' ' + s.slice(11, 16);
}

/* 横向滚动条在窄屏会切断内容，用两侧渐隐提示「还能滑」 */
function edgeFade(el) {
  if (!el || el._fadeBound) return;
  el._fadeBound = true;
  const upd = () => {
    const max = el.scrollWidth - el.clientWidth;
    el.classList.toggle('fade-l', el.scrollLeft > 2);
    el.classList.toggle('fade-r', max > 2 && el.scrollLeft < max - 2);
  };
  el.addEventListener('scroll', upd, { passive: true });
  if (window.ResizeObserver) new ResizeObserver(upd).observe(el);
  el._fadeUpd = upd;
  upd();
}

function refreshFades() {
  ['#rail ol', '#nav'].forEach(sel => {
    const el = $(sel);
    edgeFade(el);
    if (el && el._fadeUpd) el._fadeUpd();
  });
}

/* ------------------------------------------------------------ 请求 */

async function api(path, opts = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (App.session) headers['X-Token'] = App.session.token;
  const res = await fetch('/api' + path, {
    method: opts.method || 'GET',
    headers,
    body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
  });
  let data = null;
  try { data = await res.json(); } catch (e) { /* empty */ }
  if (!res.ok) {
    if (res.status === 400 && data && data.message) throw new Error(data.message);
    throw new Error((data && data.message) || '请求失败');
  }
  return data;
}

/* ------------------------------------------------------------ 提示 */

function toast(msg, kind = '') {
  const box = $('#toasts');
  const el = document.createElement('div');
  el.className = 'toast ' + kind;
  el.textContent = msg;
  box.appendChild(el);
  setTimeout(() => {
    el.style.transition = 'opacity .3s ease';
    el.style.opacity = '0';
    setTimeout(() => el.remove(), 320);
  }, 3200);
}

/* ------------------------------------------------------------ 阶段导轨 */

const STAGE_LABEL = { PREACH: '宣讲', OPENDAY: '开放日', INTENT: '填报', INTERVIEW: '面试', CONFIRM: '确认', SUPPLEMENT: '补录', ADJUST: '调剂', PUBLISH: '公示' };

function stageState(stage, current) {
  if (!current) return 'todo';
  if (stage.seq < current.seq) return 'done';
  if (stage.seq === current.seq) return 'active';
  return 'todo';
}

function renderRail() {
  const boot = App.boot;
  if (!boot) return;

  $('#rail').innerHTML = '<ol>' + boot.stages.map(s => {
    const st = stageState(s, boot.current);
    const mark = st === 'done' ? ICON.check : String(s.seq);
    return `<li class="step ${st}" title="${esc(s.summary)}">
      <span class="step-dot">${mark}</span>
      <span class="step-label">${esc(STAGE_LABEL[s.code] || s.name)}</span>
      <span class="step-time">${fmtDate(s.startAt)}</span>
    </li>`;
  }).join('') + '</ol>';

  const pill = $('#stagePill');
  const cur = boot.current;
  pill.innerHTML = cur ? `<i></i>第 ${cur.seq} 阶段 · ${esc(cur.name)}` : '';

  const left = $('#stageLeft');
  if (left) {
    const endMs = cur && cur.endAt ? new Date(cur.endAt) - new Date(boot.demoNow) : 0;
    left.innerHTML = cur && cur.endAt
      ? (endMs > 0 ? `本阶段还剩 <b>${fmtSpan(endMs)}</b>` : '本阶段<b>已截止</b>')
      : '';
  }

  const clockEl = $('#demoClock');
  clockEl.textContent = fmtFull(boot.demoNow);
  const canAdjust = boot.me.role === 'admin';
  clockEl.classList.toggle('is-link', canAdjust);
  clockEl.title = canAdjust ? '演示时钟 · 点击调节' : '演示时钟';
  if (canAdjust && !clockEl._bound) {
    clockEl._bound = true;
    clockEl.addEventListener('click', () => navigate('a/stages'));
  }
  $('#whoName').textContent = boot.me.name;
  $('#whoSub').textContent = boot.me.sub;
  $('#meAvatar').innerHTML = pixelAvatar(boot.me.name, 20);
  refreshFades();

  /* 窄屏阶段条要横向滚动：直接把当前阶段滚到中间，省得用户自己找 */
  const rail = $('#rail ol');
  const act = rail && rail.querySelector('.step.active');
  if (act && rail.scrollWidth > rail.clientWidth + 4) {
    rail.scrollLeft = Math.max(0, act.offsetLeft - (rail.clientWidth - act.offsetWidth) / 2);
  }
}

/* ------------------------------------------------------------ 页签 */

const TABS = {
  student: [
    ['s/overview', '总览'], ['s/labs', '实训室'], ['s/intent', '意向填报'],
    ['s/result', '我的结果'], ['s/notice', '公示名单'],
  ],
  mentor: [
    ['m/overview', '概览'], ['m/profile', '我的介绍'], ['m/applicants', '报名学生'],
    ['m/score', '面试评分'], ['m/decision', '录取决策'],
  ],
  admin: [
    ['a/dashboard', '录取看板'], ['a/base', '基础数据'], ['a/stages', '阶段控制'],
    ['a/adjust', '统筹调剂'], ['a/notice', '公示管理'],
  ],
};

function activeTab() {
  const active = App.route.split('/').slice(0, 2).join('/');
  if (active === 's/lab' || active === 's/mentor') return 's/labs';
  return active;
}

function renderTabs() {
  const role = App.session.role;
  const on = activeTab();
  const items = TABS[role];
  $('#nav').innerHTML = items.map(([key, label]) =>
    `<button class="nav-item ${key === on ? 'on' : ''}" type="button" data-tab="${key}"><span class="ni">${NAV_ICON[key] || ''}</span>${label}</button>`
  ).join('');
  const hit = items.find(([key]) => key === on);
  $('#barTitle').textContent = hit ? hit[1] : items[0][1];
  refreshFades();
}

/* ------------------------------------------------------------ 路由 */

function navigate(hash) {
  if (location.hash === '#' + hash) { render(); return; }
  location.hash = '#' + hash;
}

function parseRoute() {
  const raw = location.hash.replace(/^#\/?/, '') || '';
  return raw;
}

/* 每次 render 领一个序号；慢请求回来时若已有更新的渲染，直接丢弃，避免旧页面盖掉新页面 */
let renderSeq = 0;

async function render() {
  const seq = ++renderSeq;
  const route = parseRoute();
  App.route = route || defaultRoute();
  if (!route) { navigate(App.route); return; }
  renderTabs();

  const [role, key, param] = App.route.split('/');
  const view = VIEWS[App.session.role] && VIEWS[App.session.role][key];
  if (!view) {
    $('#view').innerHTML = `<div class="empty"><b>页面不存在</b>返回上方页签继续。</div>`;
    return;
  }
  $('#view').innerHTML = '<div class="empty">载入中…</div>';
  try {
    const out = await view(param);
    if (seq !== renderSeq) return;
    const stale = $('#view');
    const root = stale.cloneNode(false); // 换一个干净的容器，避免事件监听累积
    stale.replaceWith(root);
    if (typeof out === 'string') {
      root.innerHTML = out;
    } else {
      root.innerHTML = out.html;
    }
    const mount = (out && out.mount) || view.mount;
    if (mount) mount(root);
    root.focus({ preventScroll: true });
    window.scrollTo({ top: 0, behavior: 'instant' });
  } catch (e) {
    if (seq !== renderSeq) return;
    $('#view').innerHTML = `<div class="empty"><b>没能取到数据</b>${esc(e.message)}</div>`;
  }
}

function defaultRoute() {
  const role = App.session ? App.session.role : 'student';
  return { student: 's/overview', mentor: 'm/overview', admin: 'a/dashboard' }[role];
}

/* ------------------------------------------------------------ 页面骨架 */

function pageHead(title, sub, actions = '') {
  return `<div class="page-head">
    <div>
      <h1 class="page-title">${esc(title)}</h1>
      ${sub ? `<p class="page-sub">${esc(sub)}</p>` : ''}
    </div>
    ${actions ? `<div class="page-actions">${actions}</div>` : ''}
  </div>`;
}

function stageBanner() {
  const c = App.boot.current;
  if (!c) return '';
  return `<div class="notice">
    <span class="dot"></span>
    <span><b>当前阶段 · ${esc(c.name)}</b>　${esc(c.summary)}<br>
    <span class="muted small tnum">时间窗 ${fmtFull(c.startAt)} — ${fmtFull(c.endAt)}</span></span>
  </div>`;
}

function meter(done, total, tint) {
  const pct = total <= 0 ? 0 : Math.min(100, Math.round((done / total) * 100));
  const cls = tint || pct < 100 ? '' : 'low';
  const style = tint ? ` style="--lab:${tint}"` : '';
  return `<div class="meter ${cls}"${style}><i style="width:${pct}%"></i></div>`;
}

/* ------------------------------------------------------------ 登录 */

function showLogin(message) {
  const box = $('#login');
  box.hidden = false;
  $('#loginError').hidden = !message;
  $('#loginError').textContent = message || '';
  $('#loginDemo').hidden = true;
  $('#password').value = '';
  setTimeout(() => {
    const u = $('#username');
    (u.value ? $('#password') : u).focus();
  }, 60);
}

function loginError(msg) {
  const el = $('#loginError');
  el.hidden = false;
  el.textContent = msg;
}

async function doLogin(e) {
  if (e) e.preventDefault();
  const username = $('#username').value.trim();
  const password = $('#password').value;
  if (!username || !password) { loginError('请输入账号和密码'); return; }

  const btn = $('#loginBtn');
  btn.disabled = true;
  btn.textContent = '登录中…';
  $('#loginError').hidden = true;
  try {
    const res = await api('/login', { method: 'POST', body: { username, password } });
    App.session = res;
    sessionStorage.setItem('session', JSON.stringify(res));
    await boot();
    $('#login').hidden = true;
    navigate(defaultRoute());
    render();
    if (res.mustChange) openPwd(true);
  } catch (err) {
    loginError(err.message);
    $('#password').value = '';
    $('#password').focus();
  } finally {
    btn.disabled = false;
    btn.textContent = '登录';
  }
}

async function logout() {
  try { await api('/logout', { method: 'POST' }); } catch (err) { /* 忽略 */ }
  App.session = null;
  App.boot = null;
  App.route = '';
  sessionStorage.removeItem('session');
  $('#rail').innerHTML = '';
  $('#nav').innerHTML = '';
  $('#view').innerHTML = '';
  $('#stagePill').innerHTML = '';
  $('#barTitle').textContent = '总览';
  $('#whoName').textContent = '未登录';
  $('#whoSub').textContent = '';
  $('#meAvatar').innerHTML = '·';
  $('#username').value = '';
  showLogin();
}

async function boot() {
  App.boot = await api('/bootstrap');
  renderRail();
}

/* ------------------------------------------------------------ 修改密码 */

function openPwd(forced) {
  $('#pwdModal').hidden = false;
  $('#pwdError').hidden = true;
  $('#pwdOld').value = '';
  $('#pwdNew').value = '';
  $('#pwdNew2').value = '';
  $('#pwdCancel').textContent = forced ? '稍后再说' : '取消';
  setTimeout(() => $('#pwdOld').focus(), 60);
}

function closePwd() {
  $('#pwdModal').hidden = true;
}

function pwdError(msg) {
  const el = $('#pwdError');
  el.hidden = false;
  el.textContent = msg;
}

async function savePwd() {
  const oldPwd = $('#pwdOld').value;
  const newPwd = $('#pwdNew').value;
  const again = $('#pwdNew2').value;
  if (!oldPwd || !newPwd) { pwdError('请填写当前密码与新密码'); return; }
  if (newPwd !== again) { pwdError('两次输入的新密码不一致'); return; }
  const btn = $('#pwdSave');
  btn.disabled = true;
  try {
    await api('/password', { method: 'POST', body: { oldPassword: oldPwd, newPassword: newPwd } });
    closePwd();
    toast('密码已修改，请用新密码重新登录', 'ok');
    await logout();
  } catch (err) {
    pwdError(err.message);
  } finally {
    btn.disabled = false;
  }
}

/* ------------------------------------------------------------ 绑定 */

function bindLogin() {
  $('#loginForm').addEventListener('submit', doLogin);
  $('#demoToggle').addEventListener('click', () => {
    const box = $('#loginDemo');
    box.hidden = !box.hidden;
  });
  $('#switchBtn').addEventListener('click', logout);
  $('#pwdBtn').addEventListener('click', () => openPwd(false));
  $('#pwdClose').addEventListener('click', closePwd);
  $('#pwdCancel').addEventListener('click', closePwd);
  $('#pwdSave').addEventListener('click', savePwd);
  $('#pwdModal').addEventListener('click', e => { if (e.target.id === 'pwdModal') closePwd(); });
  $('#nav').addEventListener('click', e => {
    const t = e.target.closest('[data-tab]');
    if (t) navigate(t.dataset.tab);
  });
  window.addEventListener('hashchange', render);
}
