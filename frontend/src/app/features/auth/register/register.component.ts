import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="onSubmit()">
      <mat-form-field class="full-width">
        <mat-label>Username</mat-label>
        <input matInput formControlName="username">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Email</mat-label>
        <input matInput type="email" formControlName="email">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Full Name</mat-label>
        <input matInput formControlName="fullName">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Password</mat-label>
        <input matInput type="password" formControlName="password">
      </mat-form-field>

      <button mat-raised-button color="primary" type="submit" class="full-width" [disabled]="form.invalid || loading">
        {{ loading ? 'Registering...' : 'Register' }}
      </button>

      <p style="text-align: center; margin-top: 16px;">
        Already have an account? <a routerLink="/login">Login</a>
      </p>
    </form>
  `
})
export class RegisterComponent {
  loading = false;
  form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    fullName: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(6)]]
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
      this.authService.register(this.form.getRawValue() as any).subscribe({
        next: () => {
          this.notification.success('Registration successful');
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Registration failed');
        }
      });
    }
  }
}
