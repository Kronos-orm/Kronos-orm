// src/global.d.ts
import {EventEmitter} from "@angular/core";

declare global {
    interface Window {
        onWikiChange: EventEmitter<{ id: string, event: MouseEvent }>;
    }
}