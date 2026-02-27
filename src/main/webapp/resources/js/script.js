console.log('script.js 로드됨');

let currentData = null; // 서버 데이터를 보관할 창고

let MONTHS = [];
const CAT_META = {
  '식비': { emoji:'🍽️', color:'#f59e0b' },
  '교통': { emoji:'🚇', color:'#3b82f6' },
  '쇼핑': { emoji:'🛍️', color:'#ec4899' },
  '여행/문화': { emoji:'🎬', color:'#10b981' },
  '의료': { emoji:'💊', color:'#8b5cf6' },
  '기타': { emoji:'📦', color:'#6b7280' },
};

const fmt = n => '₩ ' + (n * 1000).toLocaleString();

function initSelect() {
    const sel = document.getElementById('monthSelect');
    if (!sel) return;
    sel.innerHTML = "";
    MONTHS.forEach((m, i) => {
        const opt = document.createElement('option');
        opt.value = m;
        opt.textContent = m;
        if (i === 0) opt.selected = true;
        sel.appendChild(opt);
    });
}

function getSelected() {
    const sel = document.getElementById('monthSelect');
    return sel ? sel.value : "2023q3";
}

function onMonthChange() {
    getReportMonthsExpense("WDJXI9MJ1X41AITHZ3IU", getSelected());
}



function transformData(db) {
  return {
    total: db.totUseAm,
    cats: {
      '식비': db.fsbzAm,
      '교통': db.autoAm,
      '쇼핑': db.distAm,
      '여행/문화': db.trvlecAm,
      '의료': db.hosAm || 0,
      '기타': db.svcarcAm || 0
    },
    fixed: [
      { emoji: '🏥', name: '보험/의료', amt: db.insuhosAm },
      { emoji: '📚', name: '교육/사무', amt: db.offeduAm }
    ]
  };
}

// 핵심 수정: 인자가 없으면(체크박스 클릭 시) 보관된 데이터를 사용함
function renderAll(data) {
  const targetData = data || currentData; 
  if (!targetData) return;

  const key = getSelected();
  const includeFixed = document.getElementById('fixedToggle').checked;
  const fixedTotal = targetData.fixed.reduce((s, f) => s + f.amt, 0);
  const displayTotal = includeFixed ? targetData.total + fixedTotal : targetData.total;

  document.getElementById('totalSpend').textContent = fmt(displayTotal);
  document.getElementById('totalDiff').textContent = `${key} 기준`;

  const sorted = Object.entries(targetData.cats).sort((a,b) => b[1] - a[1]);
  const top = sorted[0];
  if(top) {
      document.getElementById('topCat').textContent = CAT_META[top[0]].emoji + ' ' + top[0];
      document.getElementById('topCatAmt').textContent = `${fmt(top[1])} (${Math.round(top[1]/targetData.total*100)}%)`;
  }

  renderCatBars(targetData);
  renderFixedExpenses(targetData.fixed, key);
}

function renderCatBars(data) {
  const sorted = Object.entries(data.cats).sort((a,b) => b[1] - a[1]);
  if (sorted.length === 0) return;
  
  const maxVal = sorted[0][1] || 1; 
  document.getElementById('catBars').innerHTML = sorted.map(([cat, amt]) => {
    const meta = CAT_META[cat] || CAT_META['기타'];
    const pct = Math.round(amt / data.total * 100) || 0;
    const barW = Math.round(amt / maxVal * 100) || 0;
    return `
      <div class="cat-row">
        <div class="cat-top">
          <div class="cat-left"><span class="cat-emoji">${meta.emoji}</span>${cat}</div>
          <div><span class="cat-amount">${fmt(amt)}</span><span class="cat-pct">${pct}%</span></div>
        </div>
        <div class="bar-track"><div class="bar-fill" style="width:${barW}%;background:${meta.color}"></div></div>
      </div>`;
  }).join('');
}

function renderFixedExpenses(fixedItems, key) {
  const total = fixedItems.reduce((s, f) => s + f.amt, 0);
  document.getElementById('fixedTotal').textContent = fmt(total);
  document.getElementById('fixedSubLabel').textContent = `${key} 기준`;
  
  document.getElementById('fixedGrid').innerHTML = fixedItems.map(f => `
    <div class="fixed-item">
      <div class="fixed-left"><span class="fixed-emoji">${f.emoji}</span>${f.name}</div>
      <span class="fixed-amt">${fmt(f.amt)}</span>
    </div>`).join('');
}

$(document).ready(function() {
	getPaymentDates("WDJXI9MJ1X41AITHZ3IU");
});