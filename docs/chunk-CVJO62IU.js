import{a as R,g as N,h as v}from"./chunk-KWHBQCHZ.js";import{a as C}from"./chunk-HNSOEGGY.js";import{A as W}from"./chunk-KYQEA5CJ.js";import{G,Ga as b,I as P,J as _,Oa as z,ab as U,b as $,c as I,db as F,h as H,nb as t}from"./chunk-HYAZYSCF.js";import{h as T}from"./chunk-TWZW5B45.js";var wt=T($(),1),mt=T(I(),1),Bt=T(_(),1);var A={},q=(e,a)=>{A[e]=a},Z=e=>A[e],J=()=>Object.keys(A),j=()=>J().length,K={get:Z,set:q,keys:J,size:j},Q=e=>e.append("circle").attr("class","start-state").attr("r",t().state.sizeUnit).attr("cx",t().state.padding+t().state.sizeUnit).attr("cy",t().state.padding+t().state.sizeUnit),V=e=>e.append("line").style("stroke","grey").style("stroke-dasharray","3").attr("x1",t().state.textHeight).attr("class","divider").attr("x2",t().state.textHeight*2).attr("y1",0).attr("y2",0),D=(e,a)=>{let o=e.append("text").attr("x",2*t().state.padding).attr("y",t().state.textHeight+2*t().state.padding).attr("font-size",t().state.fontSize).attr("class","state-title").text(a.id),d=o.node().getBBox();return e.insert("rect",":first-child").attr("x",t().state.padding).attr("y",t().state.padding).attr("width",d.width+2*t().state.padding).attr("height",d.height+2*t().state.padding).attr("rx",t().state.radius),o},tt=(e,a)=>{let o=function(l,m,w){let k=l.append("tspan").attr("x",2*t().state.padding).text(m);w||k.attr("dy",t().state.textHeight)},s=e.append("text").attr("x",2*t().state.padding).attr("y",t().state.textHeight+1.3*t().state.padding).attr("font-size",t().state.fontSize).attr("class","state-title").text(a.descriptions[0]).node().getBBox(),g=s.height,p=e.append("text").attr("x",t().state.padding).attr("y",g+t().state.padding*.4+t().state.dividerMargin+t().state.textHeight).attr("class","state-description"),i=!0,r=!0;a.descriptions.forEach(function(l){i||(o(p,l,r),r=!1),i=!1});let y=e.append("line").attr("x1",t().state.padding).attr("y1",t().state.padding+g+t().state.dividerMargin/2).attr("y2",t().state.padding+g+t().state.dividerMargin/2).attr("class","descr-divider"),x=p.node().getBBox(),c=Math.max(x.width,s.width);return y.attr("x2",c+3*t().state.padding),e.insert("rect",":first-child").attr("x",t().state.padding).attr("y",t().state.padding).attr("width",c+2*t().state.padding).attr("height",x.height+g+2*t().state.padding).attr("rx",t().state.radius),e},et=(e,a,o)=>{let d=t().state.padding,s=2*t().state.padding,g=e.node().getBBox(),p=g.width,i=g.x,r=e.append("text").attr("x",0).attr("y",t().state.titleShift).attr("font-size",t().state.fontSize).attr("class","state-title").text(a.id),x=r.node().getBBox().width+s,c=Math.max(x,p);c===p&&(c=c+s);let l,m=e.node().getBBox();a.doc,l=i-d,x>p&&(l=(p-c)/2+d),Math.abs(i-m.x)<d&&x>p&&(l=i-(x-p)/2);let w=1-t().state.textHeight;return e.insert("rect",":first-child").attr("x",l).attr("y",w).attr("class",o?"alt-composit":"composit").attr("width",c).attr("height",m.height+t().state.textHeight+t().state.titleShift+1).attr("rx","0"),r.attr("x",l+d),x<=p&&r.attr("x",i+(c-s)/2-x/2+d),e.insert("rect",":first-child").attr("x",l).attr("y",t().state.titleShift-t().state.textHeight-t().state.padding).attr("width",c).attr("height",t().state.textHeight*3).attr("rx",t().state.radius),e.insert("rect",":first-child").attr("x",l).attr("y",t().state.titleShift-t().state.textHeight-t().state.padding).attr("width",c).attr("height",m.height+3+2*t().state.textHeight).attr("rx",t().state.radius),e},it=e=>(e.append("circle").attr("class","end-state-outer").attr("r",t().state.sizeUnit+t().state.miniPadding).attr("cx",t().state.padding+t().state.sizeUnit+t().state.miniPadding).attr("cy",t().state.padding+t().state.sizeUnit+t().state.miniPadding),e.append("circle").attr("class","end-state-inner").attr("r",t().state.sizeUnit).attr("cx",t().state.padding+t().state.sizeUnit+2).attr("cy",t().state.padding+t().state.sizeUnit+2)),at=(e,a)=>{let o=t().state.forkWidth,d=t().state.forkHeight;if(a.parentId){let s=o;o=d,d=s}return e.append("rect").style("stroke","black").style("fill","black").attr("width",o).attr("height",d).attr("x",t().state.padding).attr("y",t().state.padding)},nt=(e,a,o,d)=>{let s=0,g=d.append("text");g.style("text-anchor","start"),g.attr("class","noteText");let p=e.replace(/\r\n/g,"<br/>");p=p.replace(/\n/g,"<br/>");let i=p.split(z.lineBreakRegex),r=1.25*t().state.noteMargin;for(let y of i){let x=y.trim();if(x.length>0){let c=g.append("tspan");if(c.text(x),r===0){let l=c.node().getBBox();r+=l.height}s+=r,c.attr("x",a+t().state.noteMargin),c.attr("y",o+s+1.25*t().state.noteMargin)}}return{textWidth:g.node().getBBox().width,textHeight:s}},st=(e,a)=>{a.attr("class","state-note");let o=a.append("rect").attr("x",0).attr("y",t().state.padding),d=a.append("g"),{textWidth:s,textHeight:g}=nt(e,0,0,d);return o.attr("height",g+2*t().state.noteMargin),o.attr("width",s+t().state.noteMargin*2),o},O=function(e,a){let o=a.id,d={id:o,label:a.id,width:0,height:0},s=e.append("g").attr("id",o).attr("class","stateGroup");a.type==="start"&&Q(s),a.type==="end"&&it(s),(a.type==="fork"||a.type==="join")&&at(s,a),a.type==="note"&&st(a.note.text,s),a.type==="divider"&&V(s),a.type==="default"&&a.descriptions.length===0&&D(s,a),a.type==="default"&&a.descriptions.length>0&&tt(s,a);let g=s.node().getBBox();return d.width=g.width+2*t().state.padding,d.height=g.height+2*t().state.padding,K.set(o,d),d},X=0,rt=function(e,a,o){let d=function(r){switch(r){case N.relationType.AGGREGATION:return"aggregation";case N.relationType.EXTENSION:return"extension";case N.relationType.COMPOSITION:return"composition";case N.relationType.DEPENDENCY:return"dependency"}};a.points=a.points.filter(r=>!Number.isNaN(r.y));let s=a.points,g=G().x(function(r){return r.x}).y(function(r){return r.y}).curve(P),p=e.append("path").attr("d",g(s)).attr("id","edge"+X).attr("class","transition"),i="";if(t().state.arrowMarkerAbsolute&&(i=window.location.protocol+"//"+window.location.host+window.location.pathname+window.location.search,i=i.replace(/\(/g,"\\("),i=i.replace(/\)/g,"\\)")),p.attr("marker-end","url("+i+"#"+d(N.relationType.DEPENDENCY)+"End)"),o.title!==void 0){let r=e.append("g").attr("class","stateLabel"),{x:y,y:x}=U.calcLabelPosition(a.points),c=z.getRows(o.title),l=0,m=[],w=0,k=0;for(let u=0;u<=c.length;u++){let h=r.append("text").attr("text-anchor","middle").text(c[u]).attr("x",y).attr("y",x+l),f=h.node().getBBox();w=Math.max(w,f.width),k=Math.min(k,f.x),b.info(f.x,y,x+l),l===0&&(l=h.node().getBBox().height,b.info("Title height",l,x)),m.push(h)}let E=l*c.length;if(c.length>1){let u=(c.length-1)*l*.5;m.forEach((h,f)=>h.attr("y",x+f*l-u)),E=l*c.length}let n=r.node().getBBox();r.insert("rect",":first-child").attr("class","box").attr("x",y-w/2-t().state.padding/2).attr("y",x-E/2-t().state.padding/2-3.5).attr("width",w+t().state.padding).attr("height",E+t().state.padding),b.info(n)}X++},B,L={},ot=function(){},dt=function(e){e.append("defs").append("marker").attr("id","dependencyEnd").attr("refX",19).attr("refY",7).attr("markerWidth",20).attr("markerHeight",28).attr("orient","auto").append("path").attr("d","M 19,7 L9,13 L14,7 L9,1 Z")},ct=function(e,a,o,d){B=t().state;let s=t().securityLevel,g;s==="sandbox"&&(g=H("#i"+a));let p=s==="sandbox"?H(g.nodes()[0].contentDocument.body):H("body"),i=s==="sandbox"?g.nodes()[0].contentDocument:document;b.debug("Rendering diagram "+e);let r=p.select(`[id='${a}']`);dt(r);let y=d.db.getRootDoc();Y(y,r,void 0,!1,p,i,d);let x=B.padding,c=r.node().getBBox(),l=c.width+x*2,m=c.height+x*2,w=l*1.75;F(r,m,w,B.useMaxWidth),r.attr("viewBox",`${c.x-B.padding}  ${c.y-B.padding} `+l+" "+m)},lt=e=>e?e.length*B.fontSizeFactor:1,Y=(e,a,o,d,s,g,p)=>{let i=new W({compound:!0,multigraph:!0}),r,y=!0;for(r=0;r<e.length;r++)if(e[r].stmt==="relation"){y=!1;break}o?i.setGraph({rankdir:"LR",multigraph:!0,compound:!0,ranker:"tight-tree",ranksep:y?1:B.edgeLengthFactor,nodeSep:y?1:50,isMultiGraph:!0}):i.setGraph({rankdir:"TB",multigraph:!0,compound:!0,ranksep:y?1:B.edgeLengthFactor,nodeSep:y?1:50,ranker:"tight-tree",isMultiGraph:!0}),i.setDefaultEdgeLabel(function(){return{}}),p.db.extract(e);let x=p.db.getStates(),c=p.db.getRelations(),l=Object.keys(x);for(let n of l){let u=x[n];o&&(u.parentId=o);let h;if(u.doc){let f=a.append("g").attr("id",u.id).attr("class","stateGroup");h=Y(u.doc,f,u.id,!d,s,g,p);{f=et(f,u,d);let S=f.node().getBBox();h.width=S.width,h.height=S.height+B.padding/2,L[u.id]={y:B.compositTitleSize}}}else h=O(a,u);if(u.note){let f={descriptions:[],id:u.id+"-note",note:u.note,type:"note"},S=O(a,f);u.note.position==="left of"?(i.setNode(h.id+"-note",S),i.setNode(h.id,h)):(i.setNode(h.id,h),i.setNode(h.id+"-note",S)),i.setParent(h.id,h.id+"-group"),i.setParent(h.id+"-note",h.id+"-group")}else i.setNode(h.id,h)}b.debug("Count=",i.nodeCount(),i);let m=0;c.forEach(function(n){m++,b.debug("Setting edge",n),i.setEdge(n.id1,n.id2,{relation:n,width:lt(n.title),height:B.labelHeight*z.getRows(n.title).length,labelpos:"c"},"id"+m)}),C(i),b.debug("Graph after layout",i.nodes());let w=a.node();i.nodes().forEach(function(n){n!==void 0&&i.node(n)!==void 0?(b.warn("Node "+n+": "+JSON.stringify(i.node(n))),s.select("#"+w.id+" #"+n).attr("transform","translate("+(i.node(n).x-i.node(n).width/2)+","+(i.node(n).y+(L[n]?L[n].y:0)-i.node(n).height/2)+" )"),s.select("#"+w.id+" #"+n).attr("data-x-shift",i.node(n).x-i.node(n).width/2),g.querySelectorAll("#"+w.id+" #"+n+" .divider").forEach(h=>{let f=h.parentElement,S=0,M=0;f&&(f.parentElement&&(S=f.parentElement.getBBox().width),M=parseInt(f.getAttribute("data-x-shift"),10),Number.isNaN(M)&&(M=0)),h.setAttribute("x1",0-M+8),h.setAttribute("x2",S-M-8)})):b.debug("No Node "+n+": "+JSON.stringify(i.node(n)))});let k=w.getBBox();i.edges().forEach(function(n){n!==void 0&&i.edge(n)!==void 0&&(b.debug("Edge "+n.v+" -> "+n.w+": "+JSON.stringify(i.edge(n))),rt(a,i.edge(n),i.edge(n).relation))}),k=w.getBBox();let E={id:o||"root",label:o||"root",width:0,height:0};return E.width=k.width+2*B.padding,E.height=k.height+2*B.padding,b.debug("Doc rendered",E,i),E},gt={setConf:ot,draw:ct},bt={parser:R,db:N,renderer:gt,styles:v,init:e=>{e.state||(e.state={}),e.state.arrowMarkerAbsolute=e.arrowMarkerAbsolute,N.clear()}};export{bt as diagram};
