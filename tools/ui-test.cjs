// Run: npm install --no-save --prefix .cache/ui-test jsdom@26
// NODE_PATH="$PWD/.cache/ui-test/node_modules" node tools/ui-test.cjs
const { JSDOM } = require('jsdom');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const path = 'app/src/main/assets/';
const dom = new JSDOM(fs.readFileSync(path+'index.html','utf8'), {runScripts:'outside-only',url:'https://appassets.androidplatform.net/assets/index.html'});
const w=dom.window, d=w.document;
let data=[
 {id:'folder',title:'Akademik',folder:true,favorite:true,modified:1,date:'Bugün'},
 {id:'note',title:'<img src=x onerror=alert(1)>',coverTitle:'Kentsel\nEskizler',coverSubtitle:'Serisi',tag:'CİLT I',paper:'Noktalı',description:'Ahşap',content:'Metin',favorite:true,color:0,pages:1,modified:2,date:'Bugün'},
 {id:'trash',title:'Silinmiş',coverTitle:'Silinmiş',deleted:true,modified:3,date:'Bugün'}
];
w.Atelier={postMessage(raw){
 const r=JSON.parse(raw),a=r.args;let result=true;
 switch(r.action){
 case 'list':result=data;break;
 case 'get':result=data.find(b=>b.id===a.id);break;
 case 'save':Object.assign(data.find(b=>b.id===a.id),a);break;
 case 'favorite':{const b=data.find(b=>b.id===a.id);b.favorite=!b.favorite;break;}
 case 'trash':data.find(b=>b.id===a.id).deleted=true;break;
 case 'restore':data.find(b=>b.id===a.id).deleted=false;break;
 }
 queueMicrotask(()=>w.Atelier.onmessage({data:JSON.stringify({requestId:r.requestId,data:result})}));
}};
w.confirm=()=>true;
w.HTMLCanvasElement.prototype.getContext=function(){return new Proxy({}, {get:(target,key)=>target[key] || (()=>{}),set:(target,key,value)=>{target[key]=value;return true;}});};
w.HTMLCanvasElement.prototype.setPointerCapture=function(){};
w.eval(fs.readFileSync(path+'studio.js','utf8'));
w.eval(fs.readFileSync(path+'app.js','utf8'));
const tick=()=>new Promise(r=>setTimeout(r,10));
(async()=>{
 await tick();assert.equal(d.querySelectorAll('.card').length,2,'trash excluded');
 assert.equal(d.querySelector('.card-title').textContent,data[1].title,'user HTML is text, never markup');
 assert.equal(d.querySelectorAll('#gallery img').length,0,'no injected HTML');
 d.querySelector('[data-filter="1"]').click();assert.equal(d.querySelectorAll('.card').length,1);
 d.querySelector('[data-filter="0"]').click();
 d.querySelector('[data-fav="note"]').click();await tick();assert.equal(data[1].favorite,false);
 d.querySelector('[data-open="note"]').click();await tick();assert.equal(d.getElementById('editor').hidden,false);
 d.getElementById('noteBody').value='Düzenlenen not';d.getElementById('noteBody').dispatchEvent(new w.Event('input'));
 d.getElementById('save').click();await tick();assert.equal(data[1].content,'Düzenlenen not');
 // Page state, undo and paper configuration are real document changes.
 d.getElementById('addPage').click();
 assert.equal(w.InkStudio.document().pages.length,2);
 d.getElementById('starPage').click();
 assert.equal(w.InkStudio.document().pages[1].starred,true);
 d.getElementById('prevPage').click();
 assert.equal(d.getElementById('pageNumber').textContent,'Sayfa 1 / 2');
 d.querySelector('[data-template="cornell"]').click();
 d.querySelector('[data-paper="slate"]').click();
 d.querySelector('[data-spacing="28"]').click();
 d.querySelector('[data-orientation="landscape"]').click();
 const config=JSON.parse(w.TemplateStudio.document());
 assert.equal(config.template,'cornell');assert.equal(config.paperColor,'slate');assert.equal(config.spacing,28);assert.equal(config.orientation,'landscape');
 d.getElementById('save').click();await tick();
 assert.equal(JSON.parse(data[1].document).pages.length,2);
 d.getElementById('delete').click();await tick();assert.equal(data[1].deleted,true);assert.equal(d.getElementById('editor').hidden,true);
 d.querySelector('[data-section="2"]').click();assert.equal(d.querySelectorAll('.card').length,2);
 d.querySelector('[data-open="note"]').click();await tick();assert.equal(d.getElementById('noteBody').readOnly,true);
 d.getElementById('restore').click();await tick();assert.equal(data[1].deleted,false);
 console.log('PASS: rendering, HTML escaping, filters, favorites, editor, save, trash, restore');
 dom.window.close();
})().catch(e=>{console.error(e);dom.window.close();process.exitCode=1;});
