'use strict';
const $ = id => document.getElementById(id);
const paths = {
 edit:'M4 5h12M4 10h8M4 15h5m4 5 1-5 6-6 3 3-6 6-4 2Z',
 folder:'M3 7V4h6l2 3h10v3M3 9h19l-4 11H2L3 9Z',
 foldercopy:'M8 7h13v13H8zM4 16H2V3h13v2',
 bookmark:'M6 3h12v18l-6-4-6 4V3Zm3 5 3 3 3-3',
 trash:'M3 6h18M9 6V3h6v3M6 6l1 15h10l1-15M10 10v7m4-7v7',
 cloud:'M7 18H5a4 4 0 0 1-1-8 7 7 0 0 1 13-3 5 5 0 0 1 1 11h-4m-6-4 3 3 6-7',
 pen:'m3 19 4-1L19 6l-3-3L4 15l-1 4Zm8-12-3 3M8 3 3 8m14 8 4-4M12 21l6-6',
 pencil:'m4 16-1 5 5-1L21 7l-4-4L4 16Zm9-9 4 4M4 16l4 4',
 menu:'M3 6h18M3 12h18M3 18h18',search:'M10 17a7 7 0 1 0 0-14 7 7 0 0 0 0 14Zm5-2 6 6',
 tune:'M4 4v4m0 4v8M1 8h6M12 4v10m0 4v2m-3-6h6M20 4v2m0 4v10m-3-14h6',
 user:'M8 7a4 4 0 1 0 8 0 4 4 0 0 0-8 0ZM4 21v-3c0-5 16-5 16 0v3H4Z',
 sort:'m3 6 4-4 4 4M7 2v17m6-1 4 4 4-4m-4 4V5',
 star:'m12 2 3 6 7 1-5 5 1 7-6-3-6 3 1-7-5-5 7-1 3-6Z',
 arrow:'M4 12h16m-6-6 6 6-6 6',back:'M20 12H4m6-6-6 6 6 6',
 plus:'M12 3v18M3 12h18',camera:'M3 7h4l2-3h6l2 3h4v14H3V7Zm9 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z'
};
function icon(name){return `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="${paths[name] || paths.edit}"/></svg>`;}
document.querySelectorAll('[data-icon]').forEach(el => el.innerHTML = icon(el.dataset.icon));
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let seq=0;const pending=new Map();
function call(action,args={}){
 return new Promise((resolve,reject)=>{
  if(!window.Atelier){reject(new Error('Odağı Android uygulamasında açın. Kotlin motoru bulunamadı.'));return;}
  const requestId=++seq;const timer=setTimeout(()=>{pending.delete(requestId);reject(new Error('İşlem zaman aşımına uğradı'));},15000);
  pending.set(requestId,{resolve,reject,timer});Atelier.postMessage(JSON.stringify({requestId,action,args}));
 });
}
if(window.Atelier) Atelier.onmessage=event=>{const r=JSON.parse(event.data),p=pending.get(r.requestId);if(!p)return;clearTimeout(p.timer);pending.delete(r.requestId);r.error?p.reject(new Error(r.error)):p.resolve(r.data);};
window.showError=message=>{ $('toast').textContent=String(message);$('toast').hidden=false;clearTimeout(window.toastTimer);window.toastTimer=setTimeout(()=>$('toast').hidden=true,4000); };
function fail(e){showError(e.message || e);}
let books=[],section=0,filter=0,alpha=false,favorites=false,parentId='',search='',limit=40,loadVersion=0;
let current=null,dirty=false,saveTimer,saveChain=Promise.resolve(),saving=0,revision=0;
async function load(){const version=++loadVersion;try{const result=await call('list',{search});if(version!==loadVersion)return;books=result;render();}catch(e){fail(e);}}
function render(){
 document.querySelectorAll('[data-section]').forEach(b=>b.classList.toggle('active',Number(b.dataset.section)===section));
 document.querySelectorAll('[data-filter]').forEach(b=>b.classList.toggle('selected',Number(b.dataset.filter)===filter));
 $('heading').textContent=parentId?(books.find(b=>b.id===parentId)?.title || 'Klasör'):['Kişisel Kitaplık','Favoriler','Çöp Kutusu'][section];
 $('subtitle').textContent=section===2?'Silinen notlarınızı geri yükleyin':`${books.filter(b=>!b.deleted&&!b.folder).length} defter, ${books.filter(b=>!b.deleted&&b.folder).length} akıllı klasör · Cihazda kaydedildi`;
 $('parent').hidden=!parentId;
 let rows=books.filter(b=>(section===2?b.deleted:!b.deleted)&&(section!==1||b.favorite)&&(!favorites||b.favorite)&&(!filter||(filter===1?b.folder:!b.folder))&&(!parentId||b.parentId===parentId));
 rows.sort(alpha?(a,b)=>a.title.localeCompare(b.title,'tr'):(a,b)=>b.modified-a.modified);
 $('gallery').innerHTML=rows.length?rows.slice(0,limit).map(card).join(''):'<div class="empty"><h2>Henüz bir not yok</h2><p>Yeni bir defterle düşüncelerinize yer açın.</p></div>';
 $('more').hidden=rows.length<=limit;
}
function card(b){
 const fav=`<button class="fav" data-fav="${esc(b.id)}" aria-label="${b.favorite?'Favorilerden çıkar':'Favoriye ekle'}" aria-pressed="${b.favorite}">${icon('star')}</button>`;
 const foot=`<div class="card-foot"><span>${esc(b.date)}</span>${b.folder?icon('arrow'):`<span class="status">${b.deleted?'Silindi':'• Cihazda'}</span>`}</div>`;
 if(b.folder)return `<article class="card folder-card" tabindex="0" role="button" aria-label="${esc(b.title)}" data-open="${esc(b.id)}"><div class="folder-top"><span class="folder-symbol">${icon('foldercopy')}</span>${fav}</div><div><div class="folder-meta"><span class="badge">Akademik</span><span>${books.filter(n=>n.parentId===b.id&&!n.deleted).length} Not Defteri</span></div><h3 class="ellipsis">${esc(b.title)}</h3><p class="desc ellipsis">${esc(b.description)}</p></div>${foot}</article>`;
 return `<article class="card" tabindex="0" role="button" aria-label="${esc(b.title)}" data-open="${esc(b.id)}"><div class="cover color${Number(b.color)||0}"><div class="cover-top"><span class="tag">${esc(b.tag)}</span>${fav}</div><div class="cover-label"><h3>${esc(b.coverTitle)}</h3><p>${esc(b.coverSubtitle)}</p></div><div class="cover-bottom"><span>${esc(b.paper)}</span><span>${Number(b.pages)} Sayfa</span></div></div><div class="card-body"><div class="card-title ellipsis">${esc(b.title)}</div><p class="desc ellipsis">${esc(b.description)}</p>${foot}</div></article>`;
}
function drawer(open){$('sidebar').classList.toggle('open',open);$('shade').hidden=!open;}
$('menu').onclick=()=>drawer(true);$('shade').onclick=()=>drawer(false);
document.querySelectorAll('[data-section]').forEach(b=>b.onclick=()=>{section=Number(b.dataset.section);parentId='';limit=40;drawer(false);render();});
document.querySelectorAll('[data-filter]').forEach(b=>b.onclick=()=>{filter=Number(b.dataset.filter);limit=40;render();});
$('filter').onclick=()=>{favorites=!favorites;$('filter').setAttribute('aria-pressed',favorites);$('filter').style.color=favorites?'#4b6151':'';render();};
$('sort').onclick=()=>{alpha=!alpha;$('sort').querySelector('span').textContent=alpha?'Ada göre':'Son Değiştirilme';render();};
$('parent').onclick=()=>{parentId='';render();};$('more').onclick=()=>{limit+=40;render();};
let searchTimer;$('search').oninput=e=>{clearTimeout(searchTimer);searchTimer=setTimeout(()=>{search=e.target.value;limit=40;load();},220);};
$('gallery').onclick=async e=>{const f=e.target.closest('[data-fav]');if(f){try{await call('favorite',{id:f.dataset.fav});await load();}catch(err){fail(err);}return;}const c=e.target.closest('[data-open]');if(c)open(c.dataset.open).catch(fail);};
$('gallery').onkeydown=e=>{if(e.target.matches('[data-open]')&&(e.key==='Enter'||e.key===' ')){e.preventDefault();open(e.target.dataset.open).catch(fail);}};
let createFolder=false,color=0;
function type(folder){createFolder=folder;$('typeFolder').classList.toggle('selected',folder);$('typeNote').classList.toggle('selected',!folder);$('colors').hidden=folder;$('name').placeholder=folder?'Klasör adı':'Defter adı';}
$('typeFolder').onclick=()=>type(true);$('typeNote').onclick=()=>type(false);
$('new').onclick=()=>{$('createForm').reset();type(false);$('create').showModal();};$('cancel').onclick=()=>$('create').close();
document.querySelectorAll('[data-color]').forEach(b=>b.onclick=()=>{color=Number(b.dataset.color);document.querySelectorAll('[data-color]').forEach(c=>c.classList.toggle('chosen',c===b));});
$('createForm').onsubmit=async e=>{e.preventDefault();const submit=e.submitter;submit.disabled=true;try{const b=await call('create',{title:$('name').value,folder:createFolder,color,parentId});$('create').close();await load();if(!b.folder)await open(b.id);}catch(err){fail(err);}finally{submit.disabled=false;}};
async function quick(){try{const b=await call('create',{title:'Hızlı Not',parentId});await load();await open(b.id);}catch(e){fail(e);}}
$('quick').onclick=quick;$('write').onclick=quick;$('photo').onclick=()=>call('photo').catch(fail);
window.photoAdded=id=>{load();open(id).catch(fail);};
$('profile').onclick=()=>$('info').showModal();$('closeInfo').onclick=()=>$('info').close();
async function open(id){
 const b=await call('get',{id});
 if(b.folder&&!b.deleted){parentId=id;render();return;}
 current=b;dirty=false;revision=0;$('noteTitle').value=b.title;$('noteBody').value=b.content;$('paperType').textContent=`${b.paper} · ${b.pages} sayfa`;
 $('noteTitle').readOnly=b.deleted;$('noteBody').readOnly=b.deleted;$('save').hidden=b.deleted;$('restore').hidden=!b.deleted;
 $('noteImage').hidden=!b.imageUri;
 if(b.imageUri?.startsWith('data:image/'))$('noteImage').src=b.imageUri;else{$('noteImage').removeAttribute('src');$('noteImage').hidden=true;}
 $('saveStatus').textContent=b.deleted?'Silindi':'Cihazda kaydedildi';$('editor').hidden=false;$('editor').scrollTop=0;
}
function changed(){if(!current||current.deleted)return;dirty=true;revision++;$('saveStatus').textContent='Kaydediliyor…';clearTimeout(saveTimer);saveTimer=setTimeout(()=>save().catch(fail),700);}
$('noteTitle').oninput=changed;$('noteBody').oninput=changed;
function save(){
 clearTimeout(saveTimer);if(!current||!dirty||current.deleted)return saveChain;
 const rev=revision,args={id:current.id,title:$('noteTitle').value,content:$('noteBody').value};dirty=false;saving++;
 const operation=saveChain.catch(()=>{}).then(()=>call('save',args)).then(()=>{if(current?.id===args.id&&revision===rev)$('saveStatus').textContent='Kaydedildi';}).catch(e=>{if(current?.id===args.id){dirty=true;$('saveStatus').textContent='Kayıt başarısız';}throw e;}).finally(()=>saving--);
 saveChain=operation;return operation;
}
async function closeEditor(){try{await save();$('editor').hidden=true;current=null;$('noteImage').removeAttribute('src');await load();}catch(e){fail(e);}}
$('back').onclick=closeEditor;$('save').onclick=()=>save().catch(fail);
$('delete').onclick=async()=>{if(!current)return;if(current.deleted&&!confirm('Kalıcı olarak silinsin mi? Bu işlem geri alınamaz.'))return;try{await save();await call(current.deleted?'remove':'trash',{id:current.id});current=null;$('editor').hidden=true;await load();}catch(e){fail(e);}};
$('restore').onclick=async()=>{try{await call('restore',{id:current.id});current=null;$('editor').hidden=true;await load();}catch(e){fail(e);}};
document.addEventListener('visibilitychange',()=>{if(document.hidden)save().catch(fail);});
window.goBack=()=>{const dialog=document.querySelector('dialog[open]');if(dialog){dialog.close();return true;}if(!$('editor').hidden){closeEditor();return true;}if($('sidebar').classList.contains('open')){drawer(false);return true;}if(parentId){parentId='';render();return true;}return false;};
load();

window.flushNote=()=>{save().catch(fail);return true;};
