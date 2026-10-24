// Optional real Chromium checks. Install playwright-core@1.51.1 and @sparticuz/chromium@133.0.0
// in .cache/ui-test, then run with NODE_PATH="$PWD/.cache/ui-test/node_modules".
const {chromium:pw}=require('playwright-core');const chromium=require('@sparticuz/chromium');
const fs=require('node:fs');const assert=require('node:assert/strict');
(async()=>{
 const browser=await pw.launch({executablePath:await chromium.executablePath(),args:chromium.args,headless:true});
 const page=await browser.newPage({viewport:{width:1280,height:1024},deviceScaleFactor:1});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://appassets.androidplatform.net/assets/**',route=>{const path='app/src/main/assets/'+new URL(route.request().url()).pathname.split('/assets/')[1];const ext=path.split('.').pop();route.fulfill({body:fs.readFileSync(path),contentType:({html:'text/html',css:'text/css',js:'text/javascript',ttf:'font/ttf'})[ext]||'text/plain'});});
 await page.addInitScript(()=>{
  let data=[{id:'urban',title:'İskandinav Strüktür Notları',coverTitle:'Kentsel\nEskizler',coverSubtitle:'Helsinki & Stockholm\nSerisi',paper:'Noktalı · Fildişi',pages:48,color:0,tag:'CİLT I',date:'Bugün 14:20',favorite:true,description:'Ahşap birleşim detayları',content:''}];
  window.Atelier={postMessage(raw){const r=JSON.parse(raw),a=r.args;let result=true;
   if(r.action==='list')result=data.map(({document,content,...b})=>b);
   if(r.action==='get')result=data.find(b=>b.id===a.id);
   if(r.action==='save')Object.assign(data.find(b=>b.id===a.id),a);
   if(r.action==='create'){result={...a,id:'new',coverTitle:a.title,description:'',content:'',pages:1};data.push(result);}
   queueMicrotask(()=>window.Atelier.onmessage({data:JSON.stringify({requestId:r.requestId,data:result})}));
  }};
 });
 await page.goto('https://appassets.androidplatform.net/assets/index.html');await page.locator('.card').waitFor();
 await page.click('#new');await page.click('#newNotebook');await page.locator('#create[open]').waitFor();
 await page.click('[data-template="cornell"]');await page.click('[data-paper="slate"]');await page.click('[data-orientation="landscape"]');
 assert.equal(await page.locator('#previewSheet').getAttribute('data-pattern'),'cornell');
 fs.mkdirSync('.cache/screenshots',{recursive:true});await page.screenshot({path:'.cache/screenshots/templates.png'});
 await page.click('#cancel');await page.click('[data-open="urban"]');await page.locator('#inkCanvas').waitFor();
 await page.waitForFunction(()=>window.InkStudio.document().pages[0].strokes.length>10);
 await page.screenshot({path:'.cache/screenshots/canvas.png'});
 const before=await page.evaluate(()=>InkStudio.document().pages[0].strokes.length);
 const rect=await page.locator('#inkCanvas').boundingBox();
 await page.mouse.move(rect.x+200,rect.y+500);await page.mouse.down();await page.mouse.move(rect.x+400,rect.y+550,{steps:20});await page.mouse.up();
 assert.equal(await page.evaluate(()=>InkStudio.document().pages[0].strokes.length),before+1);
 await page.click('#undo');assert.equal(await page.evaluate(()=>InkStudio.document().pages[0].strokes.length),before);
 await page.click('#redo');assert.equal(await page.evaluate(()=>InkStudio.document().pages[0].strokes.length),before+1);
 await page.click('#addPage');assert.equal(await page.evaluate(()=>InkStudio.document().pages.length),49);
 assert.equal(await page.evaluate(()=>InkStudio.document().pages[48].strokes.length),0);
 await page.click('#prevPage');await page.click('#back');await page.locator('#editor').waitFor({state:'hidden'});
 await page.click('[data-open="urban"]');assert.equal(await page.evaluate(()=>InkStudio.document().pages.length),49);
 await page.click('#back');await page.locator('#editor').waitFor({state:'hidden'});
 await page.setViewportSize({width:390,height:844});await page.click('#new');await page.click('#newNotebook');
 await page.screenshot({path:'.cache/screenshots/templates-phone.png'});
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true,'phone has no horizontal overflow');
 assert.deepEqual(errors,[],'no browser errors');
 console.log('PASS: real canvas draw, undo/redo, page isolation, persistence round-trip, preview settings, mobile layout');
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1);});
