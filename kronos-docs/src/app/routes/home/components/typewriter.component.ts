import {Component} from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {NgxTypedWriterModule} from "ngx-typed-writer";

@Component({
  selector: 'typewriter',
  imports: [
    SharedModule,
    NgxTypedWriterModule,
  ],
  template: `
    <div class="viewport">
      <div class="code-block">
        <div class="code-header">
          <span class="dot red"></span>
          <span class="dot yellow"></span>
          <span class="dot green"></span>
          <span class="code-filename">User.kt</span>
        </div>
        <pre class="code-body"><span class="hl-anno">&#64;Table</span>(<span class="hl-str">"tb_user"</span>)
<span class="hl-kw">data class</span> <span class="hl-cls">User</span>(
    <span class="hl-anno">&#64;PrimaryKey</span>(identity = <span class="hl-kw">true</span>)
    <span class="hl-kw">val</span> id: <span class="hl-type">Int</span>? = <span class="hl-kw">null</span>,
    <span class="hl-kw">val</span> name: <span class="hl-type">String</span>? = <span class="hl-kw">null</span>,
    <span class="hl-kw">val</span> age: <span class="hl-type">Int</span>? = <span class="hl-kw">null</span>,
) : <span class="hl-type">KPojo</span></pre>
      </div>
      @defer (on timer(2.5s)) {
        <div class="typer_text">
          <ngx-typed-writer
              [strings]="codes"
              [startDelay]="300"
              [isHTML]="true"
              [loop]="true"
              [smartBackspace]="true"
              [backSpeed]="20"
              [typeSpeed]="30"
              [showCursor]="true"
              [cursorChar]="'_'"
          >
          </ngx-typed-writer>
        </div>
      }
    </div>
  `,
  standalone: true,
  styles: [`
    :host {
      display: block;
      position: relative;
      width: 100%;
      height: 220px;
    }

    @font-face {
      font-family: "JetBrains Mono";
      src: url("/assets/fonts/JetBrainsMonoNL-Regular.ttf") format("truetype");
    }

    .viewport {
      position: relative;
      width: 100%;
      height: 100%;
      overflow: hidden;
      font-family: 'JetBrains Mono', monospace;
    }

    .code-block {
      position: absolute;
      left: 0;
      top: 0;
      width: 100%;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(255, 255, 255, 0.08);
      overflow: hidden;
      transform-origin: 10% 100%;
      animation: code-zoom 2s ease-in-out forwards;
    }

    @keyframes code-zoom {
      0%, 30% {
        transform: scale(1);
      }
      100% {
        transform: scale(2.4) translate(-2%, -27%);
        background: transparent;
        border-color: transparent;
      }
    }

    .code-header {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 12px;
      background: rgba(255, 255, 255, 0.03);
      border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    }

    .dot { width: 10px; height: 10px; border-radius: 50%; }
    .dot.red { background: #ff5f57; }
    .dot.yellow { background: #febc2e; }
    .dot.green { background: #28c840; }

    .code-filename {
      margin-left: 6px;
      font-size: 11px;
      color: rgba(255, 255, 255, 0.4);
    }

    .code-body {
      margin: 0;
      padding: 12px 16px;
      font-size: 14px;
      line-height: 1.7;
      color: rgba(255, 255, 255, 0.85);
    }

    .hl-kw { color: #CF8E6D; }
    .hl-anno { color: #BBB529; }
    .hl-str { color: #6AAB73; }
    .hl-cls { color: #C77DBB; }
    .hl-type { color: #6FAFBD; }

    .typer_text {
      position: absolute;
      left: 125px;
      top: 0;
      padding: 8px 0;
      font-size: 22px;
      line-height: 1.8;
      color: rgba(255, 255, 255, 0.85);
      animation: typer-fade-in 0.4s ease;
    }

    .typer_text .hl-type { color: #6FAFBD; }

    .typer_text ::ng-deep ngx-typed-writer {
      display: inline;
    }

    @keyframes typer-fade-in {
      0% { opacity: 0; }
      100% { opacity: 1; }
    }
  `],
})
export class TypewriterComponent {
  codes = [
    `<span style="color:#C084FC">.insert</span>()<br><span style="color:#C084FC">.execute</span>()`,
    `<span style="color:#C084FC">.delete</span>()<br><span style="color:#C084FC">.where</span> { it.id <span style="color:#00C8FF">==</span> <span style="color:#F9A825">1</span> }<br><span style="color:#C084FC">.execute</span>()`,
    `<span style="color:#C084FC">.select</span> { it.id <span style="color:#00C8FF">+</span> it.name }<br><span style="color:#C084FC">.where</span> { it.age <span style="color:#00C8FF">></span> <span style="color:#F9A825">18</span> }<br><span style="color:#C084FC">.orderBy</span> { it.id.<span style="color:#C084FC">desc</span>() }<br><span style="color:#C084FC">.queryList</span>()`,
    `<span style="color:#C084FC">.select</span>()<br><span style="color:#C084FC">.where</span> { it.id <span style="color:#00C8FF">in</span> ids }<br><span style="color:#C084FC">.page</span>(<span style="color:#F9A825">1</span>, <span style="color:#F9A825">10</span>)<br><span style="color:#C084FC">.withTotal</span>().<span style="color:#C084FC">queryList</span>()`,
    `<span style="color:#C084FC">.update</span>()<br><span style="color:#C084FC">.set</span> { it.name <span style="color:#00C8FF">=</span> <span style="color:#A5D6A7">"test"</span> }<br><span style="color:#C084FC">.where</span> { it.id <span style="color:#00C8FF">==</span> <span style="color:#F9A825">1</span> }<br><span style="color:#C084FC">.execute</span>()`,
    `<span style="color:#C084FC">.select</span>()<br><span style="color:#C084FC">.groupBy</span> { it.age }<br><span style="color:#C084FC">.having</span> { it.age <span style="color:#00C8FF">></span> <span style="color:#F9A825">18</span> }<br><span style="color:#C084FC">.lock</span>()<br><span style="color:#C084FC">.queryList</span>()`,
  ];
}