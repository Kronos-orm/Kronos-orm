import{b as J}from"./chunk-XLNHBPPO.js";import{a as W,b as Y,d as Z}from"./chunk-ZTL2PG2M.js";import{F as q,I as z,Z as H,_ as M,n as w,r as G,w as j,z as Q}from"./chunk-3UJNDYOE.js";import{v as K,w as X}from"./chunk-RHDDKGHQ.js";import{$b as g,Aa as d,Cb as v,Cc as h,Dc as k,Ha as U,Ib as a,Jb as x,Ob as P,Qb as R,Rb as D,Sb as n,Tb as r,Ub as c,Yb as S,ac as _,eb as F,fd as $,id as O,jb as i,jc as I,kb as y,kc as m,lc as b,pa as E,rc as V,sc as L,tc as C,za as u}from"./chunk-WBN6TKF6.js";function ne(e,o){e&1&&(n(0,"div",6),c(1,"span",7),r())}function oe(e,o){e&1&&(n(0,"div",8),c(1,"span",7),r())}function re(e,o){if(e&1){let t=S();n(0,"a",2),g("click",function(){let p=u(t).$implicit,s=_();return d(s.onClick.emit(p))}),v(1,ne,2,0,"div",3),n(2,"span",4),m(3),r(),v(4,oe,2,0,"div",5),r()}if(e&2){let t=o.$implicit;i(),a("ngIf",t.leftIcon),i(2),b(t.label),i(),a("ngIf",t.rightIcon)}}var ee=(()=>{let o=class o{constructor(){this.onClick=new U}};o.\u0275fac=function(p){return new(p||o)},o.\u0275cmp=E({type:o,selectors:[["ui-list"]],inputs:{list:"list"},outputs:{onClick:"onClick"},standalone:!0,features:[L],decls:3,vars:0,consts:[[1,"list-none","m-0","p-0","w-12rem"],[1,"block","p-2","border-round","hover:surface-hover","w-full","cursor-pointer","flex"],[1,"block","p-2","border-round","hover:surface-hover","w-full","cursor-pointer","flex",3,"click"],["class","mr-2 text-700 flex-1 text-right inline-block",4,"ngIf"],[1,"font-bold","text-900","flex-1"],["class","ml-2 text-700 flex-1 text-right inline-block",4,"ngIf"],[1,"mr-2","text-700","flex-1","text-right","inline-block"],[1,"pi","pi-globe"],[1,"ml-2","text-700","flex-1","text-right","inline-block"]],template:function(p,s){p&1&&(n(0,"ul",0),R(1,re,5,3,"a",1,P),r()),p&2&&(i(),D(s.list))},dependencies:[M,O,J],styles:["a[_ngcontent-%COMP%]{text-decoration:none}"]});let e=o;return e})();var N=()=>["/"];function ae(e,o){e&1&&(c(0,"img",7),n(1,"span",8),m(2,"Kronos ORM"),r()),e&2&&(x("width",60,"px"),a("routerLink",C(4,N)),i(),a("routerLink",C(5,N)))}function le(e,o){if(e&1&&(n(0,"a",18),c(1,"i",19),n(2,"span",20),m(3),h(4,"transloco"),r()()),e&2){let t=_().$implicit;a("routerLink",t.routerLink),i(),a("ngClass",t.icon),i(2),b(k(4,3,t.label))}}function pe(e,o){if(e&1&&(n(0,"a",21)(1,"span",22),c(2,"i",19),r(),n(3,"span",23)(4,"span",24),m(5),h(6,"transloco"),r(),n(7,"span",25),m(8),h(9,"transloco"),r()()()),e&2){let t=_().$implicit;a("routerLink",t.routerLink),i(2),a("ngClass",t.icon+" text-lg"),i(3),b(k(6,4,t.label)),i(3),b(k(9,6,t.subtext))}}function se(e,o){if(e&1&&(n(0,"div",26),c(1,"img",27),n(2,"span",28),m(3),h(4,"transloco"),r(),c(5,"p-button",29),h(6,"transloco"),r()),e&2){let t=_().$implicit;a("routerLink",t.routerLink),i(),a("src",t.image,F),i(2),b(k(4,5,t.subtext)),i(2),a("label",k(6,7,t.label))("outlined",!0)}}function ce(e,o){if(e&1&&v(0,le,5,5,"a",15)(1,pe,10,8,"a",16)(2,se,7,9,"div",17),e&2){let t=o.$implicit;a("ngIf",t.root),i(),a("ngIf",!t.root&&!t.image),i(),a("ngIf",t.image)}}function me(e,o){if(e&1){let t=S();n(0,"p-button",30),g("click",function(p){u(t),_();let s=I(16);return d(s.toggle(p))}),r(),n(1,"a",31),c(2,"i",11),r()}e&2&&(i(),x("height",42,"px"))}var Ne=(()=>{let o=class o{ngOnInit(){this.items=[{label:"HOME",root:!0,icon:"pi pi-home",routerLink:"/home"},{label:"DOCUMENTATION",icon:"pi pi-book",root:!0,items:[[{items:[{label:"QUICK_START",icon:"pi pi-play",subtext:"SUBTEXT_OF_QUICK_START",routerLink:`/documentation/${this.appService.language}/getting-started/quick-start`},{label:"CLASS_DEFINITION",icon:"pi pi-list",subtext:"SUBTEXT_OF_CLASS_DEFINITION",routerLink:`/documentation/${this.appService.language}/class-definition/table-class-definition`},{label:"CONNECT_TO_DB",icon:"pi pi-download",subtext:"SUBTEXT_OF_CONNECT_TO_DB",routerLink:`/documentation/${this.appService.language}/database/connect-to-db`}]}],[{items:[{label:"DATABASE_OPERATION",icon:"pi pi-user",subtext:"SUBTEXT_OF_DATABASE_OPERATION",routerLink:`/documentation/${this.appService.language}/database/database-operation`},{label:"SQL_EXECUTE",icon:"pi pi-info",subtext:"SUBTEXT_OF_SQL_EXECUTE",routerLink:`/documentation/${this.appService.language}/database/named-arguments-base-sql`},{label:"CRITERIA_DEFINITION",icon:"pi pi-search",subtext:"SUBTEXT_OF_CRITERIA_DEFINITION",routerLink:`/documentation/${this.appService.language}/database/where-having-on-clause`}]}],[{items:[{label:"ADVANCED",icon:"pi pi-star",subtext:"SUBTEXT_OF_ADVANCED",routerLink:`/documentation/${this.appService.language}/advanced/some-locks`},{label:"PLUGIN",icon:"pi pi-globe",subtext:"SUBTEXT_OF_PLUGIN",routerLink:`/documentation/${this.appService.language}/plugin/datasource-wrapper-and-third-part-framework`},{label:"CHANGELOG",icon:"pi pi-clock",subtext:"SUBTEXT_OF_CHANGELOG",routerLink:`/documentation/${this.appService.language}/getting-started/changelog`}]}],[{items:[{image:"https://cdn.leinbo.com/assets/images/kronos/code-cover.jpg",label:"GET_START",subtext:"SUBTEXT_OF_QUICK_START",routerLink:[`/documentation/${this.appService.language}/getting-started/quick-start`]}]}]]},{label:"RESOURCES",icon:"pi pi-palette",root:!0,items:[[{items:[{image:"https://cdn.leinbo.com/assets/images/kronos/code-cover.jpg",label:"COMING_SOON",subtext:"CODE_GENERATOR"}]}]]},{label:"DISCUSSION",icon:"pi pi-comments",root:!0,routerLink:"https://github.com/Kronos-orm/Kronos-orm/discussions"}],this.translocoService.selectTranslate(["DOCUMENTATION","CODE_GENERATOR","DISCUSSION"]).subscribe(([l,p,s])=>{this.menus=[{label:l,href:`/#/documentation/${this.appService.language}/getting-started/introduce`},{label:p},{label:s,href:"https://github.com/Kronos-orm/Kronos-orm/discussions"}]})}constructor(l,p,s){this.appService=l,this.translocoService=p,this.messageService=s,this.languages=[{lang:"zh-CN",label:"\u7B80\u4F53\u4E2D\u6587",rightIcon:"pi pi-globe"},{lang:"en",label:"English",rightIcon:"pi pi-globe"}],this.menus=[]}setLang(l){this.appService.language!==l&&(this.appService.language=l,this.translocoService.setActiveLang(l),window.location.reload())}onSelect(l){l.href?window.location.href=l.href:this.messageService.add({severity:"info",summary:this.translocoService.translate("COMING_SOON"),detail:l.label})}};o.\u0275fac=function(p){return new(p||o)(y(Z),y(W),y(w))},o.\u0275cmp=E({type:o,selectors:[["layout-menu-bar"]],standalone:!0,features:[V([w]),L],decls:21,vars:14,consts:[["op",""],["menu",""],[1,"hidden","md:block",3,"model","styleClass"],["pTemplate","start"],["pTemplate","item"],["pTemplate","end"],[1,"block","md:hidden","flex","align-items-center","border-none","menu-bar","p-0","pl-4"],["src","/assets/images/logo_circle.png","draggable","false","alt","logo",1,"logo",3,"routerLink"],[1,"logo",3,"routerLink"],["link","","icon","pi pi-language",3,"click"],["href","https://github.com/Kronos-orm/Kronos-orm","target","_blank",1,"p-ripple","p-element","p-button","p-component","p-button-icon-only","p-button-link"],[1,"pi","pi-github"],["icon","pi pi-bars","link","",1,"mr-4",3,"click"],["styleClass","m-0 p-0"],[3,"onClick","list"],["pRipple","","routerLinkActive","p-menuitem-active","class","flex align-items-center p-menuitem-link cursor-pointer px-3 py-2 overflow-hidden relative font-semibold text-lg",3,"routerLink",4,"ngIf"],["class","flex align-items-center p-3 cursor-pointer mb-2 gap-2",3,"routerLink",4,"ngIf"],["class","flex flex-column align-items-start gap-3 p-2",3,"routerLink",4,"ngIf"],["pRipple","","routerLinkActive","p-menuitem-active",1,"flex","align-items-center","p-menuitem-link","cursor-pointer","px-3","py-2","overflow-hidden","relative","font-semibold","text-lg",3,"routerLink"],[3,"ngClass"],[1,"mx-2"],[1,"flex","align-items-center","p-3","cursor-pointer","mb-2","gap-2",3,"routerLink"],[1,"inline-flex","align-items-center","justify-content-center","border-circle","bg-primary","w-3rem","h-3rem"],[1,"inline-flex","flex-column","gap-1",2,"width","calc(100% - 3.5rem)"],[1,"font-medium","text-lg","text-900"],[1,"white-space-nowrap","text-overflow-ellipsis","overflow-hidden","text-gray-200"],[1,"flex","flex-column","align-items-start","gap-3","p-2",3,"routerLink"],["alt","megamenu-demo",1,"w-full",3,"src"],[1,"text-900","text-gray-200"],[3,"label","outlined"],["link","","icon","pi pi-language",1,"mr-4",3,"click"],["href","https://github.com/Kronos-orm/Kronos-orm","target","_blank",1,"mr-4","p-ripple","p-element","p-button","p-component","p-button-icon-only","p-button-link"]],template:function(p,s){if(p&1){let T=S();c(0,"p-toast"),n(1,"p-megaMenu",2),v(2,ae,3,6,"ng-template",3)(3,ce,3,3,"ng-template",4)(4,me,3,2,"ng-template",5),r(),n(5,"div",6)(6,"div"),c(7,"img",7),n(8,"span",8),m(9,"Kronos ORM"),r()(),n(10,"div")(11,"p-button",9),g("click",function(f){u(T);let B=I(16);return d(B.toggle(f))}),r(),n(12,"a",10),c(13,"i",11),r(),n(14,"p-button",12),g("click",function(f){u(T);let B=I(19);return d(B.toggle(f))}),r()()(),n(15,"p-overlayPanel",13,0)(17,"ui-list",14),g("onClick",function(f){return u(T),d(s.setLang(f.lang))}),r()(),n(18,"p-overlayPanel",13,1)(20,"ui-list",14),g("onClick",function(f){return u(T),d(s.onSelect(f))}),r()()}p&2&&(i(),a("model",s.items)("styleClass","border-none menu-bar p-0 pl-4"),i(6),x("width",60,"px"),a("routerLink",C(12,N)),i(),a("routerLink",C(13,N)),i(2),x("margin-left","auto"),i(2),x("height",42,"px"),i(5),a("list",s.languages),i(3),a("list",s.menus))},dependencies:[M,$,O,Q,G,j,q,K,X,z,H,Y,ee],styles:["[_nghost-%COMP%]     .p-megamenu-start{margin-right:24px}[_nghost-%COMP%]     .menu-bar{display:flex;background:linear-gradient(45deg,#832e3d,#000,#7f52ff,#832e3d,#000,#7f52ff);background-size:500% 500%;animation:_ngcontent-%COMP%_gradient 12s linear infinite}@keyframes _ngcontent-%COMP%_gradient{0%{background-position:100% 0}to{background-position:25% 100%}}[_nghost-%COMP%]     .logo{cursor:pointer;mix-blend-mode:exclusion;transform-origin:center;vertical-align:middle;font-family:Poppins,sans-serif;font-weight:bolder;padding:15px}[_nghost-%COMP%]     .logo:hover{filter:grayscale(100%);opacity:.8;transition:.1s ease-in-out}[_nghost-%COMP%]     .p-menuitem-active{color:#ffffffde;background:#fff3}a[_ngcontent-%COMP%]{text-decoration:none}"]});let e=o;return e})();export{Ne as a};
