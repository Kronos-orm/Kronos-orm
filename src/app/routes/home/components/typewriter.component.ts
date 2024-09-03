import {Component} from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {AnimateOnScrollModule} from "primeng/animateonscroll";
import {NgxTypedWriterModule} from "ngx-typed-writer";

@Component({
  selector: 'typewriter',
  imports: [
    SharedModule,
    AnimateOnScrollModule,
    NgxTypedWriterModule,
  ],
  template: `

    <div class="typer_img"></div>
    @defer (on timer(4000)) {
      <div class="typer_text">
        <div class="text">
          <ngx-typed-writer
              [strings]="codes"
              [startDelay]="1000"
              [isHTML]="true"
              [loop]="true"
              [smartBackspace]="true"
              [backSpeed]="50"
              [typeSpeed]="100"
              [showCursor]="true"
              [cursorChar]="'_'"
          >
          </ngx-typed-writer>
        </div>
      </div>
    }
  `,
  standalone: true,
  styles: [`
    :host {
      display: block;
      position: relative;
      width: 100%;
      height: 200px;
    }

    @font-face {
      font-family: "JetBrains Mono";
      src: url("/assets/fonts/JetBrainsMonoNL-Regular.ttf") format("truetype");
    }

    .typer_img {
      background-image: url("/assets/images/animation/a1.png");
      width: 40vw;
      height: 16vw;
      transform: scale(0.5);
      transform-origin: 0 0;
      background-repeat: no-repeat;
      background-size: 100% auto;
      animation: focus 4s forwards;
      position: absolute;
      left: 0;
      top: 0;
      margin: auto;
      text-align: center;
      mix-blend-mode: exclusion;
      filter: invert(1);
    }

    @keyframes focus {
      0% {
        background-size: 100% auto;
      }
      20% {
        background-size: 100% auto;
      }
      100% {
        background-size: 200% auto;
        background-position: 35% 110%;
        opacity: 1;
      }
    }

    .typer_text {
      position: absolute;
      left: 0;
      top: 0;
      margin: auto;
      width: 100%;
      height: 100%;
      line-height: 2.5vw;

      .text {
        left: 6.4vw;
        position: absolute;
        top: 0.4vw;
        font-size: 2vw;
        font-weight: 500;
        word-break: keep-all;
        white-space: nowrap;
        font-family: 'JetBrains Mono', monospace;
      }
    }
  `],
})
export class TypewriterComponent {

  codes = [
    `.<span class="code-green">insert()</span><br/>.<span class="code-green">execute()</span>`,
    `.<span class="code-green">delete()</span><br/>.<span class="code-green">where{ </span><span class="code-red">it.id </span>== 1<span class="code-green"> }</span><br/>.<span class="code-green">execute()</span>`,
    `.<span class="code-green">select()</span><br/>.<span class="code-green">where{ </span><span class="code-red">it.id </span>== 1<span class="code-green"> }</span><br/>.<span class="code-green">queryOne()</span>`,
    `.<span class="code-green">update()</span><br/>.<span class="code-green">set{ </span><span class="code-red">it.name </span>= "name"<span class="code-green"> }</span><br/>.<span class="code-green">where{ </span><span class="code-red">it.id </span>== 1<span class="code-green"> }<br/>.<span class="code-green">execute()</span>`,
  ];
}
