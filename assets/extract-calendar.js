(function () {
  try {
    var selects = document.querySelectorAll('select');
    for (var i = 0; i < selects.length; i++) {
      var select = selects[i];
      var option = select.options[select.selectedIndex];
      var text = option ? (option.text || '') : '';
      var match = text.match(/(\d{1,2})\s*[\(（]\s*(\d{4})-(\d{2})-(\d{2})\s*至\s*(\d{4})-(\d{2})-(\d{2})/);
      if (match) {
        return JSON.stringify({
          week: Number(match[1]),
          start: match[2] + '-' + match[3] + '-' + match[4],
          end: match[5] + '-' + match[6] + '-' + match[7]
        });
      }
    }
    return JSON.stringify({error: 'notready', selects: selects.length});
  } catch (e) {
    return JSON.stringify({error: 'script', kind: e.name || 'Error'});
  }
})()
