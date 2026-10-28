import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Logo } from '../../../../shared/logo/logo';

import { EntryStatusBadge } from '../../../entry/components/entry-status-badge/entry-status-badge';
import { EntryStatus } from '../../../entry/models/entry';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, EntryStatusBadge, Logo],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  protected readonly entryStatus = EntryStatus;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.pattern(/\S/)]],
    password: ['', Validators.required],
  });
  protected readonly submitting = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly errorMessage = signal<string | null>(
    this.route.snapshot.queryParamMap.get('reason') === 'unavailable'
      ? 'QuestLog is temporarily unavailable. Please try signing in again.' : null,
  );

  protected submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.submitting.set(true);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username.trim(), password).pipe(
      finalize(() => this.submitting.set(false)),
    ).subscribe({
      next: (user) => {
        this.form.controls.password.reset();
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        if (returnUrl?.startsWith('/list/')) {
          void this.router.navigateByUrl(returnUrl);
        } else {
          void this.router.navigate(['/list', user.username]);
        }
      },
      error: (error: unknown) => {
        const status = error instanceof HttpErrorResponse ? error.status : 0;
        this.errorMessage.set(status === 401
          ? 'Incorrect username or password, or your account is disabled.'
          : status === 403
            ? 'Your sign-in session could not be verified. Please try again.'
            : 'Unable to reach QuestLog. Please try again in a moment.');
      },
    });
  }
}
