(function () {
  try {
    if (window.__gcptExams && window.__gcptExams.done === false) return 'busy';
    var st = { v: 2, done: false, at: Date.now(), error: null, count: 0, terms: [] };
    window.__gcptExams = st;

    var body = new URLSearchParams({
      xnm: '', xqm: '', ksmcdmb_id: '', kch: '', kc: '', ksrq: '', kkbm_id: '',
      _search: 'false', nd: String(Date.now()),
      'queryModel.showCount': '500',
      'queryModel.currentPage': '1',
      'queryModel.sortName': ' ',
      'queryModel.sortOrder': 'asc',
      time: '0'
    }).toString();

    var timer = setTimeout(function () {
      if (!st.done) { st.error = 'timeout'; st.done = true; }
    }, 20000);

    fetch('/jwglxt/kwgl/kscx_cxXsksxxIndex.html?doType=query&gnmkdm=N358105', {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
        'X-Requested-With': 'XMLHttpRequest'
      },
      body: body
    }).then(function (r) {
      if (r.status === 401 || r.status === 403) {
        st.error = 'auth';
        st.done = true;
        return null;
      }
      return r.text();
    }).then(function (txt) {
      if (txt == null || st.done) return;
      if (txt.charAt(0) === '<') { st.error = 'auth'; st.done = true; return; }
      var j;
      try { j = JSON.parse(txt); } catch (e) { st.error = 'parse'; st.done = true; return; }
      var items = j.items || [];
      var map = {}, order = [];
      var dateRe = /(\d{4})-(\d{1,2})-(\d{1,2})/;
      var timeRe = /[（(]\s*(\d{1,2}:\d{2})\s*[-–~至]\s*(\d{1,2}:\d{2})\s*[）)]/;
      for (var i = 0; i < items.length; i++) {
        var it = items[i];
        if (!it || !it.kcmc) continue;
        var xn = String(it.xnm == null ? '' : it.xnm).trim();
        var xq = String(it.xqm == null ? '' : it.xqm).trim();
        if (!xn || !xq) continue;
        var key = xn + '/' + xq;
        if (!map[key]) {
          map[key] = {
            xnm: xn, xqm: xq,
            term: (it.xnmc || xn) + ' 第' + (it.xqmmc || (xq === '3' ? '1' : '2')) + '学期',
            exams: []
          };
          order.push(key);
        }
        var kssj = String(it.kssj || '').trim();
        var dm = dateRe.exec(kssj);
        var tm = timeRe.exec(kssj);
        map[key].exams.push({
          n: String(it.kcmc || '').trim(),
          d: dm ? dm[1] + '-' + ('0' + dm[2]).slice(-2) + '-' + ('0' + dm[3]).slice(-2) : '',
          t: tm ? tm[1] + '-' + tm[2] : '',
          s: kssj,
          r: String(it.cdmc || '').trim(),
          c: String(it.cdxqmc || '').trim(),
          z: String(it.zwh == null ? '' : it.zwh).trim(),
          m: String(it.ksmc || '').trim(),
          f: String(it.ksfs || it.khfs || '').trim()
        });
      }
      order.sort(function (a, b) {
        var A = a.split('/'), B = b.split('/');
        if (A[0] !== B[0]) return Number(B[0]) - Number(A[0]);
        return Number(B[1]) - Number(A[1]);
      });
      var terms = [];
      for (var k = 0; k < order.length; k++) {
        var g = map[order[k]];
        g.exams.sort(function (a, b) { return a.d < b.d ? -1 : a.d > b.d ? 1 : 0; });
        terms.push(g);
      }
      st.count = items.length;
      st.terms = terms;
      if (!terms.length) st.error = 'empty';
      clearTimeout(timer);
      st.done = true;
    }).catch(function (e) {
      clearTimeout(timer);
      st.error = 'net';
      st.detail = String(e && e.message || e).slice(0, 120);
      st.done = true;
    });

    return 'started';
  } catch (e) {
    return 'throw:' + e;
  }
})()
