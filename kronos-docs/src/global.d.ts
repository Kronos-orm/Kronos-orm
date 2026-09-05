// src/global.d.ts
import type {WritableSignal} from "@angular/core";

declare global {
    interface Window {
        onWikiChange: WritableSignal<{ id: string, anchor: string } | null>;
    }
}

export {};