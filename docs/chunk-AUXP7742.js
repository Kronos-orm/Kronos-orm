import{a as F}from"./chunk-N5DZB2QX.js";import{a as o}from"./chunk-F4CEQQ2I.js";import"./chunk-XLNHBPPO.js";import{a as i}from"./chunk-YFWCYGX4.js";import{a as g}from"./chunk-FVAUZTMF.js";import"./chunk-HFYZ5PKQ.js";import"./chunk-3UJNDYOE.js";import{a as e}from"./chunk-2DBUGSSC.js";import"./chunk-XLVKZVFL.js";import{J as A}from"./chunk-H5O67WVB.js";import"./chunk-FLHNG7GK.js";import"./chunk-7JTQAWMY.js";import"./chunk-RHDDKGHQ.js";import{Ub as k,pa as c,rc as t,sc as B,xb as d}from"./chunk-WBN6TKF6.js";import{a as p,b as r,h as E}from"./chunk-TWZW5B45.js";var D=E(A());var m={title:"\u7B80\u4ECB",mdFile:"./index.md",route:"introduce",category:F,order:1,imports:[i,o],demos:{AnimateLogoComponent:i,FeatureCardsComponent:o}},a=m;var h=[];var u={AnimateLogoComponent:[{title:"TypeScript",code:`<pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-angular-ts"><span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">import</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
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
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">export</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde"> class</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde"> AnimateLogoComponent</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">{}</span></span></code></pre>`}],FeatureCardsComponent:[{title:"TypeScript",code:`<pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-angular-ts"><span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">import</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  Component</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">} </span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">from</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> '@angular/core'</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">import</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {SharedModule} </span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">from</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> "../shared.module"</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">import</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {AnimateOnScrollModule} </span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">from</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> "primeng/animateonscroll"</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">;</span></span>
<span class="line"></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#E6B673" class="ngde">@</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">Component</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">({</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  selector</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> 'feature-cards'</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  imports</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> [</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#E6B673" class="ngde">    SharedModule</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#E6B673" class="ngde">    AnimateOnScrollModule</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  ]</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  template</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> \`</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">    &#x3C;</span><span style="color:#22863A;--shiki-dark:#39BAE6" class="ngde">div</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> pAnimateOnScroll</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> enterClass</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">"zoomin"</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> class</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">"card flex flex-row md:justify-content-between gap-3 animation-duration-1000 animation-ease-in-out"</span><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">></span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">      &#x3C;</span><span style="color:#22863A;--shiki-dark:#39BAE6" class="ngde">p-card</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> pRipple</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> header</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">"Write Kotlin ORM in Simple and Type-safe Way"</span><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">/></span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">      &#x3C;</span><span style="color:#22863A;--shiki-dark:#39BAE6" class="ngde">p-card</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> pRipple</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> header</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">"Less runtime reflect, higher runtime efficiency"</span><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">/></span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">      &#x3C;</span><span style="color:#22863A;--shiki-dark:#39BAE6" class="ngde">p-card</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> pRipple</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> header</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">"Multiple database dialect support"</span><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">/></span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">    &#x3C;/</span><span style="color:#22863A;--shiki-dark:#39BAE6" class="ngde">div</span><span style="color:#24292E;--shiki-dark:#39BAE680" class="ngde">></span></span>
<span class="line"><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">  \`</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  standalone</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> true</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  styles</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> []</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">})</span></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">export</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde"> class</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde"> FeatureCardsComponent</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">}</span></span></code></pre>`}]},y=u;var f=`<ng-doc-demo componentname="AnimateLogoComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo><h1 id="\u4EC0\u4E48\u662Fkronos" href="documentation/zh-CN/getting-started/introduce" headinglink="true" class="ngde">\u4EC0\u4E48\u662FKronos<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u4EC0\u4E48\u662Fkronos"></ng-doc-heading-anchor></h1><p class="ngde">Kronos\u662F\u4E00\u6B3E\u57FA\u4E8E<span class="noun ngde" id="code-first" onclick="window.onWikiChange.emit({&#x27;id&#x27;: &#x27;code-first&#x27;, &#x27;anchor&#x27;: null})">Code First<svg class="icon ngde" viewBox="0 0 1026 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="200" height="200"> <path d="M897.024 0q26.624 0 49.664 10.24t40.448 27.648 27.648 40.448 10.24 49.664v576.512q0 26.624-10.24 49.664t-27.648 40.448-40.448 27.648-49.664 10.24h-128v-128h63.488q26.624 0 45.568-18.432t18.944-45.056V320.512q0-26.624-18.944-45.568T832.512 256h-512q-26.624 0-45.568 18.944T256 320.512V384H128V128q0-26.624 10.24-49.664t27.648-40.448 40.448-27.648T256 0h641.024zM576.512 448.512q26.624 0 49.664 10.24t40.448 27.648 27.648 40.448 10.24 49.664v256q0 26.624-10.24 49.664t-27.648 40.448-40.448 27.648-49.664 10.24h-384q-26.624 0-50.176-10.24t-40.96-27.648-27.648-40.448-10.24-49.664v-256q0-26.624 10.24-49.664t27.648-40.448 40.96-27.648 50.176-10.24h384z m0 256q0-26.624-18.944-45.056T512 641.024H256q-26.624 0-45.056 18.432t-18.432 45.056v64.512q0 26.624 18.432 45.056T256 832.512h256q26.624 0 45.568-18.432t18.944-45.056v-64.512z" class="ngde"></path></svg></span> \u6A21\u5F0F\u3001KCP\uFF08<strong class="ngde">\u7F16\u8BD1\u5668\u63D2\u4EF6</strong>\uFF09\uFF0C\u4E3Akotlin\u8BBE\u8BA1\u7684\u73B0\u4EE3\u5316\u7684<strong class="ngde">ORM</strong>\u6846\u67B6\uFF0C\u5B83\u540C\u65F6\u652F\u6301<strong class="ngde">JVM</strong>\u548C<strong class="ngde">Android</strong>\u5E73\u53F0\u3002</p><pre class="mermaid ngde">graph LR
    A[Kronos] --> B[Kronos-core]
    B --> I[ORM for KPojo]
    I --> J[Query\u3001Insert\u3001Update\u3001Delete\uFF0Cetc.]
    A --> C[Kronos-compiler-plugin]
    C --> L[Kronos-compiler-kotlin-plugin]
    C --> D[Kronos-maven-plugin]
    C --> E[Kronos-gradle-plugin]
    A --> F[Kronos-data-source-wrapper]
    F --> G[DataSource]
    G --> H[Mysql, Sqlite, Postgresql, Oracle, SqlServer, etc.]
    A --> K[Kronos-logging and Other Plugins]</pre><h1 id="\u4E3A\u4EC0\u4E48\u4F7F\u7528kronos" href="documentation/zh-CN/getting-started/introduce" headinglink="true" class="ngde">\u4E3A\u4EC0\u4E48\u4F7F\u7528Kronos<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u4E3A\u4EC0\u4E48\u4F7F\u7528kronos"></ng-doc-heading-anchor></h1><p class="ngde"><em class="ngde">Kronos</em>\u4E3AKotlin\u800C\u5F00\u53D1\uFF0C\u901A\u8FC7KCP\u5B9E\u73B0\u7684\u8868\u8FBE\u5F0F\u6811\u5206\u6790\u652F\u6301\u4EE5\u53CAkotlin\u7684\u6CDB\u578B\u548C\u9AD8\u9636\u51FD\u6570\uFF0CKronos\u63D0\u4F9B\u4E86<strong class="ngde">\u8D85\u7EA7\u5BCC\u6709\u8868\u73B0\u529B\u3001\u7B80\u6D01\u800C\u53C8\u8BED\u4E49\u5316</strong>\u7684\u5199\u6CD5\uFF0C\u4F7F\u64CD\u4F5C\u6570\u636E\u5E93\u53D8\u5F97\u66F4\u52A0\u7B80\u5355\u3002</p><p class="ngde">\u57FA\u4E8ECode First\u7684\u7406\u5FF5\uFF0C\u6211\u4EEC\u63D0\u4F9B\u4E86<strong class="ngde">\u6570\u636E\u5E93\u8868\u7ED3\u6784\u7684\u81EA\u52A8\u521B\u5EFA\u3001\u81EA\u52A8\u540C\u6B65\uFF0C\u4EE5\u53CA\u5BF9\u8868\u7ED3\u6784\u3001\u7D22\u5F15</strong>\u7B49\u64CD\u4F5C\u7684\u652F\u6301\u3002</p><p class="ngde">\u540C\u65F6\u901A\u8FC7\u7F16\u8BD1\u5668\u63D2\u4EF6\uFF0C\u6211\u4EEC\u5B9E\u73B0\u4E86\u63D0\u4F9B\u4E86\u65E0\u53CD\u5C04\u7684Pojo\u548CMap\u4E92\u8F6C\u65B9\u6848\u3002</p><p class="ngde"><em class="ngde">Kronos</em>\u7684\u7EA7\u8054\u64CD\u4F5C\u3001\u8DE8\u8868\u8DE8\u5E93\u67E5\u8BE2\u5927\u5927\u63D0\u5347\u4E86\u5F00\u53D1\u6548\u7387\uFF0C\u5E76\u57FA\u4E8Ekotlin\u534F\u7A0B\u673A\u5236\u5927\u5927\u63D0\u9AD8\u4E86\u9AD8\u5E76\u53D1\u6027\u80FD\u3002</p><h1 id="\u793A\u4F8B" href="documentation/zh-CN/getting-started/introduce" headinglink="true" class="ngde">\u793A\u4F8B<ng-doc-heading-anchor class="ng-doc-anchor ngde" anchor="\u793A\u4F8B"></ng-doc-heading-anchor></h1><ng-doc-blockquote type="note" class="ngde"><p class="ngde">\u4EE5\u4E0B\u662F\u4E00\u4E2A\u7B80\u5355\u7684\u793A\u4F8B\u3002</p></ng-doc-blockquote><pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0" name="demo" icon="kotlin"><code class="language-kotlin"><span class="line"><span style="color:#6A737D;--shiki-dark:#ACB6BF8C;font-style:inherit;--shiki-dark-font-style:italic" class="ngde">// \u521B\u5EFA\u4E00\u4E2AUser\u5BF9\u8C61</span></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">val</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> user: </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">User</span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde"> =</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> User</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    id </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">=</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 1</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    name </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> "Kronos"</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    age </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">=</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 18</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">)</span></span>
<span class="line"></span>
<span class="line"><span style="color:#6A737D;--shiki-dark:#ACB6BF8C;font-style:inherit;--shiki-dark-font-style:italic" class="ngde"> // \u5982\u679C\u8868\u4E0D\u5B58\u5728\u5219\u521B\u5EFA\u8868\uFF0C\u5426\u5219\u540C\u6B65\u8868\u7ED3\u6784\uFF0C\u5305\u62EC\u8868\u5217\u3001\u7D22\u5F15\u3001\u5907\u6CE8\u7B49</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">dataSource.table.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">sync</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(user)</span></span>
<span class="line"></span>
<span class="line"><span style="color:#6A737D;--shiki-dark:#ACB6BF8C;font-style:inherit;--shiki-dark-font-style:italic" class="ngde">// \u63D2\u5165\u6570\u636E</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">user.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">insert</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">().</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">execute</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">()</span></span>
<span class="line"></span>
<span class="line"><span style="color:#6A737D;--shiki-dark:#ACB6BF8C;font-style:inherit;--shiki-dark-font-style:italic" class="ngde">// \u6839\u636Eid\u66F4\u65B0name\u5B57\u6BB5</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">user.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">update</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">().</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">set</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> { it.name </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> "Kronos ORM"</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> }.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">by</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">{ it.id }.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">execute</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">()</span></span>
<span class="line"></span>
<span class="line"><span style="color:#6A737D;--shiki-dark:#ACB6BF8C;font-style:inherit;--shiki-dark-font-style:italic" class="ngde">// \u6839\u636Eid\u67E5\u8BE2name\u5B57\u6BB5</span></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">val</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> name: </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">String</span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde"> =</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> user.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">select</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">{ it.name }.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">where</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">{ it.id </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">==</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 1</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> }.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">queryOne</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">&#x3C;</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">String</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">>()</span></span>
<span class="line"></span>
<span class="line"><span style="color:#6A737D;--shiki-dark:#ACB6BF8C;font-style:inherit;--shiki-dark-font-style:italic" class="ngde">// \u5220\u9664id\u4E3A1\u7684\u6570\u636E</span></span>
<span class="line"><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">User</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">().</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">delete</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">().</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">where</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">{ it.id </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">==</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 1</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> }.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">execute</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">()</span></span></code></pre><ng-doc-demo componentname="FeatureCardsComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo>`,x=(()=>{let s=class s extends e{constructor(){super(),this.pageType="guide",this.pageContent=f,this.editSourceFileUrl="https://github.com/Kronos-orm/Kronos-orm/edit/docs/src/app/docs/zh-CN/1.getting-started/1.introduce/index.md?message=docs(): describe your changes here...",this.viewSourceFileUrl="https://github.com/Kronos-orm/Kronos-orm/blob/docs/src/app/docs/zh-CN/1.getting-started/1.introduce/index.md",this.page=a,this.demoAssets=y}};s.\u0275fac=function(l){return new(l||s)},s.\u0275cmp=c({type:s,selectors:[["ng-doc-page-frb98ui8"]],standalone:!0,features:[t([{provide:e,useExisting:s},h,a.providers??[]]),d,B],decls:1,vars:0,template:function(l,v){l&1&&k(0,"ng-doc-page")},dependencies:[g],encapsulation:2,changeDetection:0});let n=s;return n})(),b=[r(p({},(0,D.isRoute)(a.route)?a.route:{}),{path:"",component:x,title:"\u7B80\u4ECB"})],z=b;export{x as PageComponent,z as default};