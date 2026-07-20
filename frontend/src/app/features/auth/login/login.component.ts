import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="onSubmit()">
      <mat-form-field class="full-width">
        <mat-label>Username</mat-label>
        <input matInput formControlName="username">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Password</mat-label>
        <input matInput type="password" formControlName="password">
      </mat-form-field>

      <button mat-raised-button color="primary" type="submit" class="full-width" [disabled]="form.invalid || loading">
        {{ loading ? 'Logging in...' : 'Login' }}
      </button>

      <p style="text-align: center; margin-top: 16px;">
        Don't have an account? <a routerLink="/register">Register</a>
      </p>
    </form>
  `
})
export class LoginComponent {
  loading = false;
  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private notification: NotificationService
  ) {}

  onSubmit(): void {
    if (this.form.valid) {
      this.loading = true;
      this.authService.login(this.form.getRawValue() as { username: string; password: string }).subscribe({
        next: () => {
          this.notification.success('Login successful');
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Login failed');
        }
      });
    }
  }
}
