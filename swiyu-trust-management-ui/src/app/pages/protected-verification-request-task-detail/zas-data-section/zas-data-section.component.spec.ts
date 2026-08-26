import {provideHttpClient} from '@angular/common/http';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of, throwError} from 'rxjs';
import {TrustOnboardingTaskApi, ZasData} from '../../../api/generated';
import {ZasDataSectionComponent} from './zas-data-section.component';

describe('ZasDataSectionComponent', () => {
  let fixture: ComponentFixture<ZasDataSectionComponent>;
  let component: ZasDataSectionComponent;
  let mockApi: jest.Mocked<TrustOnboardingTaskApi>;

  const taskId = 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd';

  function getZasData(): ZasData {
    return {businessId: taskId, statusCode: 'ACTIVE'} as ZasData;
  }

  beforeEach(async () => {
    mockApi = {
      getProtectedVerificationRequestTaskZasData: jest.fn(),
      markProtectedVerificationRequestTaskZasDataReviewed: jest.fn()
    } as unknown as jest.Mocked<TrustOnboardingTaskApi>;

    await TestBed.configureTestingModule({
      imports: [ZasDataSectionComponent],
      providers: [provideHttpClient(), {provide: TrustOnboardingTaskApi, useValue: mockApi}]
    }).compileComponents();

    fixture = TestBed.createComponent(ZasDataSectionComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('taskId', taskId);
  });

  it('marks the ZAS data as reviewed only after it has been fetched, then emits opened', () => {
    mockApi.getProtectedVerificationRequestTaskZasData.mockReturnValue(of(getZasData()) as never);
    mockApi.markProtectedVerificationRequestTaskZasDataReviewed.mockReturnValue(of(undefined) as never);
    const openedSpy = jest.fn();
    component.opened.subscribe(openedSpy);

    component.load();

    expect(mockApi.getProtectedVerificationRequestTaskZasData).toHaveBeenCalledWith({taskId});
    expect(mockApi.markProtectedVerificationRequestTaskZasDataReviewed).toHaveBeenCalledWith({taskId});
    expect(component.zasData()).toEqual(getZasData());
    expect(openedSpy).toHaveBeenCalledTimes(1);
    expect(component.error()).toBe(false);
  });

  it('does not emit opened or show the data if marking it as reviewed fails', () => {
    mockApi.getProtectedVerificationRequestTaskZasData.mockReturnValue(of(getZasData()) as never);
    mockApi.markProtectedVerificationRequestTaskZasDataReviewed.mockReturnValue(
      throwError(() => new Error('boom')) as never
    );
    const openedSpy = jest.fn();
    component.opened.subscribe(openedSpy);

    component.load();

    expect(component.zasData()).toBeNull();
    expect(component.error()).toBe(true);
    expect(openedSpy).not.toHaveBeenCalled();
  });

  it('shows an error and does not call the review endpoint if fetching fails', () => {
    mockApi.getProtectedVerificationRequestTaskZasData.mockReturnValue(throwError(() => new Error('boom')) as never);

    component.load();

    expect(mockApi.markProtectedVerificationRequestTaskZasDataReviewed).not.toHaveBeenCalled();
    expect(component.error()).toBe(true);
  });
});
