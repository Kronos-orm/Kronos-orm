import{a as d}from"./chunk-YFWCYGX4.js";import{a as B}from"./chunk-CG4UHNJO.js";import{a as g}from"./chunk-FVAUZTMF.js";import"./chunk-HFYZ5PKQ.js";import"./chunk-3UJNDYOE.js";import{a as l}from"./chunk-2DBUGSSC.js";import"./chunk-XLVKZVFL.js";import{J as E}from"./chunk-H5O67WVB.js";import"./chunk-FLHNG7GK.js";import"./chunk-7JTQAWMY.js";import"./chunk-RHDDKGHQ.js";import{Ub as r,pa as i,rc as t,sc as k,xb as p}from"./chunk-WBN6TKF6.js";import{a as o,b as c,h as C}from"./chunk-TWZW5B45.js";var y=C(E());var u={title:"No-value Strategy",mdFile:"./index.md",category:B,order:10,imports:[d],demos:{AnimateLogoComponent:d}},a=u;var h=[];var A={AnimateLogoComponent:[{title:"TypeScript",code:`<pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-angular-ts"><span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">import</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  Component</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">} </span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">from</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> '@angular/core'</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">import</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {SharedModule} </span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">from</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> "../shared.module"</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#E6B673" class="ngde">@</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">Component</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">({</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  selector</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> 'animate-logo'</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  imports</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> [</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#E6B673" class="ngde">    SharedModule</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  ]</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  template</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> \`</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">    &#x3C;</span><span style="color:#22863A;--shiki-dark:#39BAE6" class="ngde">div</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> class</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">"bg"</span><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">></span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">      &#x3C;</span><span style="color:#22863A;--shiki-dark:#39BAE6" class="ngde">img</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> class</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">"logo"</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> src</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">"/assets/images/logo_circle.png"</span><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde"> /></span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">    &#x3C;/</span><span style="color:#22863A;--shiki-dark:#39BAE6" class="ngde">div</span><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">></span></span>
<span class="line"><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">  \`</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  standalone</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> true</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  styles</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> [</span></span>
<span class="line"><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">    \`</span></span>
<span class="line"><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">      :host</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        text-align</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#F29668;font-style:inherit;--shiki-dark-font-style:italic" class="ngde"> center</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        display</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#F29668;font-style:inherit;--shiki-dark-font-style:italic" class="ngde"> block</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        width</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 100</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">      }</span></span>
<span class="line"></span>
<span class="line"><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">      .bg</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        padding</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 20</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">px</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        background</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#F07178" class="ngde"> linear-gradient</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde">45</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">deg</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span><span style="color:#005CC5;--shiki-dark:#95E6CB" class="ngde"> #832E3D</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 0</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span><span style="color:#005CC5;--shiki-dark:#95E6CB" class="ngde"> #000</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 20</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span><span style="color:#005CC5;--shiki-dark:#95E6CB" class="ngde"> #7F52FF</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 40</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span><span style="color:#005CC5;--shiki-dark:#95E6CB" class="ngde"> #832E3D</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 60</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span><span style="color:#005CC5;--shiki-dark:#95E6CB" class="ngde"> #000</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 80</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span><span style="color:#005CC5;--shiki-dark:#95E6CB" class="ngde"> #7F52FF</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 100</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">)</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        background-size</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 500</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 500</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        animation</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> gradient </span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde">12</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">s</span><span style="color:#005CC5;--shiki-dark:#F29668;font-style:inherit;--shiki-dark-font-style:italic" class="ngde"> linear</span><span style="color:#005CC5;--shiki-dark:#F29668;font-style:inherit;--shiki-dark-font-style:italic" class="ngde"> infinite</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">      }</span></span>
<span class="line"></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">      @keyframes</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> gradient</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">        0%</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">          background-position</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 100</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 0</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">        }</span></span>
<span class="line"><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">        100%</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">          background-position</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 25</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 100</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">%</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">        }</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">      }</span></span>
<span class="line"></span>
<span class="line"><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">      .logo</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        width</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 100</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">px</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        height</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 100</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">px</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        animation-name</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> spin</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        animation-duration</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 27.00</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">s</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        animation-iteration-count</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#F29668;font-style:inherit;--shiki-dark-font-style:italic" class="ngde"> infinite</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        animation-timing-function</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#F29668;font-style:inherit;--shiki-dark-font-style:italic" class="ngde"> linear</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">        mix-blend-mode</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#F29668;font-style:inherit;--shiki-dark-font-style:italic" class="ngde"> exclusion</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">      }</span></span>
<span class="line"></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">      @keyframes</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> spin</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">        0%</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">          transform</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#F07178" class="ngde"> rotate</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde">0</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">deg</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">)</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">        }</span></span>
<span class="line"><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">        100%</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#39BAE6" class="ngde">          transform</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#F07178" class="ngde"> rotate</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde">360</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">deg</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">)</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">        }</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">      }</span></span>
<span class="line"><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">    \`</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  ]</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">})</span></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">export</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde"> class</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde"> AnimateLogoComponent</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">{}</span></span></code></pre>`}]},F=A;var m='<ng-doc-demo componentname="AnimateLogoComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo><h2 id="novaluestrategy-\u65E0\u503C\u7B56\u7565" href="documentation/en/concept/no-value-strategy" headinglink="true" class="ngde"><code class="ngde title ngde">NoValueStrategy</code> \u65E0\u503C\u7B56\u7565<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="novaluestrategy-\u65E0\u503C\u7B56\u7565"></ng-doc-heading-anchor></h2><p class="ngde">\u65E0\u503C\u7B56\u7565\u662F\u67E5\u8BE2\u6761\u4EF6\u8BED\u53E5Criteria\u4E2D\u7684\u4E00\u79CD\u7B56\u7565\uFF0C\u5F53\u6761\u4EF6\u8BED\u53E5\u4E3A\u4E8C\u5143\u64CD\u4F5C\u7B26\u65F6(\u5373\u53EF\u4EE5\u63A5\u6536\u53D8\u91CF\u53C2\u6570)\uFF0C\u5982\u679C\u53C2\u6570\u4E3A<code class="ngde">null</code>\uFF0C\u90A3\u4E48\u5C06\u4F1A\u4F7F\u7528\u65E0\u503C\u7B56\u7565\u751F\u6210SQL\u8BED\u53E5\u3002</p><h2 id="\u81EA\u5B9A\u4E49\u5168\u5C40\u9ED8\u8BA4\u7684\u65E0\u503C\u7B56\u7565" href="documentation/en/concept/no-value-strategy" headinglink="true" class="ngde">\u81EA\u5B9A\u4E49\u5168\u5C40\u9ED8\u8BA4\u7684\u65E0\u503C\u7B56\u7565<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u81EA\u5B9A\u4E49\u5168\u5C40\u9ED8\u8BA4\u7684\u65E0\u503C\u7B56\u7565"></ng-doc-heading-anchor></h2><p class="ngde"><code class="ngde">Kronos</code>\u9ED8\u8BA4\u7684\u65E0\u503C\u7B56\u7565\u4E3A<code class="ngde">DefaultNoValueStrategy</code>\uFF0C\u5B83\u7684\u4E3B\u8981\u903B\u8F91\u5982\u4E0B\uFF1A</p><ul class="ngde"><li class="ngde">\u5F53\u64CD\u4F5C\u7C7B\u578B\u4E3A<code class="ngde">UPDATE</code>\u6216<code class="ngde">DELETE</code>\u65F6\uFF1A<ul class="ngde"><li class="ngde">\u5982\u679C\u6761\u4EF6\u7C7B\u578B\u4E3A\u76F8\u7B49\u5224\u65AD\uFF0C\u5219\u8F6C\u6362\u4E3A<code class="ngde">is null</code>\u6216<code class="ngde">is not null</code></li><li class="ngde">\u5982\u679C\u6761\u4EF6\u7C7B\u578B\u4E3Alike\u3001in\u3001between\u6216\u6570\u503C\u5224\u65AD\uFF0C\u5219\u76F4\u63A5\u8BA4\u5B9A\u6761\u4EF6\u4E3A\u5F53\u524D\u6761\u4EF6\u7684\u76F8\u53CD\u503C</li><li class="ngde">\u5982\u679C\u6761\u4EF6\u4E3A\u5927\u4E8E\u3001\u5927\u4E8E\u7B49\u4E8E\u3001\u5C0F\u4E8E\u3001\u5C0F\u4E8E\u7B49\u4E8E\uFF0C\u5219\u8BA4\u5B9A\u6761\u4EF6\u4E3Afalse</li><li class="ngde">\u5176\u4ED6\u60C5\u51B5\u5FFD\u7565\u8BE5\u6761\u4EF6\u8BED\u53E5</li></ul></li><li class="ngde">\u5F53\u64CD\u4F5C\u7C7B\u578B\u4E3A<code class="ngde">SELECT</code>\u65F6\uFF0C\u5FFD\u7565\u8BE5\u6761\u4EF6\u8BED\u53E5</li></ul><p class="ngde">\u521B\u5EFA\u4E00\u4E2A\u81EA\u5B9A\u4E49\u7684\u65E0\u503C\u7B56\u7565\uFF0C\u53EA\u9700\u8981\u5B9E\u73B0<code class="ngde">NoValueStrategy</code>\u63A5\u53E3\u5373\u53EF\uFF0C\u5982\u4E0B\u6240\u793A\uFF1A</p><ng-doc-tab group="custom" name="YourCustomNoValueStrategy.kt" icon="" class="ngde"><pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-kotlin"><span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">object</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde"> YourCustomNoValueStrategy</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> : </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">NoValueStrategy</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>\n<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">    fun</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> ifNoValue</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(kOperateType: </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">KOperationType</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">, criteria: </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">Criteria</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">): </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">NoValueStrategyType</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>\n<span class="line"><span style="color:#6A737D;--shiki-dark:#ACB6BF8C;font-style:inherit;--shiki-dark-font-style:italic" class="ngde">        // your logic</span></span>\n<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    }</span></span>\n<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">}</span></span></code></pre></ng-doc-tab><ng-doc-tab group="custom" name="Main.kt" icon="" class="ngde"><pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-kotlin"><span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">Kronos.noValueStrategy </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">=</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> YourCustomNoValueStrategy</span></span></code></pre></ng-doc-tab><h2 id="\u52A8\u6001\u8BBE\u7F6E\u65E0\u503C\u7B56\u7565" href="documentation/en/concept/no-value-strategy" headinglink="true" class="ngde">\u52A8\u6001\u8BBE\u7F6E\u65E0\u503C\u7B56\u7565<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u52A8\u6001\u8BBE\u7F6E\u65E0\u503C\u7B56\u7565"></ng-doc-heading-anchor></h2><p class="ngde">\u53EA\u9700\u5728\u6761\u4EF6\u8BED\u53E5\u4E2D\u8C03\u7528<code class="ngde">ifNoValue</code>\u65B9\u6CD5\u5373\u53EF\uFF0C\u5982\u4E0B\u6240\u793A\uFF1A</p><pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-kotlin"><span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">where</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> { (it.age </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">==</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> null</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">).</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">ifNoValue</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(Ignore) }</span></span></code></pre><h2 id="\u65E0\u503C\u7B56\u7565\u7684\u7C7B\u578B" href="documentation/en/concept/no-value-strategy" headinglink="true" class="ngde">\u65E0\u503C\u7B56\u7565\u7684\u7C7B\u578B<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u65E0\u503C\u7B56\u7565\u7684\u7C7B\u578B"></ng-doc-heading-anchor></h2><p class="ngde">\u76EE\u524D\u652F\u6301\u7684\u65E0\u503C\u7B56\u7565\u6709\uFF1A</p><div class="ng-doc-table-wrapper ngde"><table class="ngde ngde" style="min-width: 692px"><thead class="ngde ngde"><tr class="ngde ngde"><th class="ngde ngde">name</th><th class="ngde ngde">description</th><th class="ngde ngde">type</th><th class="ngde ngde">default</th></tr></thead><tbody class="ngde"><tr class="ngde ngde"><td class="ngde ngde" style="white-space: nowrap"><code class="ngde title ngde">Ignore</code></td><td class="ngde ngde" style="min-width: 150px">\u5FFD\u7565\u8BE5\u6761\u4EF6\u8BED\u53E5</td><td class="ngde ngde"><code class="ngde ngde"><span class="type ngde" style="color: #c41d7f">NoValueStrategyType</span></code></td><td class="ngde ngde" style="white-space: nowrap"><code class="ngde ngde">ignore</code></td></tr><tr class="ngde ngde"><td class="ngde ngde" style="white-space: nowrap"><code class="ngde title ngde">False</code></td><td class="ngde ngde" style="min-width: 150px">\u6761\u4EF6\u8BED\u53E5\u4E3Afalse</td><td class="ngde ngde"><code class="ngde ngde"><span class="type ngde" style="color: #c41d7f">NoValueStrategyType</span></code></td><td class="ngde ngde" style="white-space: nowrap"><code class="ngde ngde">false</code></td></tr><tr class="ngde ngde"><td class="ngde ngde" style="white-space: nowrap"><code class="ngde title ngde">True</code></td><td class="ngde ngde" style="min-width: 150px">\u6761\u4EF6\u8BED\u53E5\u4E3Atrue</td><td class="ngde ngde"><code class="ngde ngde"><span class="type ngde" style="color: #c41d7f">NoValueStrategyType</span></code></td><td class="ngde ngde" style="white-space: nowrap"><code class="ngde ngde">true</code></td></tr><tr class="ngde ngde"><td class="ngde ngde" style="white-space: nowrap"><code class="ngde title ngde">JudgeNull</code></td><td class="ngde ngde" style="min-width: 150px">\u8F6C\u6362\u4E3A`is null`\u6216`is not null`</td><td class="ngde ngde"><code class="ngde ngde"><span class="type ngde" style="color: #c41d7f">NoValueStrategyType</span></code></td><td class="ngde ngde" style="white-space: nowrap"><code class="ngde ngde">judgeNull</code></td></tr><tr class="ngde ngde"><td class="ngde ngde" style="white-space: nowrap"><code class="ngde title ngde">Auto</code></td><td class="ngde ngde" style="min-width: 150px">\u9ED8\u8BA4\u7B56\u7565\uFF0C\u6839\u636E\u5168\u5C40\u9ED8\u8BA4\u7B56\u7565\u751F\u6210SQL\u8BED\u53E5\uFF0C\u901A\u5E38\u4E0D\u9700\u8981\u624B\u52A8\u8BBE\u7F6E</td><td class="ngde ngde"><code class="ngde ngde"><span class="type ngde" style="color: #c41d7f">NoValueStrategyType</span></code></td><td class="ngde ngde" style="white-space: nowrap"><code class="ngde ngde">auto</code></td></tr></tbody></table></div>',f=(()=>{let s=class s extends l{constructor(){super(),this.pageType="guide",this.pageContent=m,this.editSourceFileUrl="https://github.com/Kronos-orm/Kronos-orm/edit/docs/src/app/docs/en/6.concept/no-value-strategy/index.md?message=docs(): describe your changes here...",this.viewSourceFileUrl="https://github.com/Kronos-orm/Kronos-orm/blob/docs/src/app/docs/en/6.concept/no-value-strategy/index.md",this.page=a,this.demoAssets=F}};s.\u0275fac=function(e){return new(e||s)},s.\u0275cmp=i({type:s,selectors:[["ng-doc-page-vh1c5mfv"]],standalone:!0,features:[t([{provide:l,useExisting:s},h,a.providers??[]]),p,k],decls:1,vars:0,template:function(e,x){e&1&&r(0,"ng-doc-page")},dependencies:[g],encapsulation:2,changeDetection:0});let n=s;return n})(),b=[c(o({},(0,y.isRoute)(a.route)?a.route:{}),{path:"",component:f,title:"No-value Strategy"})],M=b;export{f as PageComponent,M as default};