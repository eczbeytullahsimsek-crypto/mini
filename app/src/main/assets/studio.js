'use strict';
// Vector strokes, not full-resolution bitmap history. Only the visible page has a canvas.
(()=>{
 const $=id=>document.getElementById(id);
 const names={grid:'Kareli',lined:'Çizgili',blank:'Düz Kağıt',cornell:'Cornell',dotted:'Noktalı',planner:'Haftalık Plan'};
 const colors={white:['#ffffff','#1d1b18','#00000018'],cream:['#faf7f2','#1d1b18','#5c524620'],sand:['#f3efea','#241a09','#685a4426'],slate:['#2c302e','#f6f0ea','#ffffff24']};
 const copy=o=>JSON.parse(JSON.stringify(o));
 const defaults=()=>({version:1,template:'grid',paperColor:'white',spacing:20,orientation:'portrait',size:'standard',pages:[{strokes:[],starred:false}]});
 const label=d=>`${names[d.template]} · ${d.spacing===14?'5':d.spacing===28?'10':'7'}mm`;
 function decorate(el,d){const c=colors[d.paperColor]||colors.white;el.dataset.pattern=d.template;el.style.setProperty('--paper',c[0]);el.style.setProperty('--paper-ink',c[1]);el.style.setProperty('--rule',c[2]);el.style.setProperty('--space',d.spacing+'px');}
 let template=defaults();
 window.TemplateStudio={
  init(){
   document.querySelectorAll('[data-template]').forEach(b=>b.onclick=()=>{template.template=b.dataset.template;this.update();});
   document.querySelectorAll('[data-paper]').forEach(b=>b.onclick=()=>{template.paperColor=b.dataset.paper;this.update();});
   document.querySelectorAll('[data-spacing]').forEach(b=>b.onclick=()=>{template.spacing=Number(b.dataset.spacing);this.update();});
   document.querySelectorAll('[data-orientation]').forEach(b=>b.onclick=()=>{template.orientation=b.dataset.orientation;this.update();});
   document.querySelectorAll('[data-size]').forEach(b=>b.onclick=()=>{template.size=b.dataset.size;this.update();});
   $('name').addEventListener('input',()=>{$('previewTitle').textContent=$('name').value||'Not Defteri Başlığı…';});
   this.update();
  },
  update(){
   [['template',template.template],['paper',template.paperColor],['spacing',String(template.spacing)],['orientation',template.orientation],['size',template.size]].forEach(([attr,val])=>document.querySelectorAll(`[data-${attr}]`).forEach(b=>{b.classList.toggle('selected',b.dataset[attr]===val);b.setAttribute('aria-pressed',String(b.dataset[attr]===val));}));
   decorate($('previewSheet'),template);$('previewFrame').classList.toggle('landscape',template.orientation==='landscape');
   $('previewFrame').classList.toggle('a4',template.size==='a4');$('previewBadge').textContent=label(template);$('spacingLabel').textContent=template.spacing===14?'Dar (5mm)':template.spacing===28?'Geniş (10mm)':'Standart (7mm)';
  },
  document(){return JSON.stringify(template);},
  paper(){return label(template)+' · '+({white:'Doğal Beyaz',cream:'Fildişi',sand:'Sıcak Kum',slate:'Koyu Arduvaz'}[template.paperColor]);}
 };
 let doc=defaults(),pageIndex=0,bookId='',readonly=false,changed=()=>{},ctx,canvas,tool='pen',ink='#2c302e',pen='fountain',width=.5,opacity=1,zoom=1,active=null,undo=[],redo=[],selection=[],lasso=[],moving=null,frame=0,bgImage=null,bgVersion=0;
 const W=1000;let H=1380;
 const page=()=>doc.pages[pageIndex];
 function remember(){undo.push(page().strokes.slice());if(undo.length>25)undo.shift();redo=[];buttons();}
 function buttons(){$('undo').disabled=readonly||!undo.length;$('redo').disabled=readonly||!redo.length;$('starPage').setAttribute('aria-pressed',!!page().starred);$('starPage').classList.toggle('selected',!!page().starred);}
 function dirty(){buttons();changed();}
 function renderStroke(c,s){
  if(!s.points?.length)return;if(s.text){c.save();c.fillStyle=s.color;c.font=`${s.size||16}px ${s.serif?'Newsreader':'Manrope'}`;c.fillText(s.text,s.points[0].x,s.points[0].y);c.restore();return;}c.save();c.lineCap='round';c.lineJoin='round';c.strokeStyle=s.color;c.fillStyle=s.color;c.globalAlpha=s.opacity;
  c.globalCompositeOperation=s.tool==='eraser'?'destination-out':'source-over';
  const pts=s.points;
  if(pts.length===1){c.beginPath();c.arc(pts[0].x,pts[0].y,s.width/2,0,Math.PI*2);c.fill();}
  else for(let i=1;i<pts.length;i++){c.lineWidth=s.width*(s.pen==='brush'?(.4+(pts[i].p||.5)*1.2):s.pen==='fountain'?(.7+(pts[i].p||.5)*.6):1);c.beginPath();c.moveTo(pts[i-1].x,pts[i-1].y);c.lineTo(pts[i].x,pts[i].y);c.stroke();}
  c.restore();
 }
 function redraw(){if(!ctx)return;ctx.clearRect(0,0,W,H);for(const s of page().strokes)renderStroke(ctx,s);if(active&&active.tool!=='lasso')renderStroke(ctx,active);overlay();}
 function schedule(){if(frame)return;frame=requestAnimationFrame(()=>{frame=0;redraw();});}
 function bounds(){const pts=selection.flatMap(i=>page().strokes[i]?.points||[]);if(!pts.length)return null;return pts.reduce((b,p)=>({x:Math.min(b.x,p.x),y:Math.min(b.y,p.y),right:Math.max(b.right,p.x),bottom:Math.max(b.bottom,p.y)}),{x:Infinity,y:Infinity,right:-Infinity,bottom:-Infinity});}
 function overlay(){ctx.save();ctx.strokeStyle='#637a69';ctx.lineWidth=1.5;ctx.setLineDash([7,5]);
  if(lasso.length){ctx.beginPath();lasso.forEach((p,i)=>i?ctx.lineTo(p.x,p.y):ctx.moveTo(p.x,p.y));ctx.stroke();}
  const b=bounds();if(b)ctx.strokeRect(b.x-10,b.y-10,b.right-b.x+20,b.bottom-b.y+20);ctx.restore();
 }
 function point(e){const r=canvas.getBoundingClientRect();return {x:(e.clientX-r.left)*W/r.width,y:(e.clientY-r.top)*H/r.height,p:e.pressure||.5};}
 function inPolygon(p,polygon){let inside=false;for(let i=0,j=polygon.length-1;i<polygon.length;j=i++){const a=polygon[i],b=polygon[j];if(((a.y>p.y)!==(b.y>p.y))&&(p.x<(b.x-a.x)*(p.y-a.y)/(b.y-a.y)+a.x))inside=!inside;}return inside;}
 function down(e){
  if(readonly||tool==='text'||e.button>0||active||moving)return;e.preventDefault();canvas.setPointerCapture(e.pointerId);const p=point(e);
  if(tool==='lasso'){
   const b=bounds();if(b&&p.x>b.x-10&&p.x<b.right+10&&p.y>b.y-10&&p.y<b.bottom+10){remember();moving={start:p,original:page().strokes.slice()};return;}
   selection=[];lasso=[p];active={tool:'lasso',pointer:e.pointerId};return;
  }
  remember();active={tool:tool==='shapes'?'pen':tool,color:ink,pen,width:width*5.5*(tool==='eraser'?7:tool==='highlighter'?6:1),opacity:opacity*(tool==='highlighter'?.28:1),points:[p],pointer:e.pointerId,straight:tool==='shapes'};
  if(!active.straight)renderStroke(ctx,active);
 }
 function move(e){
  if(!active&&!moving)return;e.preventDefault();
  if(moving){const p=point(e),dx=p.x-moving.start.x,dy=p.y-moving.start.y;page().strokes=moving.original.map((s,i)=>selection.includes(i)?{...s,points:s.points.map(p=>({...p,x:p.x+dx,y:p.y+dy}))}:s);schedule();return;}
  if(active.pointer!==e.pointerId)return;
  if(active.tool==='lasso'){lasso.push(point(e));schedule();return;}
  if(active.straight){active.points=[active.points[0],point(e)];schedule();return;}
  for(const event of e.getCoalescedEvents?.()||[e]){
   const p=point(event),last=active.points.at(-1);if(Math.hypot(p.x-last.x,p.y-last.y)<.8)continue;
   if(active.points.length>=20000)break;
   active.points.push(p);renderStroke(ctx,{...active,points:[last,p]});
  }
 }
 function up(e){
  if(moving){moving=null;redraw();dirty();return;}
  if(!active||active.pointer!==e.pointerId)return;
  if(active.tool==='lasso'){selection=page().strokes.map((s,i)=>s.tool!=='eraser'&&s.points.some(p=>inPolygon(p,lasso))?i:-1).filter(i=>i>=0);lasso=[];active=null;redraw();return;}
  const {pointer,straight,...stroke}=active;page().strokes.push(stroke);active=null;redraw();dirty();
 }
 function dimensions(){const ratio=doc.size==='a4'?Math.SQRT2:1.38;H=doc.orientation==='landscape'?Math.round(W/ratio):Math.round(W*ratio);const dpr=Math.min(window.devicePixelRatio||1,1.5);canvas.width=W*dpr;canvas.height=H*dpr;ctx=canvas.getContext('2d');ctx.scale(dpr,dpr);$('drawingPage').style.aspectRatio=`${W}/${H}`;applyZoom();}
 function applyZoom(){$('drawingPage').style.width=`${zoom*100}%`;$('zoomReset').textContent=Math.round(zoom*100)+'%';}
 function config(){decorate($('paperBackground'),doc);$('paperBackground').classList.toggle('cornell',doc.template==='cornell');$('cycleTemplate').textContent=label(doc);$('paperType').textContent=label(doc);}
 async function loadBackground(){const token=++bgVersion;bgImage=null;$('paperBackground').style.backgroundImage='';$('paperBackground').classList.remove('pdf-background');
  if(page().pdfPage!==undefined){try{const url=await window.studioCall('pdfPage',{id:bookId,page:page().pdfPage});if(token!==bgVersion)return;const image=new Image();image.onload=()=>{if(token===bgVersion){bgImage=image;$('paperBackground').style.backgroundImage=`url("${url}")`;$('paperBackground').classList.add('pdf-background');}};image.src=url;}catch(e){showError(e.message);}}
  else $('paperBackground').classList.remove('pdf-background');
 }
 function showPage(){active=null;moving=null;selection=[];lasso=[];undo=[];redo=[];dimensions();config();redraw();buttons();$('pageNumber').textContent=`Sayfa ${pageIndex+1} / ${doc.pages.length}`;$('prevPage').disabled=pageIndex===0;$('nextPage').disabled=pageIndex===doc.pages.length-1;$('pageShelf').textContent=pageIndex<doc.pages.length-1?`Sayfa ${pageIndex+2} →`:'Yeni sayfa ekle +';loadBackground();}
 function selectTool(t){tool=t;document.querySelectorAll('[data-tool]').forEach(b=>{b.classList.toggle('selected',b.dataset.tool===t);b.setAttribute('aria-pressed',b.dataset.tool===t);});$('textPanel').hidden=t!=='text';$('drawingPage').hidden=t==='text';$('pageShelf').parentElement.hidden=t==='text';if(t==='text')$('noteBody').focus();}
 function changePage(delta){const next=pageIndex+delta;if(next<0||next>=doc.pages.length)return;pageIndex=next;showPage();$('canvasScroll').scrollTop=0;}
 function newPage(){if(readonly)return;if(doc.pages.length>=200){showError('Bir defter en fazla 200 sayfa olabilir');return;}doc.pages.push({strokes:[],starred:false});pageIndex=doc.pages.length-1;showPage();dirty();}
 function drawPaper(c){const palette=colors[doc.paperColor]||colors.white;c.fillStyle=palette[0];c.fillRect(0,0,W,H);c.strokeStyle=palette[2];c.fillStyle=palette[2];c.lineWidth=.8;const sp=doc.spacing;
  if(doc.template==='grid'||doc.template==='cornell'){c.beginPath();for(let x=0;x<W;x+=sp){c.moveTo(x,0);c.lineTo(x,H);}for(let y=0;y<H;y+=sp){c.moveTo(0,y);c.lineTo(W,y);}c.stroke();}
  if(doc.template==='lined'){c.beginPath();for(let y=sp;y<H;y+=sp){c.moveTo(0,y);c.lineTo(W,y);}c.stroke();}
  if(doc.template==='dotted')for(let x=sp/2;x<W;x+=sp)for(let y=sp/2;y<H;y+=sp){c.beginPath();c.arc(x,y,1,0,Math.PI*2);c.fill();}
  if(doc.template==='cornell'){c.lineWidth=2;c.beginPath();c.moveTo(W*.25,H*.06);c.lineTo(W*.25,H*.86);c.moveTo(0,H*.06);c.lineTo(W,H*.06);c.moveTo(0,H*.86);c.lineTo(W,H*.86);c.stroke();}
  if(doc.template==='planner'){c.lineWidth=1.5;c.beginPath();for(let i=1;i<7;i++){c.moveTo(W*i/7,0);c.lineTo(W*i/7,H);}for(let y=50;y<H;y+=sp*4){c.moveTo(0,y);c.lineTo(W,y);}c.stroke();c.fillStyle=palette[1];c.font='16px Manrope';['Pzt','Sal','Çar','Per','Cum','Cmt','Paz'].forEach((s,i)=>c.fillText(s,i*W/7+15,30));}
  if(bgImage)c.drawImage(bgImage,0,0,W,H);
 }
 async function exportPage(format){
  try{
   if(page().pdfPage!==undefined&&!bgImage)throw new Error('PDF sayfası yüklenirken bekleyin');
   const out=document.createElement('canvas');out.width=W;out.height=H;const c=out.getContext('2d');drawPaper(c);
   // Draw ink on its own transparent layer so erasers cannot erase paper/PDF.
   const layer=document.createElement('canvas');layer.width=W;layer.height=H;const lc=layer.getContext('2d');page().strokes.forEach(s=>renderStroke(lc,s));c.drawImage(layer,0,0);
   const image=out.toDataURL('image/png');await window.studioCall('export',{format,image});$('exportDialog').close();showError('Kaydetmek için dosya konumu seçin');
  }catch(e){showError(e.message);}
 }
 function sampleSketch(){
  const strokes=[];
  const line=(points,color='#2c302e',width=2)=>strokes.push({tool:'pen',pen:'ballpoint',width,color,opacity:1,points:points.map(([x,y])=>({x,y,p:.5}))});
  const text=(value,x,y,size=15,color='#424843',serif=false)=>strokes.push({text:value,size,color,serif,points:[{x,y}]});
  text('Ahşap Birleşim Detayları — Kırlangıç Kuyruğu Geçme',180,70,25,'#1d1b18',true);
  text('NOT 01: AKSLAR',35,70,12,'#685a44');text('Kuzey cephe',35,95,13);text('nem oranı %14',35,116,13);text('DIN 1052 std.',35,137,13);
  line([[180,105],[595,105]],'#6c8372',22);
  text('Geleneksel Masif Çam Taşıyıcı & Birleşim Mukavemeti',184,111,14,'#4b6151');
  line([[220,180],[460,180],[520,230],[280,230],[220,180]]);
  line([[220,180],[220,240],[280,290],[280,230]]);
  line([[460,180],[460,240],[520,290],[520,230]]);
  line([[220,240],[460,240]]);line([[280,290],[520,290]]);
  line([[350,180],[390,180],[410,210],[330,210],[350,180]],'#b86b53',2.5);
  line([[540,180],[540,290]],'#685a44',1.4);line([[535,186],[540,180],[545,186]],'#685a44',1.4);line([[535,284],[540,290],[545,284]],'#685a44',1.4);
  text('h = 160 mm',552,240,13,'#685a44');
  line([[370,195],[420,150],[470,150]],'#4b6151',1.2);text('1:8 Konik Kırlangıç Açısı',480,152,13,'#4b6151');
  text('σ m,d = (M Ed / W net) ≤ f m,d · k mod',220,360,18,'#1d1b18',true);
  text('• Birleşim yüzeyinde gizli çelik bulon pimleme (Ø12mm)',220,402,14);
  text('• Kenar mesafesi minimum 7d seçilmelidir.',220,430,14);
  text('• Doğal reçine ile mikro çatlak su yalıtımı sağlanacak.',220,458,14);
  line([[720,210],[860,210],[860,400],[720,400],[720,210]],'#2c302e',1.8);
  for(let y=220;y<395;y+=16)line([[720,y],[Math.min(860,720+400-y),400]],'#c3c8c1',.8);
  text('KESİT A-A (1:10)',720,428,12,'#1d1b18');return strokes;
 }
 window.InkStudio={
  init(){
   canvas=$('inkCanvas');canvas.addEventListener('pointerdown',down);canvas.addEventListener('pointermove',move);canvas.addEventListener('pointerup',up);canvas.addEventListener('pointercancel',up);canvas.addEventListener('lostpointercapture',e=>{if(active)up(e);});
   document.querySelectorAll('[data-tool]').forEach(b=>b.onclick=()=>{if(b.dataset.tool==='pen'&&tool==='pen')$('penDialog').showModal();else selectTool(b.dataset.tool);});
   $('penSettings').onclick=()=>$('penDialog').showModal();
   document.querySelectorAll('[data-close]').forEach(b=>b.onclick=()=>$(b.dataset.close).close());
   document.querySelectorAll('[data-pen]').forEach(b=>b.onclick=()=>{pen=b.dataset.pen;document.querySelectorAll('[data-pen]').forEach(x=>x.classList.toggle('selected',x===b));});
   $('strokeWidth').oninput=e=>{width=Number(e.target.value);$('strokeLabel').textContent=width.toFixed(1)+' mm';};
   document.querySelectorAll('[data-width]').forEach(b=>b.onclick=()=>{$('strokeWidth').value=b.dataset.width;$('strokeWidth').dispatchEvent(new Event('input'));});
   ['#2c302e','#6c8372','#b86b53','#8a6d56','#415a77','#c99700'].forEach(color=>{const b=document.createElement('button');b.style.background=color;b.setAttribute('aria-label',color);b.onclick=()=>{ink=color;$('customInk').value=color;document.querySelectorAll('#inkPalette button').forEach(x=>x.classList.toggle('selected',x===b));};$('inkPalette').append(b);});
   $('customInk').oninput=e=>ink=e.target.value;$('inkOpacity').oninput=e=>{opacity=Number(e.target.value)/100;$('opacityLabel').textContent=e.target.value+'%';};
   $('undo').onclick=()=>{if(readonly||!undo.length)return;redo.push(page().strokes.slice());page().strokes=undo.pop();selection=[];redraw();dirty();};
   $('redo').onclick=()=>{if(readonly||!redo.length)return;undo.push(page().strokes.slice());page().strokes=redo.pop();selection=[];redraw();dirty();};
   $('clearPage').onclick=()=>{if(readonly||!confirm('Bu sayfadaki mürekkep temizlensin mi?'))return;remember();page().strokes=[];selection=[];redraw();dirty();};
   $('starPage').onclick=()=>{if(readonly)return;page().starred=!page().starred;dirty();};
   $('prevPage').onclick=()=>changePage(-1);$('nextPage').onclick=()=>changePage(1);$('addPage').onclick=newPage;$('pageShelf').onclick=()=>pageIndex<doc.pages.length-1?changePage(1):newPage();
   $('zoomIn').onclick=()=>{zoom=Math.min(2,zoom+.25);applyZoom();};$('zoomOut').onclick=()=>{zoom=Math.max(.5,zoom-.25);applyZoom();};$('zoomReset').onclick=()=>{zoom=1;applyZoom();};
   $('zen').onclick=()=>{$('editor').classList.add('zen');$('exitZen').hidden=false;};$('exitZen').onclick=()=>{$('editor').classList.remove('zen');$('exitZen').hidden=true;};
   $('export').onclick=()=>$('exportDialog').showModal();document.querySelectorAll('[data-export]').forEach(b=>b.onclick=()=>exportPage(b.dataset.export));
   $('pageOptions').onclick=()=>{$('editTemplate').value=doc.template;$('editPaper').value=doc.paperColor;$('editSpacing').value=String(doc.spacing);$('optionsDialog').showModal();};
   $('applyOptions').onclick=()=>{if(!readonly){doc.template=$('editTemplate').value;doc.paperColor=$('editPaper').value;doc.spacing=Number($('editSpacing').value);config();dirty();}$('optionsDialog').close();};
   $('cycleTemplate').onclick=()=>{if(readonly)return;const keys=Object.keys(names);doc.template=keys[(keys.indexOf(doc.template)+1)%keys.length];config();dirty();};
   document.addEventListener('keydown',e=>{if($('editor').hidden||/INPUT|TEXTAREA|SELECT/.test(e.target.tagName))return;if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='z'){e.preventDefault();$(e.shiftKey?'redo':'undo').click();}else if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='y'){e.preventDefault();$('redo').click();}});
  },
  open(book,onChange){
   bookId=book.id;readonly=!!book.deleted;changed=onChange;pageIndex=0;zoom=1;
   if(book.document){doc=JSON.parse(book.document);if(!Array.isArray(doc.pages)||!doc.pages.length)throw new Error('Geçersiz çizim belgesi');}
   else{doc=defaults();doc.template=book.paper?.includes('Cornell')?'cornell':book.paper?.includes('Çizgili')?'lined':'dotted';doc.paperColor=book.paper?.includes('Kum')?'sand':'cream';doc.pages=Array.from({length:Math.max(1,Math.min(200,book.pages||1))},()=>({strokes:[],starred:false}));}
   if(!book.document&&book.id==='urban')doc.pages[0].strokes=sampleSketch();
   $('canvasTitle').textContent=book.title;$('editor').classList.remove('zen');$('exitZen').hidden=true;
   ['addPage','clearPage','applyOptions','starPage'].forEach(id=>$(id).disabled=readonly);
   selectTool(book.content||book.imageUri?'text':'pen');showPage();
  },
  serialize(){if(active)up({pointerId:active.pointer});return JSON.stringify(doc);},paper(){return label(doc);},
  title(value){$('canvasTitle').textContent=value;},
  stop(){bgVersion++;bgImage=null;if(frame)cancelAnimationFrame(frame);frame=0;active=null;moving=null;},
  document(){return copy(doc);}
 };
})();
