import{a as g}from"./chunk-NZX2KOOX.js";import{a as p}from"./chunk-OW34UWA4.js";import"./chunk-LWH4IL2R.js";import{a as i}from"./chunk-HRYCDFWX.js";import{a as F}from"./chunk-ZURMYKNO.js";import"./chunk-4JROJSXB.js";import"./chunk-RFANYCWZ.js";import{a as e}from"./chunk-6UJLH36S.js";import"./chunk-IXRPNEAA.js";import{J as A}from"./chunk-C2WBB6IV.js";import"./chunk-VUUK7CK3.js";import"./chunk-H7KULVEM.js";import"./chunk-72PMQN5Q.js";import{Ub as k,pa as r,rc as t,sc as B,xb as d}from"./chunk-2GGA2KC7.js";import{a as o,b as c,h as C}from"./chunk-TWZW5B45.js";var D=C(A());var m={title:"Welcome & Introduction",mdFile:"./index.md",category:g,order:0,imports:[i,p],demos:{AnimateLogoComponent:i,FeatureCardsComponent:p}},a=m;var h=[];var u={AnimateLogoComponent:[{title:"TypeScript",code:`<pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0"><code class="language-angular-ts"><span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">import</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
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
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  styles</span><span style="color:#24292E;--shiki-dark:#BFBDB6B3" class="ngde">:</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> [</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">\`</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    p-card </span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">:hover</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    }</span></span>
<span class="line"><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde">  \`</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">]</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">})</span></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">export</span><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde"> class</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde"> FeatureCardsComponent</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> {</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">}</span></span></code></pre>`}]},y=u;var f=`<ng-doc-demo componentname="AnimateLogoComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo><p class="ngde"><strong class="ngde">Kronos ORM (Kotlin Reactive Object-Relational-Mapping) is a modern Kotlin ORM framework based on KCP and designed for K2.</strong></p><p class="ngde"><em class="ngde">Kronos</em> is a lightweight framework that provides developers with a simple solution for interacting with multiple databases.</p><p class="ngde"><em class="ngde">Kronos</em> analyzes IR expression trees to simplify code logic, making ORM coding concise and semantic. Through a compiler plugin, we also provide a simple solution for converting between Pojo and Map.</p><p class="ngde">The design philosophy behind <em class="ngde">Kronos</em> is to address the shortcomings of existing ORM frameworks and provide a more convenient and efficient data operation experience based on coroutines and task mechanisms.</p><pre class="shiki shiki-themes github-light ayu-dark" style="background-color:#fff;--shiki-dark-bg:#0b0e14;color:#24292e;--shiki-dark:#bfbdb6" tabindex="0" name="demo" icon="kotlin"><code class="language-kotlin"><span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">if</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(</span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">!</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">db.table.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">exsits</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">&#x3C;</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">User</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">>()){</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">  db.table.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">create</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">&#x3C;</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">User</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">>()</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">}</span></span>
<span class="line"></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">val</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> user: </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">User</span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde"> =</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde"> User</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">        id </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">=</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 1</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">        name </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> "Kronos"</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">,</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">        age </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">=</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 18</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    )</span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">    </span></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">user.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">insert</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">(user)</span></span>
<span class="line"></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">user.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">update</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">().</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">set</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> { it.name </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">=</span><span style="color:#032F62;--shiki-dark:#AAD94C" class="ngde"> "Kronos ORM"</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> }.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">where</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> { it.id </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">==</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 1</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> }.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">execute</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">()</span></span>
<span class="line"></span>
<span class="line"><span style="color:#D73A49;--shiki-dark:#FF8F40" class="ngde">val</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> nameOfUser: </span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">String</span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde"> =</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> user.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">select</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">{ it.name }.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">where</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> { it.id </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">==</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 1</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> }.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">queryOne</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">&#x3C;</span><span style="color:#6F42C1;--shiki-dark:#59C2FF" class="ngde">String</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">>()</span></span>
<span class="line"></span>
<span class="line"><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">user.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">delete</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">().</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">where</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> { it.id </span><span style="color:#D73A49;--shiki-dark:#F29668" class="ngde">==</span><span style="color:#005CC5;--shiki-dark:#D2A6FF" class="ngde"> 1</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde"> }.</span><span style="color:#6F42C1;--shiki-dark:#FFB454" class="ngde">execute</span><span style="color:#24292E;--shiki-dark:#BFBDB6" class="ngde">()</span></span></code></pre><ng-doc-demo componentname="FeatureCardsComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo>`,b=(()=>{let s=class s extends e{constructor(){super(),this.pageType="guide",this.pageContent=f,this.editSourceFileUrl="https://github.com/Kronos-orm/Kronos-orm/edit/docs/src/app/docs/en/getting-started/welcome/index.md?message=docs(): describe your changes here...",this.viewSourceFileUrl="https://github.com/Kronos-orm/Kronos-orm/blob/docs/src/app/docs/en/getting-started/welcome/index.md",this.page=a,this.demoAssets=y}};s.\u0275fac=function(l){return new(l||s)},s.\u0275cmp=r({type:s,selectors:[["ng-doc-page-29fibfmg"]],standalone:!0,features:[t([{provide:e,useExisting:s},h,a.providers??[]]),d,B],decls:1,vars:0,template:function(l,v){l&1&&k(0,"ng-doc-page")},dependencies:[F],encapsulation:2,changeDetection:0});let n=s;return n})(),x=[c(o({},(0,D.isRoute)(a.route)?a.route:{}),{path:"",component:b,title:"Welcome & Introduction"})],G=x;export{b as PageComponent,G as default};
