import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {HomeComponent} from './home.component';
import {AppService} from '../../app.service';

describe('HomeComponent', () => {
    it('updates visible navigation and documentation links when locale changes', async () => {
        const app = {language: 'en'};
        await TestBed.configureTestingModule({imports: [HomeComponent], providers: [
            provideRouter([]), provideNoopAnimations(), {provide: AppService, useValue: app}
        ]}).compileComponents();
        const fixture = TestBed.createComponent(HomeComponent); fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('.nav-cta').textContent).toContain('Open docs');
        expect(fixture.nativeElement.querySelector('.nav-cta').getAttribute('href')).toContain('/documentation/en/');
        app.language = 'zh-CN'; fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('.nav-cta').textContent).toContain('打开文档');
        expect(fixture.nativeElement.querySelector('.nav-cta').getAttribute('href')).toContain('/documentation/zh-CN/');
        fixture.destroy();
    });
});
