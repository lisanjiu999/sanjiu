/* 管理员端 */

VIEWS.admin.dashboard = async () => {
  const d = await api('/admin/dashboard');
  const t = d.totals;
  const c = App.boot.current;
  const next = App.boot.stages.find(s => s.seq === (c ? c.seq + 1 : 1));
  const pct = t.students ? Math.round((t.enrolled / t.students) * 100) : 0;

  return `
  ${pageHead('录取看板', '名额、报名、确认与递补的实时口径。所有数字都从录取表算出，不做二次维护。',
    `<button class="btn" data-goto-clock type="button">调节演示时间</button>
     <button class="btn btn-primary" id="advanceBtn" ${next ? '' : 'disabled'}>推进到「${next ? esc(next.name) : '已结束'}」</button>`)}

  <div class="sec">
    <p class="lede">全院 ${t.students} 名学生，${t.submitted} 人已提交意向，共收到 ${t.applicants} 份报名；目前 ${t.enrolled} 人已确认去向（占 ${pct}%），${t.unplaced} 人尚未落定，其中 ${t.adjusted} 人经统筹调剂安排。${t.pending} 条拟录取等待学生确认。</p>
  </div>

  <div class="sec">
    <div class="sec-head"><h2 class="sec-title">分实训室</h2><span class="sec-note">剩余名额 = 导师名额之和 − 已确认人数</span></div>
    <div class="table-wrap"><table class="data">
      <thead><tr>
        <th>实训室</th><th class="num" style="width:70px">名额</th><th class="num" style="width:70px">报名</th><th class="num" style="width:78px">拟录取</th>
        <th class="num" style="width:70px">递补</th><th class="num" style="width:78px">已确认</th><th class="num" style="width:96px">剩余</th><th style="width:156px">占用</th>
      </tr></thead>
      <tbody>${d.labs.map(l => `<tr>
        <td><div class="cell-lab"><i style="background:${labColor(l.code)}"></i><span class="cell-strong">${esc(l.code)}</span><span>${esc(l.name)}</span></div><div class="cell-sub">${esc(l.direction)}</div></td>
        <td class="num">${l.quota}</td>
        <td class="num">${l.applicants}</td>
        <td class="num">${l.admits}</td>
        <td class="num">${l.waits}</td>
        <td class="num cell-strong">${l.enrolled}</td>
        <td class="num ${l.remaining === 0 ? 'cell-strong' : ''}">${l.remaining}${l.remaining === 0 ? '<span class="tag tag-red" style="margin-left:8px">已满</span>' : ''}</td>
        <td><div class="cell-meter">${meter(l.enrolled, l.quota)}<b>${l.quota ? Math.round(l.enrolled / l.quota * 100) : 0}%</b></div></td>
      </tr>`).join('')}</tbody>
    </table></div>
  </div>

  <div class="sec">
    <div class="sec-head"><h2 class="sec-title">当前阶段</h2></div>
    <div class="kv wide" style="max-width:780px">
      <div class="kv-row"><span class="kv-k">阶段</span><span class="kv-v">${esc(c ? c.seq + '. ' + c.name : '—')}</span></div>
      <div class="kv-row"><span class="kv-k">说明</span><span class="kv-v">${esc(c ? c.summary : '')}</span></div>
      <div class="kv-row"><span class="kv-k">时间窗</span><span class="kv-v tnum">${fmtFull(c ? c.startAt : '')} — ${fmtFull(c ? c.endAt : '')}</span></div>
      <div class="kv-row"><span class="kv-k">演示时钟</span><span class="kv-v tnum">${fmtFull(App.boot.demoNow)}
        <button class="btn-link" data-goto-clock type="button" style="margin-left:10px">调节</button></span></div>
    </div>
  </div>`;
};

VIEWS.admin.dashboard.mount = root => {
  root.querySelectorAll('[data-goto-clock]').forEach(el =>
    el.addEventListener('click', () => navigate('a/stages')));
  const btn = $('#advanceBtn');
  if (!btn) return;
  btn.addEventListener('click', () => {
    btn.disabled = true;
    api('/admin/stage/advance', { method: 'POST' })
      .then(async res => {
        toast('已进入「' + res.stage + '」' + (res.log.length ? '：' + res.log.join('；') : ''), 'ok');
        await boot();
        render();
      })
      .catch(e => { toast(e.message, 'err'); btn.disabled = false; });
  });
};

VIEWS.admin.base = async () => {
  const [labsData, mentorData] = await Promise.all([api('/labs'), api('/mentors')]);
  const labs = labsData.labs;
  const labOptions = labs.map(l => `<option value="${l.id}">${esc(l.code)} ${esc(l.name)}</option>`).join('');
  const labName = Object.fromEntries(labs.map(l => [l.id, l.code + ' ' + l.name]));

  return `
  ${pageHead('基础数据', '导师可以新增、在实训室之间移动、停用。名下已有学生的导师不能直接移动或删除。', '')}

  <div class="sec">
    <div class="sec-head"><h2 class="sec-title">导师</h2><span class="sec-note tnum">${mentorData.mentors.length} 位在岗</span></div>
    <div class="table-wrap"><table class="data">
      <thead><tr><th>姓名</th><th>职称</th><th>方向</th><th>所属实训室</th><th class="num">名额</th><th class="num">剩余</th><th>介绍</th><th style="text-align:right">操作</th></tr></thead>
      <tbody>${mentorData.mentors.map(m => `<tr>
        <td><div class="cell-who">${pixelAvatar(m.name, 20)}<span class="cell-strong">${esc(m.name)}</span></div></td>
        <td class="cell-sub">${esc(m.title)}</td>
        <td class="cell-sub">${esc(m.tags)}</td>
        <td>
          <select data-move="${m.id}" style="min-width:144px">
            ${labs.map(l => `<option value="${l.id}" ${l.id === m.labId ? 'selected' : ''}>${esc(l.code)} ${esc(l.name)}</option>`).join('')}
          </select>
        </td>
        <td class="num">${m.quota}</td>
        <td class="num">${m.remaining}</td>
        <td class="cell-sub">${m.introStatus === 'APPROVED' ? '已审核' : m.introStatus === 'PENDING' ? '待审核' : '草稿'}</td>
        <td style="text-align:right"><button class="btn btn-sm" data-del="${m.id}">停用</button></td>
      </tr>`).join('')}</tbody>
    </table></div>
  </div>

  <div class="sec">
    <div class="sec-head"><h2 class="sec-title">新增导师</h2><span class="sec-note">新建后默认为草稿，导师自行完善介绍</span></div>
    <div class="field-row" style="max-width:1000px;align-items:flex-end;flex-wrap:wrap">
      <div class="field" style="flex:1.4"><label>姓名</label><input type="text" id="nName" placeholder="张…"></div>
      <div class="field" style="flex:1"><label>职称</label><input type="text" id="nTitle" placeholder="副教授"></div>
      <div class="field" style="flex:1.6"><label>方向</label><input type="text" id="nTags" placeholder="方向一 · 方向二"></div>
      <div class="field" style="flex:1.4"><label>实训室</label><select id="nLab">${labOptions}</select></div>
      <div class="field" style="flex:.6"><label>名额</label><input type="number" id="nQuota" value="3" min="1" max="10"></div>
      <button class="btn btn-primary" id="addMentorBtn">新增</button>
    </div>
  </div>`;
};

VIEWS.admin.base.mount = root => {
  root.addEventListener('change', e => {
    const sel = e.target.closest('[data-move]');
    if (!sel) return;
    api('/admin/mentors/' + sel.dataset.move + '/move', { method: 'PUT', body: { labId: Number(sel.value) } })
      .then(() => toast('已移动到新实训室', 'ok'))
      .catch(err => { toast(err.message, 'err'); render(); });
  });

  root.addEventListener('click', e => {
    const del = e.target.closest('[data-del]');
    if (del) {
      if (!confirm('停用该导师？停用后学生端不再展示，已报名学生需另行处理。')) return;
      api('/admin/mentors/' + del.dataset.del, { method: 'DELETE' })
        .then(() => { toast('已停用', 'ok'); render(); })
        .catch(err => toast(err.message, 'err'));
      return;
    }
    if (e.target.id === 'addMentorBtn') {
      const body = {
        name: $('#nName').value.trim(),
        title: $('#nTitle').value.trim(),
        tags: $('#nTags').value.trim(),
        labId: Number($('#nLab').value),
        quota: Number($('#nQuota').value),
      };
      if (!body.name) { toast('先填姓名'); return; }
      api('/admin/mentors', { method: 'POST', body })
        .then(() => { toast('已新增', 'ok'); render(); })
        .catch(err => toast(err.message, 'err'));
    }
  });
};

/* 后端返回 2026-11-02T09:30:00，datetime-local 只吃 2026-11-02T09:30 */
const localInput = iso => String(iso || '').slice(0, 16);
/* Date → 本地时区的 datetime-local 值（不能用 toISOString，那是 UTC） */
const stamp = d => {
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
};

VIEWS.admin.stages = async () => {
  const stages = App.boot.stages;
  const cur = App.boot.current;
  const next = cur ? stages.find(s => s.seq === cur.seq + 1) : null;
  return `
  ${pageHead('阶段控制', '八个阶段由时间窗驱动，服务端裁决。整站的「现在」取自下面这台演示时钟，拨到哪一天，各端权限就走到哪一步。')}

  <div class="sec">
    <div class="clock-card">
      <div class="clock-head">
        <div class="clock-copy">
          <h3>演示时钟</h3>
          <p>学生的填报、导师的评分、管理员的确认，都按这个时刻开闭。</p>
        </div>
        <div class="clock-read">
          <b class="tnum">${fmtFull(App.boot.demoNow)}</b>
          <span>${cur ? '第 ' + cur.seq + ' 阶段 · ' + esc(cur.name) : '未进入任何阶段'}</span>
        </div>
      </div>
      <div class="clock-row">
        <label class="clock-field">
          <span>拨到</span>
          <input type="datetime-local" id="clockAt" step="60" value="${localInput(App.boot.demoNow)}">
        </label>
        <button class="btn btn-primary" id="clockApply">应用</button>
        <div class="clock-quick">
          <button class="btn btn-sm" data-shift="-1440" type="button">前一天</button>
          <button class="btn btn-sm" data-shift="1440" type="button">后一天</button>
          <button class="btn btn-sm" data-jump="start" type="button">本阶段开始</button>
          <button class="btn btn-sm" data-jump="next" type="button" ${next ? '' : 'disabled'}>下一阶段</button>
        </div>
      </div>
    </div>
  </div>

  <div class="sec">
    <div class="sec-head"><h2 class="sec-title">时间窗</h2><span class="sec-note">改完即生效，不用重启；开始必须早于结束</span></div>
    <div class="table-wrap"><table class="data stages-table">
      <thead><tr>
        <th class="num" style="width:56px">序号</th><th>阶段</th>
        <th style="width:196px">开始</th><th style="width:196px">结束</th>
        <th style="width:92px">状态</th>
      </tr></thead>
      <tbody>${stages.map(s => {
        const st = stageState(s, cur);
        return `<tr class="${st === 'active' ? 'is-active' : ''}">
          <td class="num tnum">${String(s.seq).padStart(2, '0')}</td>
          <td>
            <div class="cell-strong">${esc(s.name)}</div>
            <div class="cell-sub">${esc(s.summary)}</div>
          </td>
          <td><input type="datetime-local" class="win-input" step="60"
                data-id="${s.id}" data-f="startAt" value="${localInput(s.startAt)}" aria-label="${esc(s.name)} 开始时间"></td>
          <td><input type="datetime-local" class="win-input" step="60"
                data-id="${s.id}" data-f="endAt" value="${localInput(s.endAt)}" aria-label="${esc(s.name)} 结束时间"></td>
          <td>${st === 'active' ? '<span class="tag tag-ink">进行中</span>' : st === 'done' ? '<span class="tag tag-ok">已结束</span>' : '<span class="tag tag-quiet">未开始</span>'}</td>
        </tr>`;
      }).join('')}</tbody>
    </table></div>
  </div>

  <div class="sec">
    <div class="notice"><span class="dot"></span><span>
      上面的「推进到下一阶段」会顺带结算上一阶段：面试结束生成拟录取，确认结束释放逾期名额并按顺序递补，公示开始时录取结果转正式。
      <br><span class="muted small">直接拨时钟不会触发这些结算动作——它只是让系统「以为」到了某一天，适合演示某个阶段的界面状态。</span>
    </span></div>
  </div>`;
};

VIEWS.admin.stages.mount = root => {
  const input = $('#clockAt');

  const applyClock = async at => {
    if (!at) { toast('先选一个时间'); return; }
    try {
      await api('/admin/stage/clock', { method: 'PUT', body: { at } });
      await boot();
      render();
      toast('时钟已拨到 ' + fmtFull(at), 'ok');
    } catch (e) { toast(e.message, 'err'); }
  };

  $('#clockApply').addEventListener('click', () => applyClock(input.value));
  input.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); applyClock(input.value); } });

  root.querySelectorAll('[data-shift]').forEach(btn => btn.addEventListener('click', () => {
    const d = new Date(App.boot.demoNow);
    d.setMinutes(d.getMinutes() + Number(btn.dataset.shift));
    applyClock(stamp(d));
  }));

  root.querySelectorAll('[data-jump]').forEach(btn => btn.addEventListener('click', () => {
    const cur = App.boot.current;
    if (!cur) return;
    if (btn.dataset.jump === 'start') { applyClock(localInput(cur.startAt)); return; }
    const nx = App.boot.stages.find(s => s.seq === cur.seq + 1);
    if (nx) applyClock(localInput(nx.startAt));
  }));

  root.querySelectorAll('.win-input').forEach(el => el.addEventListener('change', async () => {
    const tr = el.closest('tr');
    const start = tr.querySelector('[data-f="startAt"]').value;
    const end = tr.querySelector('[data-f="endAt"]').value;
    if (!start || !end) return;
    try {
      await api('/admin/stage/' + el.dataset.id + '/window', { method: 'PUT', body: { startAt: start, endAt: end } });
      await boot();
      render();
      toast('时间窗已更新', 'ok');
    } catch (e) {
      toast(e.message, 'err');
      render();
    }
  }));
};

VIEWS.admin.adjust = async () => {
  const data = await api('/admin/adjust/preview');
  const s = data.stat;
  const groups = { 1: '室内调剂', 2: '跨室调剂', 3: '最终安置' };
  return `
  ${pageHead('统筹调剂', '三轮补录后仍未落定的学生进入调剂池，按三级优先依次分配。这一步只是试算，确认后才落库。',
    `<button class="btn btn-primary" id="commitBtn" ${s.pool === 0 ? 'disabled' : ''}>确认落库</button>`)}

  <div class="sec">
    <p class="lede">调剂池 ${s.pool} 人。${[
      s.level1 ? `${s.level1} 人可在本实训室内换导师解决` : '',
      s.level2 ? `${s.level2} 人需要跨实训室安排` : '',
      s.level3 ? `${s.level3} 人未服从调剂，直接统筹安置` : '',
      s.failed ? `${s.failed} 人全院名额已满，需人工处理` : ''
    ].filter(Boolean).join('；')}。分配顺序按绩点与面试结果降序，余量大的导师优先承接。</p>
  </div>

  <div class="sec">
    ${data.rows.length === 0 ? `<div class="empty"><b>调剂池是空的</b>所有学生都已确定去向，无需调剂。</div>` : `
    <div class="table-wrap"><table class="data">
      <thead><tr><th>学号</th><th>姓名</th><th>班级</th><th class="num">绩点</th><th>优先级</th><th>去向</th><th>依据</th></tr></thead>
      <tbody>${data.rows.map(r => `<tr>
        <td class="tnum cell-sub">${esc(r.no)}</td>
        <td><div class="cell-who">${pixelAvatar(r.name, 15)}<span class="cell-strong">${esc(r.name)}</span></div></td>
        <td class="cell-sub">${esc(r.className)}</td>
        <td class="num">${r.gpa.toFixed(1)}</td>
        <td>${r.level === 1 ? '<span class="tag tag-ok">一 · 室内</span>' : r.level === 2 ? '<span class="tag">二 · 跨室</span>' : '<span class="tag tag-quiet">三 · 统筹</span>'}</td>
        <td>${r.mentorName ? esc(r.labCode + ' · ' + r.mentorName) : '<span class="muted">待人工处理</span>'}</td>
        <td class="cell-sub">${esc(r.reason)}</td>
      </tr>`).join('')}</tbody>
    </table></div>`}
  </div>`;
};

VIEWS.admin.adjust.mount = root => {
  const btn = $('#commitBtn');
  if (!btn) return;
  btn.addEventListener('click', () => {
    if (!confirm('确认执行调剂？将写入录取表与调剂记录，操作留痕。')) return;
    btn.disabled = true;
    api('/admin/adjust/commit', { method: 'POST' })
      .then(res => { toast('已安排 ' + res.count + ' 人', 'ok'); render(); })
      .catch(e => { toast(e.message, 'err'); btn.disabled = false; });
  });
};

VIEWS.admin.notice = async () => {
  const data = await api('/announcement');
  const total = data.groups.reduce((s, g) => s + g.rows.length, 0);
  return `
  ${pageHead('公示管理', '生成公示名单并发布，公示期 1 天。发布后录取结果转为正式。',
    `<button class="btn btn-primary" id="publishBtn" ${total === 0 ? 'disabled' : ''}>发布公示</button>`)}

  <div class="sec">
    <div class="field" style="max-width:720px;margin-bottom:0">
      <label for="annTitle">公示标题</label>
      <input type="text" id="annTitle" maxlength="60" placeholder="实训室进组选拔录取名单公示">
      <p class="small muted" style="margin:0">留空就用默认标题。学期、届次自己写，例如「2027 年春季学期实训室进组选拔录取名单公示」。</p>
    </div>
  </div>

  <div class="sec">
    <div class="kv" style="max-width:720px">
      <div class="kv-row"><span class="kv-k">待公示人数</span><span class="kv-v tnum">${total} 人</span></div>
      <div class="kv-row"><span class="kv-k">已发布批次</span><span class="kv-v">${data.announcements.length ? data.announcements.map(a => esc(a.batchNo)).join('、') : '无'}</span></div>
      <div class="kv-row"><span class="kv-k">异议</span><span class="kv-v">${data.announcements.reduce((s, a) => s + a.objections, 0)} 条</span></div>
    </div>
  </div>

  <div class="sec">
    <div class="sec-head"><h2 class="sec-title">名单预览</h2><span class="sec-note">按实训室分组，与公示页一致</span></div>
    ${total === 0 ? `<div class="empty"><b>暂无可公示名单</b>需先完成确认或调剂。</div>` : `
    <div class="table-wrap"><table class="data">
      <thead><tr><th>实训室</th><th class="num">人数</th><th>学生</th></tr></thead>
      <tbody>${data.groups.filter(g => g.rows.length).map(g => `<tr>
        <td class="cell-strong">${esc(g.code)}　${esc(g.name)}</td>
        <td class="num">${g.rows.length}</td>
        <td class="cell-sub">${g.rows.map(r => esc(r.studentName)).join('、')}</td>
      </tr>`).join('')}</tbody>
    </table></div>`}
  </div>`;
};

VIEWS.admin.notice.mount = root => {
  const btn = $('#publishBtn');
  if (!btn) return;
  btn.addEventListener('click', () => {
    btn.disabled = true;
    api('/admin/announce', { method: 'POST', body: { title: ($('#annTitle').value || '').trim() } })
      .then(() => { toast('公示已发布', 'ok'); render(); })
      .catch(e => { toast(e.message, 'err'); btn.disabled = false; });
  });
};
