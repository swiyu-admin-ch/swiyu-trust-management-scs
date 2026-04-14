import {JsonPipe} from '@angular/common';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HomeComponent} from './home.component';

describe('HomeComponent (Standalone + Jest)', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        HomeComponent, // ✅ Standalone-Komponente hier importieren
        JsonPipe
      ],
      providers: []
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });
});
