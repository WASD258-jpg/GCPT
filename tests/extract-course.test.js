const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const source = fs.readFileSync('assets/extract-course.js', 'utf8');
function cell(text, rowSpan=1, colSpan=1, status=false) {
  return {innerText:text,rowSpan,colSpan,getClientRects:()=>[{}],querySelectorAll:sel=>sel==='*'&&status?[{red:true}]:[]};
}
function table(rows,visible=true) {
  return {rows:rows.map(cells=>({cells})),innerText:rows.flat().map(c=>c.innerText).join('\n'),getClientRects:()=>visible?[{}]:[],contains:()=>false};
}
function run(tables,body='课表') {
  return vm.runInNewContext(source, {document:{body:{innerText:body},querySelectorAll:s=>s==='table'?tables:[]},getComputedStyle:n=>({color:n.red?'rgb(255, 0, 0)':'rgb(0, 0, 255)',fontStyle:n.red?'italic':'normal'})});
}
let headers=[cell('时间段'),cell('节次'),...['一','二','三','四','五','六','日'].map(d=>cell('星期'+d))];
let a=cell('课程甲\n(1-2节)1-5周\n楼A101',2),b=cell('课程乙\n(1-2节)2-6周\n楼B102',2);
let first=[cell('上午',2),cell('1'),a,b,...Array.from({length:5},()=>cell(''))];
let second=[cell('2'),...Array.from({length:5},()=>cell(''))];
let other=cell('其它课程：体育甲/2-8周/无;',1,9);
let original=table([headers,first,second,[other]]);
let r=run([original]);
assert.equal(r.cells.length,2);assert.equal(r.cells[0].day,1);assert.equal(r.cells[1].day,2);
assert.equal(r.pending.length,1);assert.equal(r.matchedBlocks,2);
let hidden=table([headers,[cell('隐藏课程\n(1-8节)1-20周',1,9)]],false);
assert.equal(run([hidden,original]).cells.length,2);
assert.equal(run([], '系统升级维护中').error,'maintenance');
assert.equal(run([]).error,'notready');
let red=cell('待选课程\n(1-2节)1-5周',1,1,true);
assert.equal(run([table([headers,[cell('上午'),cell('1'),red]])]).cells[0].status,'pending');
let ambiguous=cell('无法定位\n(1-2节)1-5周',1,2);
assert.equal(run([table([headers,[cell('上午'),cell('1'),ambiguous]])]).cells.length,0);
console.log('PASS: 10 DOM extraction assertions (rowspan, weekday, hidden tables, maintenance, pending selection, ambiguous colspan)');
