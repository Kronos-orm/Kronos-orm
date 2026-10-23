import {TestBed} from '@angular/core/testing';
import {NgDocSidebarService} from '@ng-doc/app/services/sidebar';
import {AppService} from '../../app.service';
import {DocumentationComponent} from './documentation.component';

describe('DocumentationComponent layout', () => {
    async function create(isMobile: boolean, language = 'en') {
        const sidebar = {isMobile, toggle: jasmine.createSpy()};
        TestBed.configureTestingModule({imports: [DocumentationComponent], providers: [
            {provide: NgDocSidebarService, useValue: sidebar}, {provide: AppService, useValue: {language}}
        ]});
        // Test layout state independently of NgDoc's asynchronous page renderer.
        TestBed.overrideComponent(DocumentationComponent, {set: {imports: [], template: ''}});
        await TestBed.compileComponents();
        return {fixture: TestBed.createComponent(DocumentationComponent), sidebar};
    }
    it('collapses the desktop sidebar and localizes its control label', async () => {
        const {fixture, sidebar} = await create(false, 'zh-CN');
        expect(fixture.componentInstance.sidebarToggleTooltip).toBe('隐藏侧栏');
        fixture.componentInstance.toggleSidebar();
        expect(fixture.componentInstance.sidebarCollapsed).toBeTrue();
        expect(fixture.componentInstance.sidebarToggleTooltip).toBe('显示侧栏');
        expect(sidebar.toggle).not.toHaveBeenCalled(); fixture.destroy();
    });
    it('delegates the mobile drawer to NgDoc without changing desktop layout state', async () => {
        const {fixture, sidebar} = await create(true);
        fixture.componentInstance.toggleSidebar();
        expect(sidebar.toggle).toHaveBeenCalledTimes(1);
        expect(fixture.componentInstance.sidebarCollapsed).toBeFalse(); fixture.destroy();
    });
});
