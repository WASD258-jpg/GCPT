(function () {
  try {
    var token = localStorage.getItem('qywx_xuegong_token') || '';
    var tenant = localStorage.getItem('qywx_xuegong_tenant') || 'MDAwMA==';
    if (!token) return JSON.stringify({error: 'auth'});

    function setHeaders(xhr) {
      xhr.setRequestHeader('Content-Type', 'application/json');
      xhr.setRequestHeader('Authorization', 'Basic bGFtcF93ZWI6bGFtcF93ZWJfc2VjcmV0');
      xhr.setRequestHeader('tenant', tenant);
      xhr.setRequestHeader('token', token);
    }

    function itemListFrom(data) {
      if (!data) return null;
      if (data.itemList) return data;
      if (data.data && data.data.itemList) return data.data;
      return null;
    }

    function fetchInfo() {
      var attempts = [
        {method: 'GET', url: '/sms/api/studentAffairs/info'},
        {method: 'POST', url: '/sms/api/user/getLoginUserDetail', body: '{}'},
        {method: 'POST', url: '/sms/api/user/getLoginUserDetail',
          body: '{"full":true,"org":true,"resource":true,"roles":true,"station":true}'}
      ];
      for (var i = 0; i < attempts.length; i++) {
        try {
          var a = attempts[i];
          var xhr = new XMLHttpRequest();
          xhr.open(a.method, a.url, false);
          setHeaders(xhr);
          xhr.send(a.body || null);
          if (xhr.status === 401) return {__error: 'auth'};
          if (xhr.status !== 200) continue;
          var body = JSON.parse(xhr.responseText || '{}');
          if (body && body.code === 401) return {__error: 'auth'};
          var data = itemListFrom(body && body.data ? body.data : body);
          if (data) {
            sessionStorage.setItem('studentInfo', JSON.stringify(data));
            return data;
          }
        } catch (ignored) {}
      }
      return null;
    }

    var info = sessionStorage.getItem('studentInfo');
    var infoObj = null;
    if (info) {
      try { infoObj = JSON.parse(info); } catch (ignored) {}
    }
    if (!infoObj || !infoObj.itemList) {
      var fetched = fetchInfo();
      if (fetched && fetched.__error) return JSON.stringify({error: fetched.__error});
      infoObj = fetched;
    }

    var code = '';
    if (infoObj && infoObj.itemList) {
      for (var j = 0; j < infoObj.itemList.length; j++) {
        if (infoObj.itemList[j] && infoObj.itemList[j].title === '学号') {
          code = infoObj.itemList[j].value || '';
        }
      }
    }
    if (!code && document.body) {
      var m = (document.body.innerText || '').match(/学号[：:]\s*([0-9]{6,})/);
      if (m) code = m[1];
    }
    if (!code) return JSON.stringify({error: 'student'});

    function post(api, appCode) {
      var xhr = new XMLHttpRequest();
      xhr.open('POST', '/sms/api/studentAffairs/' + api, false);
      setHeaders(xhr);
      xhr.send(JSON.stringify({code: code, appCode: appCode}));
      if (xhr.status === 401) return {__error: 'auth'};
      if (xhr.status !== 200) return {__error: 'http', status: xhr.status};
      var body = JSON.parse(xhr.responseText || '{}');
      if (!body || body.code !== 0) {
        return {__error: 'api', code: body && body.code, msg: (body && body.msg) || ''};
      }
      return body.data || [];
    }

    var out = {};
    var calls = [
      ['psy', 'getPsyActivity', 'psyactivity'],
      ['labor', 'getPsyActivity2', 'psyactivity2'],
      ['ie', 'getPsyActivity3', 'psyactivity3']
    ];
    for (var c = 0; c < calls.length; c++) {
      var value = post(calls[c][1], calls[c][2]);
      if (value && value.__error) {
        return JSON.stringify({
          error: value.__error,
          detail: value.msg || '',
          status: value.status || 0
        });
      }
      out[calls[c][0]] = value;
    }
    return JSON.stringify(out);
  } catch (e) {
    return JSON.stringify({error: 'script', kind: e.name || 'Error'});
  }
})()
