(function () {
  try {
    var body = document.body ? document.body.innerText || '' : '';
    if (body.indexOf('系统升级维护中') >= 0) return {error:'maintenance'};
    var tables = document.querySelectorAll('table'), best = null, score = 0;
    for (var t = 0; t < tables.length; t++) {
      if (!tables[t].getClientRects().length) continue;
      var tx = tables[t].innerText || '';
      if (tx.indexOf('星期一') < 0 || tx.indexOf('星期日') < 0) continue;
      var n = (tx.match(/[（(]\s*\d+\s*[-－~～]?\s*\d*\s*节/g) || []).length;
      // Prefer the innermost table when an outer layout table contains the same courses.
      if (n > 0 && (n > score || n === score && best && best.contains(tables[t]))) {best=tables[t];score=n;}
    }
    if (!best) return {error:'notready',tables:tables.length};
    var grid=[], entries=[], columns={}, days=['星期一','星期二','星期三','星期四','星期五','星期六','星期日'];
    for (var r=0;r<best.rows.length;r++) {
      if (!grid[r]) grid[r]=[];
      var cells=best.rows[r].cells,c=0;
      for(var k=0;k<cells.length;k++) {
        while(grid[r][c])c++;
        var cell=cells[k],rs=Math.max(1,cell.rowSpan||1),cs=Math.max(1,cell.colSpan||1);
        entries.push({cell:cell,col:c,row:r,span:cs});
        var label=(cell.innerText||'').replace(/\s/g,'');
        for(var d=0;d<days.length;d++)if(label===days[d]&&cs===1)columns[c]=d+1;
        for(var rr=r;rr<r+rs;rr++){if(!grid[rr])grid[rr]=[];for(var cc=c;cc<c+cs;cc++)grid[rr][cc]=true;}
        c+=cs;
      }
    }
    var out=[],pending=[],seen=[];
    for(var e=0;e<entries.length;e++) {
      var entry=entries[e],el=entry.cell,text=el.innerText||'';
      if(text.indexOf('其它课程')>=0||text.indexOf('其他课程')>=0){pending.push(text);continue;}
      if(!/[（(]\s*\d+[^）)]*节/.test(text))continue;
      if(entry.span!==1||!columns[entry.col]){pending.push('无法确认星期：'+text);continue;}
      var blocks=el.querySelectorAll('.timetable_con');
      if(!blocks.length)blocks=[el];
      for(var b=0;b<blocks.length;b++) {
        if(!blocks[b].getClientRects().length)continue;
        var bt=blocks[b].innerText||'';
        if(!/[（(]\s*\d+[^）)]*节/.test(bt))continue;
        // Red italic entries are explicitly labelled as pending selection by this school's page.
        var nodes=blocks[b].querySelectorAll('*'),unselected=false;
        for(var z=0;z<nodes.length;z++) {
          var style=getComputedStyle(nodes[z]),rgb=style.color.match(/\d+/g);
          if(style.fontStyle==='italic'&&rgb&&+rgb[0]>150&&+rgb[1]<110&&+rgb[2]<110)unselected=true;
        }
        out.push({day:columns[entry.col],text:bt,status:unselected?'pending':'page'});
      }
    }
    var terms=[],ss=document.querySelectorAll('select');
    for(var j=0;j<ss.length;j++){var s=ss[j];if(/^(xnm|xqm)$/.test(s.id||s.name)&&s.options[s.selectedIndex])terms.push(s.options[s.selectedIndex].text);}
    return {term:terms.join(' / '),cells:out,pending:pending,matchedBlocks:score};
  }catch(e){return {error:'script',kind:e.name||'Error'};}
})()
