(function () {
  try {
    if (window.__gcptGrades && window.__gcptGrades.done === false) return 'busy';
    var st = {
      v: 2, done: false, at: Date.now(), error: null, count: 0, terms: []
    };
    window.__gcptGrades = st;

    var body = new URLSearchParams({
      xnm: '', xqm: '', sfzgcj: '', cxbkjgcj: '', kcbj: '', pkey: '',
      _search: 'false', nd: String(Date.now()),
      'queryModel.showCount': '500',
      'queryModel.currentPage': '1',
      'queryModel.sortName': '',
      'queryModel.sortOrder': 'asc',
      time: '1'
    }).toString();

    var timer = setTimeout(function () {
      if (!st.done) { st.error = 'timeout'; st.done = true; }
    }, 20000);

    fetch('/jwglxt/cjcx/cjcx_cxXsgrcj.html?doType=query&gnmkdm=N305005', {
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
      for (var i = 0; i < items.length; i++) {
        var it = items[i];
        if (!it || !it.kcmc) continue;
        var xn = String(it.xnm == null ? '' : it.xnm).trim();
        var xq = String(it.xqm == null ? '' : it.xqm).trim();
        if (!xn || !xq) continue;
        var key = xn + '/' + xq;
        if (!map[key]) { map[key] = { xnm: xn, xqm: xq, rows: [] }; order.push(key); }
        map[key].rows.push({
          n: String(it.kcmc || '').trim(),
          c: it.xf == null ? '' : String(it.xf).trim(),
          g: it.jd == null ? '' : String(it.jd).trim(),
          s: String(it.cj == null ? (it.bfzcj == null ? '' : it.bfzcj) : it.cj).trim(),
          k: String(it.kcxzmc || '').trim(),
          t: String(it.jsxm || '').trim()
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
        var credits = 0, wsum = 0, wc = 0;
        for (var r = 0; r < g.rows.length; r++) {
          var cr = parseFloat(g.rows[r].c), gp = parseFloat(g.rows[r].g);
          if (!isNaN(cr)) credits += cr;
          if (!isNaN(cr) && !isNaN(gp)) { wsum += cr * gp; wc += cr; }
        }
        terms.push({
          term: g.xnm + '-' + (Number(g.xnm) + 1) + ' 第' + (g.xqm === '3' ? '1' : '2') + '学期',
          xnm: g.xnm,
          xqm: g.xqm,
          credits: Math.round(credits * 10) / 10,
          gpa: wc > 0 ? Math.round(wsum / wc * 100) / 100 : 0,
          rows: g.rows
        });
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
