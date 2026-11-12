/* 学生端 */

/* 名额用「格子」表示，比一条细进度条更容易一眼数清 */
function quotaDots(filled, total) {
  if (total > 10) return meter(filled, total);
  let out = '';
  for (let i = 0; i < total; i++) out += `<i class="${i < filled ? 'on' : ''}"></i>`;
  return `<span class="quota-dots" aria-hidden="true">${out}</span>`;
}

function labRow(l) {
  return `<div class="lab-row" data-lab="${l.id}" style="${labVars(l.code)}">
    <div class="lab-code">${esc(l.code)}</div>
    <div>
      <div class="lab-name">${esc(l.name)}</div>
      <div class="lab-dir">${esc(l.direction)}</div>
    </div>
    <div class="lab-stats">
      <span>导师 <b>${l.mentorCount}</b> 位</span>
      <span>报名 <b>${l.applicants}</b> 人</span>
      <span class="lab-bar">${meter(l.applicants, l.quota, labColor(l.code))}</span>
      <span>剩余 <b>${l.remaining}</b> / ${l.quota}</span>
      ${l.remaining === 0 ? '<span class="tag tag-red">已满</span>' : ''}
    </div>
    <div class="lab-arrow">${ICON.arrow}</div>
  </div>`;
}

/* 实验室详情顶栏：左侧色板写房号，右侧四格数据 */
function labHero(l, mentors) {
  const quota = mentors.reduce((s, m) => s + m.quota, 0);
  const rem = mentors.reduce((s, m) => s + m.remaining, 0);
  const app = mentors.reduce((s, m) => s + (m.applicants || 0), 0);
  const cell = (k, v) => `<div class="lh-cell"><b>${v}</b><span>${k}</span></div>`;
  return `<div class="lab-hero" style="${labVars(l.code)}">
    <div class="lab-hero-plate">${esc(l.code)}</div>
    <div class="lab-hero-body">
      <button class="btn-link" data-back="s/labs" style="margin-bottom:8px">${ICON.arrowLeft} 全部实训室</button>
      <h1 class="page-title">${esc(l.name)}</h1>
      <p class="page-sub">${esc(l.direction)}${l.note ? '　·　' + esc(l.note) : ''}</p>
    </div>
    <div class="lab-hero-stats">
      ${cell('导师', mentors.length)}
      ${cell('名额', quota)}
      ${cell('报名', app)}
      ${cell('剩余', rem)}
    </div>
  </div>`;
}

VIEWS.student.overview = async () => {
  const [home, labsData] = await Promise.all([api('/student/home'), api('/labs')]);
  const labs = labsData.labs.slice().sort((a, b) => b.remaining - a.remaining).slice(0, 4);
  const c = App.boot.current;
  const stageSeq = c ? c.seq : 0;
  const en = home.enrollment;

  const stat = (label, value, hint, accent) => `
    <div class="stat" style="--accent:${accent}">
      <span class="stat-label">${esc(label)}</span>
      <b class="stat-value">${esc(value)}</b>
      <span class="stat-hint">${esc(hint)}</span>
    </div>`;

  return `
  ${pageHead('我的进组进度', `${home.student.name} · ${home.student.no} · ${home.student.className} · 绩点 ${home.student.gpa}`, `<button class="btn btn-primary" data-go="s/intent">去填报意向</button>`)}
  ${stageBanner()}

  <div class="sec">
    <div class="stat-grid">
      ${stat('平行意向', `已填报 ${home.intentionCount} / 3`, home.intentionCount ? '不分先后，可继续修改' : '填报阶段开始后可提交', '#F2C14E')}
      ${stat('拟录取', stageSeq > 4 ? `收到 ${home.offerCount} 个` : '尚未公布', stageSeq > 4 ? '确认阶段由你决定去向' : '面试结束后公布', '#6FA8F5')}
      ${stat('最终去向', en ? `${en.labCode} ${en.labName}` : '尚未确定', en ? `${en.mentorName} · ${en.status === 'FINAL' ? '已公示生效' : '待公示'}` : '确认或调剂后落定', '#5FD68A')}
    </div>
  </div>

  <div class="sec">
    <div class="sec-head">
      <h2 class="sec-title">名额还有余量的实训室</h2>
      <span class="sec-note">按剩余名额排序，点开可看导师与项目方向</span>
    </div>
    <div class="lab-list">${labs.map(labRow).join('')}</div>
  </div>

  <div class="sec">
    <div class="sec-head"><h2 class="sec-title">怎么走完这八步</h2></div>
    <p class="lede">宣讲与开放日用来判断方向，填报只写意向、不排先后——你选的 3 位导师看到的是同一份材料，谁先谁后不会被知道。面试通过后可能同时收到多个拟录取，这时才由你决定去哪一间；只有一个时系统替你确认。三轮补录之后仍未落定，才进入全院调剂。</p>
  </div>`;
};

VIEWS.student.overview.mount = root => {
  root.addEventListener('click', e => {
    const go = e.target.closest('[data-go]');
    if (go) { navigate(go.dataset.go); return; }
    const row = e.target.closest('[data-lab]');
    if (row) navigate('s/lab/' + row.dataset.lab);
  });
};

VIEWS.student.labs = async () => {
  const { labs } = await api('/labs');
  return `
  ${pageHead('实训室', '七间实训室，方向与名额都写在这里。点进去看导师，再点导师看他自己写的那段话。')}
  ${stageBanner()}
  <div class="sec"><div class="lab-list">${labs.map(labRow).join('')}</div></div>`;
};

VIEWS.student.labs.mount = root => {
  root.addEventListener('click', e => {
    const row = e.target.closest('[data-lab]');
    if (row) navigate('s/lab/' + row.dataset.lab);
  });
};

VIEWS.student.lab = async id => {
  const data = await api('/labs/' + id);
  const l = data.lab;
  return `
  ${labHero(l, data.mentors)}
  <div class="sec">
    <div class="sec-head">
      <h2 class="sec-title">本室导师</h2>
      <span class="sec-note">点开看导师自己写的那段话，再决定要不要填</span>
    </div>
    <div class="mentor-list">
      ${data.mentors.map(m => `
        <div class="mentor-row" data-mentor="${m.id}">
          <div class="mentor-main">
            ${pixelAvatar(m.name, 20)}
            <div class="mentor-text">
              <div class="mentor-name">${esc(m.name)}<span class="mentor-title">${esc(m.title)} · ${esc(m.tags)}</span></div>
              <div class="mentor-excerpt">${esc(m.excerpt)}…</div>
            </div>
          </div>
          <div class="mentor-quota">
            <div class="lab-metric">已录 <b>${m.quota - m.remaining}</b> / ${m.quota} 人</div>
            ${quotaDots(m.quota - m.remaining, m.quota)}
          </div>
          <div class="lab-arrow">${ICON.arrow}</div>
        </div>`).join('')}
    </div>
  </div>`;
};

VIEWS.student.lab.mount = root => {
  root.addEventListener('click', e => {
    const back = e.target.closest('[data-back]');
    if (back) { navigate(back.dataset.back); return; }
    const row = e.target.closest('[data-mentor]');
    if (row) navigate('s/mentor/' + row.dataset.mentor);
  });
};

VIEWS.student.mentor = async id => {
  const m = await api('/mentors/' + id);
  const canApply = App.boot.current && App.boot.current.code === 'INTENT';
  const interview = App.boot.stages.find(s => s.code === 'INTERVIEW');
  const confirm = App.boot.stages.find(s => s.code === 'CONFIRM');
  return `
  <div class="page-head">
    <div>
      <button class="btn-link" data-back="s/lab/${m.labId}" style="margin-bottom:10px">${ICON.arrowLeft} ${esc(m.labCode)} ${esc(m.labName)}</button>
      <h1 class="page-title mentor-title">${pixelAvatar(m.name, 40)}<span>${esc(m.name)}</span></h1>
      <p class="page-sub">${esc(m.title)}　·　${esc(m.tags)}　·　报名 ${m.applicants} 人</p>
    </div>
    <div class="page-actions">
      ${canApply
        ? `<button class="btn btn-primary" data-pick="${m.id}">加入我的意向</button>`
        : `<span class="tag tag-quiet">当前不在填报时间窗</span>`}
    </div>
  </div>

  <div class="mentor-hero">
    <div>
      <div class="intro-body"><p>${esc(m.intro || '这位导师还没有填写介绍。')}</p></div>
    </div>
    <aside>
      <div class="kv">
        <div class="kv-row"><span class="kv-k">所属实训室</span><span class="kv-v">${esc(m.labCode)} ${esc(m.labName)}</span></div>
        <div class="kv-row"><span class="kv-k">招录名额</span><span class="kv-v tnum">${m.quota} 人</span></div>
        <div class="kv-row"><span class="kv-k">剩余名额</span><span class="kv-v tnum">${m.remaining} 人</span></div>
        <div class="kv-row"><span class="kv-k">当前报名</span><span class="kv-v tnum">${m.applicants} 人</span></div>
        <div class="kv-row"><span class="kv-k">介绍状态</span><span class="kv-v">${m.introStatus === 'APPROVED' ? '已审核' : m.introStatus === 'PENDING' ? '待审核' : '草稿'}</span></div>
      </div>
      <p class="small muted" style="margin-top:16px;line-height:1.8">导师看不到你把谁填在前面，也看不到你还报了谁。名额满了他仍可面试，只是拟录取要受名额限制。</p>
    </aside>
  </div>

  <div class="sec" style="margin-top:52px">
    <div class="sec-head">
      <h2 class="sec-title">面试与考核安排</h2>
      <span class="sec-note">以实训室为单位统一组织，所有报名学生均须参加</span>
    </div>
    <div class="kv wide" style="max-width:780px">
      <div class="kv-row"><span class="kv-k">面试时间窗</span><span class="kv-v tnum">${interview ? fmtFull(interview.startAt) + ' — ' + fmtFull(interview.endAt) : '待公布'}</span></div>
      <div class="kv-row"><span class="kv-k">考察维度</span><span class="kv-v">学习态度 · 逻辑思维 · 编程基础 · 时间投入</span></div>
      <div class="kv-row"><span class="kv-k">拟录取公布</span><span class="kv-v tnum">${confirm ? fmtFull(confirm.startAt) + ' 起' : '待公布'}</span></div>
      <div class="kv-row"><span class="kv-k">录取规则</span><span class="kv-v">面试结果只决定导师是否愿意接收；最终去向由你在确认阶段选择</span></div>
    </div>
  </div>`;
};

VIEWS.student.mentor.mount = root => {
  root.addEventListener('click', e => {
    const back = e.target.closest('[data-back]');
    if (back) { navigate(back.dataset.back); return; }
    const pick = e.target.closest('[data-pick]');
    if (pick) {
      sessionStorage.setItem('pendingMentor', pick.dataset.pick);
      navigate('s/intent');
    }
  });
};

/* ------------------------------------------------------------ 意向填报 */

VIEWS.student.intent = async () => {
  const [mine, labsData, mentorData] = await Promise.all([
    api('/student/intentions'), api('/labs'), api('/mentors'),
  ]);
  const saved = sessionStorage.getItem('keepSlots');
  let chosen = saved ? JSON.parse(saved) : mine.intentions.map(i => i.mentorId);
  const pending = sessionStorage.getItem('pendingMentor');
  if (pending && !chosen.includes(Number(pending)) && chosen.length < 3) {
    chosen.push(Number(pending));
  }
  sessionStorage.removeItem('pendingMentor');
  chosen = chosen.slice(0, 3);

  const mentors = mentorData.mentors;
  const byId = Object.fromEntries(mentors.map(m => [m.id, m]));
  const groups = {};
  labsData.labs.forEach(l => groups[l.id] = { lab: l, list: [] });
  mentors.forEach(m => { if (groups[m.labId]) groups[m.labId].list.push(m); });

  const slots = [0, 1, 2].map(i => {
    const mid = chosen[i];
    const m = mid ? byId[mid] : null;
    return `<div class="slot ${m ? 'is-filled' : 'is-empty'}">
      <div class="slot-head">
        <span class="slot-no">意向 ${i + 1}</span>
        ${m ? `<button class="btn-link" data-del="${m.id}">移除</button>` : ''}
      </div>
      <div class="slot-body">
        ${m ? `<div class="slot-name">${esc(m.name)}</div>
               <div class="slot-lab">${esc(m.labCode)} ${esc(m.labName)} · ${esc(m.title)}</div>`
            : `<div class="slot-empty">空位 · 可以不填满</div>`}
      </div>
    </div>`;
  }).join('');

  const options = Object.values(groups).filter(g => g.list.length).map(g =>
    `<optgroup label="${esc(g.lab.code)} ${esc(g.lab.name)}">
      ${g.list.map(m => `<option value="${m.id}" ${chosen.includes(m.id) ? 'disabled' : ''}>${esc(m.name)} · ${esc(m.title)} · 剩余 ${m.remaining}</option>`).join('')}
    </optgroup>`).join('');

  const locked = mine.locked;
  return `
  ${pageHead('意向填报', '最多 3 个平行意向，不分先后。窗口内可以反复改，以最后一次提交为准。',
    `<span class="tag ${locked ? 'tag-quiet' : 'tag-ink'}">${locked ? '窗口已关闭' : '填报进行中'}</span>`)}
  ${stageBanner()}

  <div class="sec">
    <div class="sec-head">
      <h2 class="sec-title">我的平行意向</h2>
      <span class="sec-note tnum">已选 ${chosen.length} / 3</span>
    </div>
    <div class="slots">${slots}</div>
  </div>

  <div class="sec">
    <div class="sec-head"><h2 class="sec-title">添加导师</h2><span class="sec-note">按实训室分组，括号内是剩余名额</span></div>
    <div class="field-row" style="align-items:flex-end;max-width:720px">
      <div class="field">
        <label for="pickMentor">选择导师</label>
        <select id="pickMentor" ${locked || chosen.length >= 3 ? 'disabled' : ''}>
          <option value="">— 从实训室里挑一位 —</option>
          ${options}
        </select>
      </div>
      <button class="btn" id="addMentor" ${locked || chosen.length >= 3 ? 'disabled' : ''}>加入</button>
    </div>
  </div>

  <div class="sec">
    <div class="sec-head"><h2 class="sec-title">调剂意愿</h2><span class="sec-note">这一项会影响三轮补录后的去向</span></div>
    <div class="grid-2" style="max-width:900px">
      <label class="check ${mine.acceptInner ? 'on' : ''}">
        <input type="checkbox" id="acceptInner" ${mine.acceptInner ? 'checked' : ''} ${locked ? 'disabled' : ''}>
        <span><b>服从实训室内部调剂</b><span>同一实训室内换导师时，系统可以直接安排，不必再走一轮。</span></span>
      </label>
      <label class="check ${mine.acceptCross ? 'on' : ''}">
        <input type="checkbox" id="acceptCross" ${mine.acceptCross ? 'checked' : ''} ${locked ? 'disabled' : ''}>
        <span><b>服从全院跨实训室调剂</b><span>本室名额满了，愿意被安排到其他实训室的空余名额。</span></span>
      </label>
    </div>
  </div>

  <div class="sec" style="display:flex;gap:14px;align-items:center">
    <button class="btn btn-primary" id="submitIntent" ${locked ? 'disabled' : ''}>提交意向</button>
    ${locked ? '<span class="small muted">已过填报时间窗，提交与修改都已锁定。</span>'
             : '<span class="small muted">提交后仍可修改，截止时间一到自动锁定。</span>'}
  </div>`;
};

VIEWS.student.intent.mount = root => {
  const state = { ids: JSON.parse(sessionStorage.getItem('keepSlots') || '[]') };
  if (!state.ids.length) {
    root.querySelectorAll('[data-del]').forEach(b => state.ids.push(Number(b.dataset.del)));
  }

  root.querySelectorAll('.check input').forEach(cb => {
    cb.addEventListener('change', () => cb.closest('.check').classList.toggle('on', cb.checked));
  });

  root.addEventListener('click', e => {
    const del = e.target.closest('[data-del]');
    if (del) {
      state.ids = state.ids.filter(x => x !== Number(del.dataset.del));
      sessionStorage.setItem('keepSlots', JSON.stringify(state.ids));
      render();
      return;
    }
    if (e.target.id === 'addMentor') {
      const sel = $('#pickMentor');
      if (!sel.value) { toast('先选一位导师'); return; }
      state.ids.push(Number(sel.value));
      sessionStorage.setItem('keepSlots', JSON.stringify(state.ids));
      render();
      return;
    }
    if (e.target.id === 'submitIntent') {
      const inner = $('#acceptInner').checked;
      const cross = $('#acceptCross').checked;
      api('/student/intentions', { method: 'POST', body: { mentorIds: state.ids, acceptInner: inner, acceptCross: cross } })
        .then(() => { sessionStorage.removeItem('keepSlots'); toast('意向已提交', 'ok'); navigate('s/result'); })
        .catch(err => toast(err.message, 'err'));
    }
  });
};

/* ------------------------------------------------------------ 我的结果 */

VIEWS.student.result = async () => {
  const data = await api('/student/offers');
  const offers = data.offers;
  const pending = offers.filter(o => o.status === 'PENDING');
  const en = data.enrollment;
  const c = App.boot.current;
  const inConfirm = c && c.code === 'CONFIRM';

  let head = '';
  if (en) {
    head = `<div class="notice ok"><span class="dot"></span><span><b>去向已确定</b>　${esc(en.labCode)} ${esc(en.labName)} · ${esc(en.mentorName)}
      <br><span class="muted small">${en.source === 'ADJUST' ? '经统筹调剂安排' : en.source === 'WAITLIST' ? '经递补补位' : '面试拟录取后确认'} · ${en.status === 'FINAL' ? '已公示生效' : '待公示'}</span></span></div>`;
  } else if (pending.length >= 2) {
    head = `<div class="notice"><span class="dot"></span><span><b>收到 ${pending.length} 个拟录取，需要你选一个</b>　请在 ${fmtDateTime(data.deadline)} 前确认，逾期视为放弃。
      <br><span class="muted small">未选择的意向会自动释放给递补队列。</span></span></div>`;
  } else if (pending.length === 1) {
    head = `<div class="notice ok"><span class="dot"></span><span><b>只有一个拟录取，系统会替你确认</b>　${esc(pending[0].mentorName)} · ${esc(pending[0].labName)}
      <br><span class="muted small">无需操作，等待系统确认即可。</span></span></div>`;
  } else if (offers.length === 0) {
    head = `<div class="notice"><span class="dot"></span><span><b>还没有收到拟录取</b>　面试结束后，导师的拟录取会出现在这里；若三轮补录后仍无结果，将进入全院调剂。</span></div>`;
  }

  const cards = offers.map(o => `
    <div class="offer ${o.status === 'CONFIRMED' ? 'chosen' : ''}">
      <div>
        <div class="offer-name">${esc(o.mentorName)}</div>
        <div class="offer-meta">${esc(o.labName)} · 第 ${o.roundNo} 轮 · 面试 ${n(o.score)} 分</div>
      </div>
      <div class="offer-quote">${esc(o.comment || '')}</div>
      <div class="offer-act" style="text-align:right">
        ${o.status === 'PENDING' && inConfirm && !en
          ? `<button class="btn btn-primary btn-sm" data-confirm="${o.id}">确认去这里</button>`
          : `<span class="tag ${o.status === 'CONFIRMED' ? 'tag-ink' : o.status === 'EXPIRED' ? 'tag-quiet' : 'tag-quiet'}">${o.status === 'CONFIRMED' ? '已确认' : o.status === 'EXPIRED' ? '已逾期' : o.status === 'RELEASED' ? '已释放' : '待确认'}</span>`}
      </div>
    </div>`).join('');

  return `
  ${pageHead('我的结果', '拟录取、确认与调剂结果都在这一页。', '')}
  ${head ? `<div class="sec">${head}</div>` : ''}
  <div class="sec">
    <div class="sec-head"><h2 class="sec-title">收到的拟录取</h2><span class="sec-note tnum">共 ${offers.length} 条</span></div>
    ${offers.length ? `<div class="offer-list">${cards}</div>`
      : `<div class="empty"><b>暂无记录</b>当前阶段为「${esc(c ? c.name : '—')}」，面试结束后这里才会有内容。</div>`}
  </div>`;
};

VIEWS.student.result.mount = root => {
  root.addEventListener('click', e => {
    const b = e.target.closest('[data-confirm]');
    if (!b) return;
    b.disabled = true;
    api('/student/offers/' + b.dataset.confirm + '/confirm', { method: 'POST' })
      .then(() => { toast('已确认去向', 'ok'); render(); })
      .catch(err => { toast(err.message, 'err'); b.disabled = false; });
  });
};

/* ------------------------------------------------------------ 公示 */

VIEWS.student.notice = async () => {
  const data = await api('/announcement');
  const published = data.announcements.find(a => a.status === 'PUBLISHED');
  const total = data.groups.reduce((s, g) => s + g.rows.length, 0);
  return `
  ${pageHead('公示名单', published ? `${published.title} · 批次 ${published.batchNo} · 公示 1 天` : '最终录取名单尚未发布',
    published ? `<span class="tag tag-ok">公示中</span>` : `<span class="tag tag-quiet">未发布</span>`)}
  <div class="sec">
    ${total === 0 ? `<div class="empty"><b>还没有可公示的名单</b>三轮补录与统筹调剂结束后，学院会在这里发布最终名单。</div>` : `
      <p class="lede" style="margin-bottom:26px">共 ${total} 人已确定去向。公示期内如对结果有异议，可向教学办公室书面提出，学院将冻结相关记录并核查。</p>
      ${data.groups.filter(g => g.rows.length).map(g => `
        <div class="sec">
          <div class="sec-head"><h2 class="sec-title">${esc(g.code)}　${esc(g.name)}</h2><span class="sec-note tnum">${g.rows.length} 人</span></div>
          <div class="table-wrap"><table class="data">
            <thead><tr><th>学号</th><th>姓名</th><th>班级</th><th>导师</th><th>录取方式</th><th>状态</th></tr></thead>
            <tbody>${g.rows.map(r => `<tr>
              <td class="tnum">${esc(r.studentNo)}</td>
              <td class="cell-strong">${esc(r.studentName)}</td>
              <td class="cell-sub">${esc(r.className)}</td>
              <td>${esc(r.mentorName)}</td>
              <td class="cell-sub">${r.source === 'ADJUST' ? '统筹调剂' : r.source === 'WAITLIST' ? '递补' : '直接录取'}</td>
              <td>${r.status === 'FINAL' ? '<span class="tag tag-ok">已生效</span>' : '<span class="tag tag-quiet">待公示</span>'}</td>
            </tr>`).join('')}</tbody>
          </table></div>
        </div>`).join('')}`}
  </div>`;
};
