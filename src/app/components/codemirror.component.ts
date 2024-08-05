import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnDestroy,
  Output,
  ViewChild
} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from "@angular/forms";
import {EditorState, Extension} from "@codemirror/state";
import {basicSetup} from "codemirror";
import {StreamLanguage} from "@codemirror/language";
import {kotlin} from "@codemirror/legacy-modes/mode/clike";
import {oneDark} from "@codemirror/theme-one-dark";
import {EditorView} from "@codemirror/view";
import {SharedModule} from "../shared.module";

@Component({
  selector: 'codemirror',
  imports: [
    SharedModule
  ],
  template: `
      <div #editorInstance [class]="styleClass" style="background: #282c34"></div>
      <p-skeleton class="w-full" *ngIf="!editorView" [class]="styleClass" height="250px"/>
  `,
  standalone: true,
  styles: [
    `
      :host{
        display: contents;
      }
    `
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CodemirrorComponent),
      multi: true
    }
  ]
})
export class CodemirrorComponent implements AfterViewInit, ControlValueAccessor, OnDestroy {
  @ViewChild('editorInstance') editorInstance!: ElementRef;
  _value: string = "";
  @Input() styleClass: string | undefined;
  disabled: boolean | undefined;

  @Input() set value(doc: string) {
    this._value = doc;
    if (this.editorView) {
      this.editorView?.setState(EditorState.create({
        doc, extensions: [
          basicSetup, StreamLanguage.define(kotlin), oneDark
        ]
      }));
    }
  }

  get value(): string {
    return this._value;
  }


  @Output() onChanged = new EventEmitter<string>();
  editorView: EditorView | undefined;
  onTouched = () => {

  }

  constructor() {
  }

  writeValue(obj: any): void {
    this.value = obj;
  }

  registerOnChange(fn: any): void {
    this.onChanged = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.initEditor([EditorView.editable.of(!isDisabled)]);
  }

  ngAfterViewInit(): void {
    this.initEditor();
  }

  ngOnDestroy() {
    this.editorView?.destroy();
  }

  initEditor(ext: any[] = []) {
    if (typeof document !== 'undefined') {
      setTimeout(() => {
        let editorEle = this.editorInstance.nativeElement;
        let extensions: Extension = [basicSetup, StreamLanguage.define(kotlin), oneDark, EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            this._value = update.state.doc.toString();
            this.onChanged.emit(this._value);
          }
        }), ...ext];
        try {
          let state = EditorState.create({
            doc: this.value,
            extensions,
          });

          if (this.editorView) {
            this.editorView.setState(state)
          } else {
            this.editorView = new EditorView({
              state,
              parent: editorEle,
            });
          }
        } catch (e) {
          console.error(e);
        }
      })
    }
  }
}
