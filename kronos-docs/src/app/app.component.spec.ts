import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {AppComponent} from './app.component';
import {AppService} from './app.service';

describe('AppComponent', () => {
    beforeEach(async () => {
        await TestBed.configureTestingModule({imports: [AppComponent], providers: [
            provideRouter([]), provideNoopAnimations(), {provide: AppService, useValue: {language: 'en'}}
        ]}).compileComponents();
    });
    it('renders the application router and notification host', () => {
        const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
        expect(fixture.componentInstance.title).toBe('Kronos');
        expect(fixture.nativeElement.querySelector('router-outlet')).not.toBeNull();
        expect(fixture.nativeElement.querySelector('p-toast')).not.toBeNull();
        fixture.destroy();
    });
    it('shows the loading indicator only while a route is fetching', () => {
        const fixture = TestBed.createComponent(AppComponent); fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('.alain-default__progress-bar')).toBeNull();
        fixture.componentInstance.isFetching = true; fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('.alain-default__progress-bar')).not.toBeNull();
        fixture.destroy();
    });
});
