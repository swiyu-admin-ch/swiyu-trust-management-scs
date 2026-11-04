import {provideHttpClient} from '@angular/common/http';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideTranslateService, TranslateService} from '@ngx-translate/core';
import {BusinessPartnerViewListItem, TaskListItem} from '../../../../api/generated';
import {BusinessPartnerDeletePanelService} from '../../../business-partner-list/delete-panel/business-partner-delete-panel.service';
import {SidepanelService} from '../sidepanel.service';
import {SidepanelHeaderComponent} from './header.component';

describe('SidepanelHeaderComponent', () => {
  let component: SidepanelHeaderComponent;
  let fixture: ComponentFixture<SidepanelHeaderComponent>;
  let sidepanelService: SidepanelService;
  let deletePanelService: BusinessPartnerDeletePanelService;
  let translateService: TranslateService;

  const task = {id: 'task-id'} as TaskListItem;
  const partner = {businessPartnerId: 'partner-id', name: 'Example AG'} as BusinessPartnerViewListItem;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SidepanelHeaderComponent],
      providers: [provideHttpClient(), provideTranslateService()]
    }).compileComponents();

    sidepanelService = TestBed.inject(SidepanelService);
    deletePanelService = TestBed.inject(BusinessPartnerDeletePanelService);
    translateService = TestBed.inject(TranslateService);

    fixture = TestBed.createComponent(SidepanelHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // The component is created while the app bootstraps, so the translations only arrive afterwards.
  function loadTranslations(): void {
    translateService.setTranslation('de', {
      'app.trust-onboarding-task.sidepanel.approve': 'Antrag annehmen',
      'app.trust-onboarding-task.sidepanel.add-note': 'Notiz hinzufügen',
      'businessPartner.delete.title': 'Geschäftspartner löschen'
    });
    translateService.use('de');
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show no title while no panel is open', () => {
    loadTranslations();

    expect(component.title()).toBe('');
  });

  it('should show the delete title when the delete panel opens', () => {
    loadTranslations();

    deletePanelService.open(partner);

    expect(component.title()).toBe('Geschäftspartner löschen');
  });

  it('should show the action title when a task panel opens', () => {
    loadTranslations();

    sidepanelService.approve(task);

    expect(component.title()).toBe('Antrag annehmen');
  });

  it('should follow whichever panel opened last', () => {
    loadTranslations();

    deletePanelService.open(partner);
    sidepanelService.addNote(task);

    expect(component.title()).toBe('Notiz hinzufügen');
  });

  it('should translate the title of an already open panel on a language switch', () => {
    loadTranslations();
    translateService.setTranslation('fr', {'businessPartner.delete.title': 'Supprimer le partenaire commercial'});
    deletePanelService.open(partner);

    translateService.use('fr');

    expect(component.title()).toBe('Supprimer le partenaire commercial');
  });
});
