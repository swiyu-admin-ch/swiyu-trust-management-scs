import {provideHttpClient} from '@angular/common/http';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideTranslateService} from '@ngx-translate/core';
import {SidepanelHeaderComponent} from './header.component';

describe('SidepanelHeaderComponent', () => {
  let component: SidepanelHeaderComponent;
  let fixture: ComponentFixture<SidepanelHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SidepanelHeaderComponent],
      providers: [provideHttpClient(), provideTranslateService()]
    }).compileComponents();

    fixture = TestBed.createComponent(SidepanelHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
