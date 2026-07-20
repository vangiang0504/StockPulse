import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { UserService } from '../user.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSlideToggleModule],
  template: `
    <h2>{{ isEdit ? 'Edit User' : 'Create User' }}</h2>

    <form [formGroup]="form" (ngSubmit)="onSubmit()" style="max-width: 500px;">
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

      @if (!isEdit) {
        <mat-form-field class="full-width">
          <mat-label>Password</mat-label>
          <input matInput type="password" formControlName="password">
        </mat-form-field>
      }

      @if (isEdit) {
        <mat-slide-toggle formControlName="active">Active</mat-slide-toggle>
      }

      <div style="margin-top: 24px; display: flex; gap: 8px;">
        <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">
          {{ loading ? 'Saving...' : (isEdit ? 'Update' : 'Create') }}
        </button>
        <button mat-button type="button" (click)="onCancel()">Cancel</button>
      </div>
    </form>
  `
})
export class UserFormComponent implements OnInit {
  isEdit = false;
  loading = false;
  userId: number | null = null;

  form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    fullName: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(6)]],
    active: [true]
  });

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private route: ActivatedRoute,
    private router: Router,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.userId = +id;
      this.form.get('password')?.clearValidators();
      this.form.get('username')?.disable();
      this.loadUser();
    }
  }

  loadUser(): void {
    this.userService.getById(this.userId!).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.form.patchValue({
            username: res.data.username,
            email: res.data.email,
            fullName: res.data.fullName,
            active: res.data.active
          });
        }
      },
      error: () => this.notification.error('Failed to load user')
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;

    if (this.isEdit) {
      const { email, fullName, active } = this.form.getRawValue();
      this.userService.update(this.userId!, { email: email!, fullName: fullName!, active: active! }).subscribe({
        next: () => {
          this.notification.success('User updated');
          this.router.navigate(['/users']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Update failed');
        }
      });
    } else {
      const { username, email, password, fullName } = this.form.getRawValue();
      this.userService.create({ username: username!, email: email!, password: password!, fullName: fullName! }).subscribe({
        next: () => {
          this.notification.success('User created');
          this.router.navigate(['/users']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Create failed');
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/users']);
  }
}
