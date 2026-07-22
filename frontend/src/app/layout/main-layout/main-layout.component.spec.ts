import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { MainLayoutComponent } from './main-layout.component';

describe('MainLayoutComponent', () => {
  let fixture: ComponentFixture<MainLayoutComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getUsername', 'getRole', 'logout']);
    authService.getUsername.and.returnValue('stock-user');

    await TestBed.configureTestingModule({
      imports: [MainLayoutComponent, NoopAnimationsModule],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }]
    }).compileComponents();

    fixture = TestBed.createComponent(MainLayoutComponent);
  });

  for (const role of ['STAFF', 'MANAGER', 'ADMIN', 'ROLE_STAFF']) {
    it(`shows Warehouse navigation for ${role}`, () => {
      authService.getRole.and.returnValue(role);

      fixture.detectChanges();

      const link = fixture.nativeElement.querySelector('a[href="/warehouses"]');
      expect(link).not.toBeNull();
      expect(link.textContent).toContain('Warehouses');
    });
  }

  it('does not show Warehouse navigation to a non-StockPulse USER role', () => {
    authService.getRole.and.returnValue('USER');

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('a[href="/warehouses"]')).toBeNull();
  });
});
