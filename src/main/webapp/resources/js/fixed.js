/**
 * 고정지출 데이터 정의 (기존 데이터 유지)
 */
const CATEGORIES = [
  {
    id: 'housing_mgmt', label: '🏠 주거/시설관리',
    items: [
      { id: 'BLDMNG_AMDEC', emoji: '🏢', name: '건물/시설관리' },
      { id: 'ARCHIT_AMDEC', emoji: '🛠️', name: '건축/자재' },
      { id: 'OFFCOM_AMDEC', emoji: '📡', name: '사무/통신기기' },
    ]
  },
  {
    id: 'health_medical', label: '🏥 보험/의료',
    items: [
      { id: 'INSU_AMDEC',   emoji: '🛡️', name: '보험료' },
      { id: 'HOS_AMDEC',    emoji: '🏥', name: '의료기관(병원)' },
      { id: 'SANIT_AMDEC',  emoji: '🧼', name: '보건/위생' },
      { id: 'HLTHFS_AMDEC', emoji: '💊', name: '건강식품' },
    ]
  },
  {
    id: 'education_book', label: '📚 교육/도서',
    items: [
      { id: 'ACDM_AMDEC',   emoji: '🏫', name: '학원비' },
      { id: 'BOOK_AMDEC',   emoji: '📖', name: '서적/문구' },
    ]
  },
  {
    id: 'service_membership', label: '🤝 용역/회원제',
    items: [
      { id: 'SVC_AMDEC',     emoji: '🛠️', name: '용역 서비스' },
      { id: 'MBRSHOP_AMDEC', emoji: '💳', name: '회원제 형태 업소' },
      { id: 'RPR_AMDEC',     emoji: '🔧', name: '수리 서비스' },
    ]
  },
  {
    id: 'auto_fuel', label: '🚗 자동차/유지',
    items: [
      { id: 'FUEL_AMDEC',    emoji: '⛽', name: '연료판매(주유)' },
      { id: 'AUTOMNT_AMDEC', emoji: '🏎️', name: '자동차정비/유지' },
    ]
  },
  {
    id: 'etc_fixed', label: '📦 기타 생활',
    items: [
      { id: 'AGRICTR_AMDEC', emoji: '🌾', name: '농업 관련' },
      { id: 'OPTIC_AMDEC',   emoji: '👓', name: '광학제품' },
    ]
  }
];

let selected = {};

/**
 * 초기화 함수
 */
function init() {
  renderTabs();
  renderPanels();
  renderApplyMonth();
  activateTab(CATEGORIES[0].id);
}

// ── 핵심 수정: data-속성을 활용한 패널 렌더링 ────────────────
function renderPanels() {
  document.getElementById('panels').innerHTML = CATEGORIES.map(c => `
    <div class="cat-panel" id="panel-${c.id}">
      <div class="btn-grid">
        ${c.items.map(item => `
          <button class="cat-btn" id="btn-${item.id}"
            data-id="${item.id}"
            data-emoji="${item.emoji}"
            data-name="${item.name}"
            data-label="${c.label}"
            onclick="handleItemClick(this)">
            <span>${item.emoji}</span>
            <span>${item.name}</span>
            <span class="check">✓</span>
          </button>
        `).join('')}
      </div>
    </div>
  `).join('');
}

// 버튼 클릭 시 데이터를 읽어오는 핸들러 함수
function handleItemClick(btn) {
  const { id, emoji, name, label } = btn.dataset;
  toggleItem(id, emoji, name, label);
}

function toggleItem(id, emoji, name, catLabel) {
  const btn = document.getElementById('btn-' + id);
  if (selected[id]) {
    delete selected[id];
    if (btn) btn.classList.remove('selected');
  } else {
    selected[id] = { emoji, name, catLabel, amount: '' };
    if (btn) btn.classList.add('selected');
  }
  renderAmountRows();
  renderSidebar();
}

function renderAmountRows() {
  const keys = Object.keys(selected);
  const container = document.getElementById('amountRows');
  if (keys.length === 0) {
    container.innerHTML = '<p class="empty-msg">위에서 항목을 선택하면 여기에 표시돼요.</p>';
    return;
  }
  container.innerHTML = keys.map(id => {
    const s = selected[id];
    return `
      <div class="amount-row" id="row-${id}">
        <span class="amount-row-emoji">${s.emoji}</span>
        <span class="amount-row-name">${s.name}</span>
        <div class="amount-wrap">
          <span class="amount-prefix">₩</span>
          <input class="amount-input" type="number" placeholder="금액 입력"
            value="${s.amount}"
            oninput="updateAmount('${id}', this.value)">
        </div>
        <button class="remove-btn" onclick="removeItem('${id}')" title="삭제">×</button>
      </div>
    `;
  }).join('');
}

// ── 나머지 유틸리티 로직 (기존 유지 및 보완) ────────────────
function updateAmount(id, val) {
  if (selected[id]) {
    selected[id].amount = val;
    updateSummary();
  }
}

function removeItem(id) {
  delete selected[id];
  const btn = document.getElementById('btn-' + id);
  if (btn) btn.classList.remove('selected');
  renderAmountRows();
  renderSidebar();
}

function renderSidebar() {
  updateSummary();
  renderRegisteredList();
  document.getElementById('submitBtn').disabled = Object.keys(selected).length === 0;
}

function updateSummary() {
  const keys = Object.keys(selected);
  const total = keys.reduce((sum, id) => sum + (Number(selected[id].amount) || 0), 0);
  document.getElementById('totalAmount').textContent = '₩ ' + total.toLocaleString();
  document.getElementById('totalCount').textContent =
    keys.length > 0 ? `${keys.length}개 항목` : '선택된 항목 없음';
}

function renderRegisteredList() {
  const keys = Object.keys(selected);
  const el = document.getElementById('registeredList');
  if (keys.length === 0) {
    el.innerHTML = '<p class="empty-side">선택된 항목이 없어요.</p>';
    return;
  }
  el.innerHTML = keys.map(id => {
    const s = selected[id];
    const amt = s.amount ? '₩ ' + Number(s.amount).toLocaleString() : '—';
    return `
      <div class="reg-item">
        <div class="reg-left">
          <div class="reg-icon">${s.emoji}</div>
          <div class="reg-name">${s.name}</div>
        </div>
        <div class="reg-amount">${amt}</div>
      </div>
    `;
  }).join('');
}

function renderTabs() {
  document.getElementById('tabBar').innerHTML = CATEGORIES.map(c =>
    `<button class="tab-btn" data-id="${c.id}" onclick="activateTab('${c.id}')">${c.label}</button>`
  ).join('');
}

function activateTab(id) {
  document.querySelectorAll('.tab-btn').forEach(b =>
    b.classList.toggle('active', b.dataset.id === id)
  );
  document.querySelectorAll('.cat-panel').forEach(p =>
    p.classList.toggle('active', p.id === 'panel-' + id)
  );
}

function renderApplyMonth() {
  const sel = document.getElementById('applyMonth');
  if(!sel) return;
  const now = new Date();
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
    const val = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`;
    const label = `${d.getFullYear()}년 ${d.getMonth()+1}월`;
    const opt = document.createElement('option');
    opt.value = val; opt.textContent = label;
    sel.appendChild(opt);
  }
}

/**
 * 서버 데이터 제출
 */
function submitFixed() {
  const keys = Object.keys(selected);
  const hasEmpty = keys.some(id => !selected[id].amount);
  
  if (hasEmpty) {
    showToast('❗ 금액이 입력되지 않은 항목이 있어요.');
    return;
  }

  const date = document.getElementById('applyMonth').value;
  const userNo = "WDJXI9MJ1X41AITHZ3IU";

  // 서블릿의 단일 업데이트 구조에 맞춰 개별 전송
  keys.forEach(id => {
    const payload = {
      category: id, // 실제 DB 컬럼명 (예: INSU_AMDEC)
      cost: selected[id].amount,
      date: date,
      userNo: userNo
    };
    
    // AJAX 요청 함수 호출
    updateFixedCost(payload);
  });
}

function showToast(msg) {
  const t = document.getElementById('toast');
  if(t) {
    t.textContent = msg;
    t.classList.add('show');
    setTimeout(() => t.classList.remove('show'), 2500);
  }
}

init();