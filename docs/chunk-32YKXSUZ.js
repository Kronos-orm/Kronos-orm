import{a as g}from"./chunk-535PBHAA.js";import{a as i}from"./chunk-YFWCYGX4.js";import{a as B}from"./chunk-FVAUZTMF.js";import"./chunk-HFYZ5PKQ.js";import"./chunk-3UJNDYOE.js";import{a as e}from"./chunk-2DBUGSSC.js";import"./chunk-XLVKZVFL.js";import{J as E}from"./chunk-H5O67WVB.js";import"./chunk-FLHNG7GK.js";import"./chunk-7JTQAWMY.js";import"./chunk-RHDDKGHQ.js";import{Ub as d,pa as c,rc as k,sc as t,xb as r}from"./chunk-WBN6TKF6.js";import{a as o,b as p,h as C}from"./chunk-TWZW5B45.js";var y=C(E());var A={title:"Cascade Update Insertion",mdFile:"./index.md",route:"cascade-upsert",category:g,order:7,imports:[i],demos:{AnimateLogoComponent:i}},a=A;var F=[];var m={AnimateLogoComponent:[{title:"TypeScript",code:`<pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-angular-ts"><span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">import</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
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
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">export</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde"> class</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde"> AnimateLogoComponent</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">{}</span></span></code></pre>`}]},h=m;var u='<ng-doc-demo componentname="AnimateLogoComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo><h2 id="\u7EA7\u8054\u63D2\u5165\u6216\u66F4\u65B0" href="documentation/en/advanced/cascade-upsert" headinglink="true" class="ngde">\u7EA7\u8054\u63D2\u5165\u6216\u66F4\u65B0<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u7EA7\u8054\u63D2\u5165\u6216\u66F4\u65B0"></ng-doc-heading-anchor></h2><p class="ngde"><strong class="ngde">\u7EA7\u8054\u63D2\u5165\u6216\u66F4\u65B0</strong>\u662F<strong class="ngde">\u7EA7\u8054\u63D2\u5165</strong>\u548C<strong class="ngde">\u7EA7\u8054\u66F4\u65B0</strong>\u7684\u5408\u5E76\uFF0C\u5B83\u7684\u7528\u6CD5\u4E0E<code class="ngde ng-doc-code-with-link ngde"><a class="ngde ngde" href="/documentation/en/database/upsert-records#\u66F4\u65B0\u63D2\u5165">\u{1F4DA}\u66F4\u65B0\u63D2\u5165</a></code>\u4FDD\u6301\u4E00\u81F4\u3002</p>',f=(()=>{let s=class s extends e{constructor(){super(),this.pageType="guide",this.pageContent=u,this.editSourceFileUrl="https://github.com/Kronos-orm/Kronos-orm/edit/docs/src/app/docs/en/4.advanced/7.cascade-upsert/index.md?message=docs(): describe your changes here...",this.viewSourceFileUrl="https://github.com/Kronos-orm/Kronos-orm/blob/docs/src/app/docs/en/4.advanced/7.cascade-upsert/index.md",this.page=a,this.demoAssets=h}};s.\u0275fac=function(l){return new(l||s)},s.\u0275cmp=c({type:s,selectors:[["ng-doc-page-cn36d7if"]],standalone:!0,features:[k([{provide:e,useExisting:s},F,a.providers??[]]),r,t],decls:1,vars:0,template:function(l,b){l&1&&d(0,"ng-doc-page")},dependencies:[B],encapsulation:2,changeDetection:0});let n=s;return n})(),x=[p(o({},(0,y.isRoute)(a.route)?a.route:{}),{path:"",component:f,title:"Cascade Update Insertion"})],T=x;export{f as PageComponent,T as default};
