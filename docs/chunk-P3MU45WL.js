import{a as g}from"./chunk-535PBHAA.js";import{a as c}from"./chunk-YFWCYGX4.js";import{a as B}from"./chunk-FVAUZTMF.js";import"./chunk-HFYZ5PKQ.js";import"./chunk-3UJNDYOE.js";import{a as e}from"./chunk-2DBUGSSC.js";import"./chunk-XLVKZVFL.js";import{J as E}from"./chunk-H5O67WVB.js";import"./chunk-FLHNG7GK.js";import"./chunk-7JTQAWMY.js";import"./chunk-RHDDKGHQ.js";import{Ub as r,pa as d,rc as k,sc as t,xb as p}from"./chunk-WBN6TKF6.js";import{a as i,b as o,h as C}from"./chunk-TWZW5B45.js";var y=C(E());var A={title:"Cascade Select",mdFile:"./index.md",route:"cascade-select",category:g,order:8,imports:[c],demos:{AnimateLogoComponent:c}},a=A;var h=[];var m={AnimateLogoComponent:[{title:"TypeScript",code:`<pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-angular-ts"><span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">import</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
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
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">export</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde"> class</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde"> AnimateLogoComponent</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">{}</span></span></code></pre>`}]},F=m;var u=`<ng-doc-demo componentname="AnimateLogoComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo><h2 id="\u90E8\u5206\u5F00\u542F\u53CA\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2" href="documentation/en/advanced/cascade-select" headinglink="true" class="ngde">\u90E8\u5206\u5F00\u542F\u53CA\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u90E8\u5206\u5F00\u542F\u53CA\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2"></ng-doc-heading-anchor></h2><h3 id="\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2" href="documentation/en/advanced/cascade-select" headinglink="true" class="ngde">\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2"></ng-doc-heading-anchor></h3><p class="ngde">Kronos\u9ED8\u8BA4\u5F00\u542F\u7EA7\u8054\u67E5\u8BE2\u529F\u80FD\uFF0C\u9700\u8981\u5728<code class="ngde">select</code>\u51FD\u6570\u4E2D\u663E\u5F0F\u5173\u95ED\uFF1A</p><pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-kotlin"><span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">KPojo.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">select</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">().</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">cascade</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(enable </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">=</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> false</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">).</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">queryList</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">()</span></span></code></pre><h3 id="\u90E8\u5206\u5F00\u542F\u7EA7\u8054\u67E5\u8BE2" href="documentation/en/advanced/cascade-select" headinglink="true" class="ngde">\u90E8\u5206\u5F00\u542F\u7EA7\u8054\u67E5\u8BE2<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u90E8\u5206\u5F00\u542F\u7EA7\u8054\u67E5\u8BE2"></ng-doc-heading-anchor></h3><p class="ngde">\u5F53KPojo\u4E2D\u6709\u591A\u4E2A\u7EA7\u8054\u58F0\u660E\uFF0C\u4F46\u53EA\u6709\u90E8\u5206\u9700\u8981\u7EA7\u8054\u67E5\u8BE2\u65F6\uFF0C\u53EF\u4EE5\u5C06\u9700\u8981\u7EA7\u8054\u67E5\u8BE2\u7684\u5C5E\u6027\u4F20\u5165<code class="ngde">cascade</code>\u51FD\u6570\uFF0C\u5176\u4F59\u7684\u5C5E\u6027\u53CA\u5B50\u5C5E\u6027\u5C06\u4E0D\u89E6\u53D1\u7EA7\u8054\u67E5\u8BE2\u3002</p><pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-kotlin"><span class="line"><span style="color:#6A737D;--shiki-dark:#ACB6BF8C;font-style:inherit;--shiki-dark-font-style:italic" class="ngde">// \u82E5KPojo\u4E2D\u53EA\u6709property1\u548Cproperty2\u9700\u8981\u7EA7\u8054\u5220\u9664\uFF0C\u90A3\u4E48\u5982\u4E0B\uFF1A</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">KPojo.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">select</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">().</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">cascade</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(KPojo::</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">property1</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">, KPojo::</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">property2</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">).</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">queryList</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">()</span></span></code></pre><p class="ngde">\u53EF\u4EE5\u9650\u5236\u5176\u5B50\u5C5E\u6027\u7EA7\u8054\u67E5\u8BE2\uFF0C\u5982\u4E0B\uFF1A</p><pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-kotlin"><span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">KPojo.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">select</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">().</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">cascade</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    KPojo::</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">property1</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    KPojo::</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">property2</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    Property1::</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">subProperty1</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    Property1::</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">subProperty2</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">).</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">queryList</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">()</span></span></code></pre><h3 id="cascadeselectignore-\u58F0\u660E\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2" href="documentation/en/advanced/cascade-select" headinglink="true" class="ngde"><code class="ngde title ngde">@CascadeSelectIgnore</code> \u58F0\u660E\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="cascadeselectignore-\u58F0\u660E\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2"></ng-doc-heading-anchor></h3><p class="ngde">\u5728\u5B9A\u4E49<code class="ngde">KPojo</code>\u7C7B\u65F6,\u901A\u8FC7\u6DFB\u52A0<code class="ngde">@CascadeSelectIgnore</code>\u6CE8\u89E3\u58F0\u660E\u67D0\u5C5E\u6027\u67E5\u8BE2\u65F6\u4E0D\u7EA7\u8054\u67E5\u8BE2, \u8BE6\u89C1\uFF1A <code class="ngde ng-doc-code-with-link ngde"><a class="ngde ngde" href="/documentation/en/class-definition/annotation-config#cascadeselectignore\u5173\u95ED\u5C5E\u6027\u7EA7\u8054\u67E5\u8BE2">\u{1F4DA}\u6CE8\u89E3\u914D\u7F6E - CascadeSelectIgnore\u5173\u95ED\u5C5E\u6027\u7EA7\u8054\u67E5\u8BE2</a></code></p><h2 id="\u7EA7\u8054\u67E5\u8BE2" href="documentation/en/advanced/cascade-select" headinglink="true" class="ngde">\u7EA7\u8054\u67E5\u8BE2<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u7EA7\u8054\u67E5\u8BE2"></ng-doc-heading-anchor></h2><p class="ngde">\u5728\u7EA7\u8054\u5173\u7CFB\u88AB\u5B9A\u4E49\u540E\uFF0C\u4F7F\u7528\uFF1A</p><ol class="ngde"><li class="ngde"><code class="ngde ng-doc-code-with-link ngde"><a class="ngde ngde" href="/documentation/en/database/select-records#querylist\u67E5\u8BE2\u6307\u5B9A\u7C7B\u578B\u5217\u8868">\u{1F4DA}queryList\u67E5\u8BE2\u6307\u5B9A\u7C7B\u578B\u5217\u8868</a></code></li><li class="ngde"><code class="ngde ng-doc-code-with-link ngde"><a class="ngde ngde" href="/documentation/en/database/select-records#queryone\u67E5\u8BE2\u5355\u6761\u8BB0\u5F55">\u{1F4DA}queryOne\u67E5\u8BE2\u5355\u6761\u8BB0\u5F55</a></code></li><li class="ngde"><code class="ngde ng-doc-code-with-link ngde"><a class="ngde ngde" href="/documentation/en/database/select-records#queryoneornull\u67E5\u8BE2\u5355\u6761\u8BB0\u5F55\u53EF\u7A7A">\u{1F4DA}queryOneOrNull\u67E5\u8BE2\u5355\u6761\u8BB0\u5F55\uFF08\u53EF\u7A7A\uFF09</a></code></li></ol><p class="ngde">\u4EE5\u4E0A\u4E09\u79CD\u65B9\u6CD5\u67E5\u8BE2\u6570\u636E\u65F6\uFF0C\u6211\u4EEC\u5C06\u81EA\u52A8\u4E3A\u60A8\u6839\u636E\u7EA7\u8054\u5173\u7CFB\u8FDB\u884C\u903B\u8F91\u67E5\u8BE2\uFF0C\u8BE6\u89C1\uFF1A<code class="ngde ng-doc-code-with-link ngde"><a class="ngde ngde" href="/documentation/en/advanced/cascade-definition#\u7EA7\u8054\u5173\u7CFB\u5B9A\u4E49">\u{1F4DA}\u7EA7\u8054\u5173\u7CFB\u5B9A\u4E49</a></code>\u3002</p><p class="ngde">\u7EA7\u8054\u67E5\u8BE2\u9ED8\u8BA4\u4E0D\u9650\u5236\u5C42\u7EA7\u548C\u7EA7\u8054\u5173\u7CFB\u65B9\u5411\uFF0C\u5982\u679C\u60A8\u7684\u7EA7\u8054\u5173\u7CFB\u5C42\u6570\u5F88\u6DF1\uFF0C\u5728\u67E5\u8BE2\u65F6\u8BF7\u6CE8\u610F<code class="ngde ng-doc-code-with-link ngde"><a class="ngde ngde" href="/documentation/en/advanced/cascade-select#\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2">\u{1F4DA}\u5173\u95ED\u7EA7\u8054\u67E5\u8BE2</a></code>\uFF0C\u6216\u4EC5<code class="ngde ng-doc-code-with-link ngde"><a class="ngde ngde" href="/documentation/en/advanced/cascade-select#\u90E8\u5206\u5F00\u542F\u7EA7\u8054\u67E5\u8BE2">\u{1F4DA}\u90E8\u5206\u5F00\u542F\u7EA7\u8054\u67E5\u8BE2</a></code>\u4EE5\u4FDD\u8BC1\u4E0D\u4F1A<strong class="ngde">\u67E5\u8BE2\u5230\u60A8\u4E0D\u9700\u8981\u7684\u6570\u636E</strong>\u3002</p><ng-doc-blockquote type="note" class="ngde"><p class="ngde"><strong class="ngde">Q:</strong> Kronos\u4E2D\u5982\u4F55\u5904\u7406\u7EA7\u8054\u5173\u7CFB\u4E2D\u7684\u5FAA\u73AF\u5F15\u7528\uFF1F\u4F1A\u65E0\u9650\u5FAA\u73AF\u67E5\u8BE2\u5417\uFF1F</p><p class="ngde"><strong class="ngde">A:</strong> Kronos\u4E2D\u4F1A\u5BF9\u5FAA\u73AF\u5F15\u7528\u8FDB\u884C\u5904\u7406\uFF0C\u6BCF\u4E2AKPojo\u7C7B\u7684\u5C5E\u6027\u5728\u4E0D\u540C\u5C42\u7EA7\u4EC5\u4F1A\u88AB\u67E5\u8BE2\u4E00\u6B21\uFF0C\u9047\u5230\u91CD\u590D\u5F15\u7528\u65F6\u81EA\u52A8\u505C\u6B62\uFF0C\u907F\u514D\u65E0\u9650\u5FAA\u73AF\u67E5\u8BE2\u3002</p><pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-ts"><span class="line"><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">A</span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">--></span><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">B</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">B</span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">--></span><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">C</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">C</span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">--></span><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">A</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">A</span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">--></span><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">B</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span><span style="color:#6A737D;--shiki-dark:#ACB6BF8C;font-style:inherit;--shiki-dark-font-style:italic" class="ngde"> //\u5C06\u4E0D\u4F1A\u89E6\u53D1\u6B64\u67E5\u8BE2</span></span></code></pre><p class="ngde">\u5982\u4E0B\u662F\u4E00\u4E2A\u793A\u4F8B\uFF1A</p><pre class="mermaid ngde">erDiagram
A {
    int bId
    B b
    List[C] listOfC
}
B {
    int cId
    C c
    List[A] listOfA
}
C {
    int aId
    A a
    List[B] listOfB
}
A ||--o{ B : "\u5173\u8054"
B ||--o{ C : "\u5173\u8054"
C ||--o{ A : "\u5173\u8054"</pre><p class="ngde">\u82E5\u6B64\u65F6\u67E5\u8BE2\u67D0\u4E2AA\u5B9E\u4F53\uFF0C\u67E5\u8BE2\u51FA\u7684\u5B8C\u6574\u7ED3\u6784\u5C06\u662F\uFF1A(\u6811\u72B6\u56FE\u8868\u793A)</p><pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-ts"><span class="line"><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">A1</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u251C\u2500\u2500 </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">bId</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> &#x3C;</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">A1\u7684bId</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">></span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u251C\u2500\u2500 b</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u251C\u2500\u2500 </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">cId</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> &#x3C;</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">B1\u7684cId</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">></span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u251C\u2500\u2500 c</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u2502 \u251C\u2500\u2500 </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">aId</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> &#x3C;</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">C1\u7684aId</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">></span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u2502 \u2514\u2500\u2500 listOfB</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u2502 \u251C\u2500\u2500 </span><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">B2</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u2502 \u251C\u2500\u2500 </span><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">B3</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u2502 \u2514\u2500\u2500 </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">...</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u2514\u2500\u2500 listOfA</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u251C\u2500\u2500 </span><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">A2</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u251C\u2500\u2500 </span><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">A3</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2502 \u2514\u2500\u2500 </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">...</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2514\u2500\u2500 listOfC</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u251C\u2500\u2500 </span><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">C1</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u251C\u2500\u2500 </span><span style="color:#005CC5;--shiki-dark:#BFBDB6" class="ngde">C2</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">\u2514\u2500\u2500 </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">...</span></span></code></pre></ng-doc-blockquote>`,f=(()=>{let s=class s extends e{constructor(){super(),this.pageType="guide",this.pageContent=u,this.editSourceFileUrl="https://github.com/Kronos-orm/Kronos-orm/edit/docs/src/app/docs/en/4.advanced/8.cascade-select/index.md?message=docs(): describe your changes here...",this.viewSourceFileUrl="https://github.com/Kronos-orm/Kronos-orm/blob/docs/src/app/docs/en/4.advanced/8.cascade-select/index.md",this.page=a,this.demoAssets=F}};s.\u0275fac=function(l){return new(l||s)},s.\u0275cmp=d({type:s,selectors:[["ng-doc-page-zxg5r455"]],standalone:!0,features:[k([{provide:e,useExisting:s},h,a.providers??[]]),p,t],decls:1,vars:0,template:function(l,x){l&1&&r(0,"ng-doc-page")},dependencies:[B],encapsulation:2,changeDetection:0});let n=s;return n})(),b=[o(i({},(0,y.isRoute)(a.route)?a.route:{}),{path:"",component:f,title:"Cascade Select"})],R=b;export{f as PageComponent,R as default};
